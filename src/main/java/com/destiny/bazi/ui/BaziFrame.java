package com.destiny.bazi.ui;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.DaYunPeriod;
import com.destiny.bazi.core.DaYunResult;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.io.BaziFileIO;
import com.destiny.bazi.io.BirthInput;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 子平八字排盤主視窗：輸入西元年月日時，計算並顯示四柱與十神；
 * 並提供選單列以儲存／載入排盤結果，以及調整畫面字體大小。
 */
public final class BaziFrame extends JFrame {

    private static final float TITLE_SIZE = 26f;
    private static final float SUBTITLE_SIZE = 13f;
    private static final float INPUT_LABEL_SIZE = 13f;
    private static final float SPINNER_SIZE = 14f;
    private static final float BUTTON_SIZE = 15f;
    private static final float SUMMARY_SIZE = 14f;
    private static final float NOTE_SIZE = 12f;
    private static final float SECTION_TITLE_SIZE = 15f;

    private static final double FONT_SCALE_MIN = 0.7;
    private static final double FONT_SCALE_MAX = 1.8;
    private static final double FONT_SCALE_STEP = 0.1;

    private final BaziCalculator calculator = new BaziCalculator();

    private final JSpinner yearSpinner;
    private final JSpinner monthSpinner;
    private final JSpinner daySpinner;
    private final JSpinner hourSpinner;
    private final JSpinner minuteSpinner;
    private final JSpinner[] spinners;
    private final JComboBox<Gender> genderCombo = new JComboBox<>(Gender.values());

    private final PillarPanel yearPanel = new PillarPanel("年柱");
    private final PillarPanel monthPanel = new PillarPanel("月柱");
    private final PillarPanel dayPanel = new PillarPanel("日柱");
    private final PillarPanel hourPanel = new PillarPanel("時柱");
    private final PillarPanel[] pillarPanels = {yearPanel, monthPanel, dayPanel, hourPanel};

    private final List<PillarPanel> dayunPanels = new ArrayList<>();
    private final JLabel dayunTitleLabel = new JLabel("大運", SwingConstants.CENTER);
    private final JLabel dayunNoteLabel = new JLabel(" ", SwingConstants.CENTER);

    private final JLabel titleLabel = new JLabel("子平八字排盤", SwingConstants.CENTER);
    private final JLabel subtitleLabel = new JLabel("輸入出生年、月、日、時、性別，推算四柱、十神與大運", SwingConstants.CENTER);
    private final JLabel inputSectionTitle = new JLabel("排盤輸入", SwingConstants.CENTER);
    private final JLabel fourPillarsSectionTitle = new JLabel("四柱", SwingConstants.CENTER);
    private final JLabel summarySectionTitle = new JLabel("命盤摘要", SwingConstants.CENTER);
    private final List<JLabel> sectionTitleLabels =
            List.of(inputSectionTitle, fourPillarsSectionTitle, dayunTitleLabel, summarySectionTitle);
    private final List<JLabel> inputLabels = new ArrayList<>();
    private final JButton calcButton = new JButton("排盤");

    private final JLabel summaryLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel noteLabel = new JLabel(" ", SwingConstants.CENTER);

    private double fontScale = 1.0;

    public BaziFrame() {
        super("子平八字排盤");

        int nowYear = Year.now().getValue();
        yearSpinner = new JSpinner(new SpinnerNumberModel(nowYear, 1900, 2100, 1));
        monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        spinners = new JSpinner[]{yearSpinner, monthSpinner, daySpinner, hourSpinner, minuteSpinner};
        for (JSpinner sp : spinners) {
            sp.setEditor(new JSpinner.NumberEditor(sp, "#"));
            sp.getEditor().getComponent(0).setBackground(DarkTheme.BG_CARD);
            sp.getEditor().getComponent(0).setForeground(DarkTheme.FG_PRIMARY);
        }
        genderCombo.setBackground(DarkTheme.BG_CARD);
        genderCombo.setForeground(DarkTheme.FG_PRIMARY);
        genderCombo.addActionListener(e -> calculate());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        setMinimumSize(new Dimension(780, 600));
        pack();
        setLocationRelativeTo(null);

        applyFontScale();
        calculate();
    }

    // ---------------------------------------------------------------- 選單列

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        DarkTheme.styleMenuBar(menuBar);

        menuBar.add(buildFileMenu());
        menuBar.add(buildViewMenu());
        menuBar.add(buildHelpMenu());
        return menuBar;
    }

    private JMenu buildFileMenu() {
        JMenu menu = new JMenu("檔案");
        DarkTheme.styleMenu(menu);

        JMenuItem save = menuItem("儲存結果...", KeyEvent.VK_S, e -> saveResult());
        JMenuItem load = menuItem("載入結果...", KeyEvent.VK_O, e -> loadResult());
        JMenuItem exit = menuItem("結束", -1, e -> dispose());

        menu.add(save);
        menu.add(load);
        menu.addSeparator();
        menu.add(exit);
        return menu;
    }

    private JMenu buildViewMenu() {
        JMenu menu = new JMenu("顯示");
        DarkTheme.styleMenu(menu);

        JMenuItem zoomIn = menuItem("放大字體", KeyEvent.VK_EQUALS, e -> adjustFontScale(FONT_SCALE_STEP));
        JMenuItem zoomOut = menuItem("縮小字體", KeyEvent.VK_MINUS, e -> adjustFontScale(-FONT_SCALE_STEP));
        JMenuItem zoomReset = menuItem("重設字體大小", KeyEvent.VK_0, e -> resetFontScale());

        menu.add(zoomIn);
        menu.add(zoomOut);
        menu.addSeparator();
        menu.add(zoomReset);
        return menu;
    }

    private JMenu buildHelpMenu() {
        JMenu menu = new JMenu("說明");
        DarkTheme.styleMenu(menu);

        JMenuItem about = menuItem("關於", -1, e -> showAbout());
        menu.add(about);
        return menu;
    }

    private JMenuItem menuItem(String text, int acceleratorKey, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        DarkTheme.styleMenuItem(item);
        if (acceleratorKey != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, InputEvent.CTRL_DOWN_MASK));
        }
        item.addActionListener(listener);
        return item;
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "子平八字排盤\n\n"
                        + "年柱以立春為界、月柱以十二節劃分、日柱以儒略日數推算、\n"
                        + "時柱以五鼠遁推算，並附十神與地支藏干十神。\n"
                        + "大運依性別與年干陰陽判定順排／逆排，起運歲數以\n"
                        + "出生時刻與最近節氣的距離換算（三日折一歲）。",
                "關於", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------------------------------------------- 版面

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(DarkTheme.BG_APP);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(buildInputPanel());
        center.add(Box.createVerticalStrut(22));
        center.add(buildFourPillarsPanel());
        center.add(Box.createVerticalStrut(22));
        center.add(buildDaYunPanel());
        center.add(Box.createVerticalStrut(22));
        center.add(buildSummaryPanel());

        JScrollPane scrollPane = new JScrollPane(center);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(DarkTheme.BG_APP);
        scrollPane.getViewport().setBackground(DarkTheme.BG_APP);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(780, 640));
        root.add(scrollPane, BorderLayout.CENTER);

        return root;
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        titleLabel.setForeground(DarkTheme.ACCENT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitleLabel.setForeground(DarkTheme.FG_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitleLabel);
        return panel;
    }

    private JComponent buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        DarkTheme.styleCard(panel);

        inputSectionTitle.setForeground(DarkTheme.ACCENT);
        panel.add(inputSectionTitle, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        row.setOpaque(false);

        row.add(labeledSpinner("年", yearSpinner));
        row.add(labeledSpinner("月", monthSpinner));
        row.add(labeledSpinner("日", daySpinner));
        row.add(labeledSpinner("時", hourSpinner));
        row.add(labeledSpinner("分", minuteSpinner));

        genderCombo.setPreferredSize(new Dimension(70, 30));
        genderCombo.setMaximumSize(new Dimension(80, 30));
        row.add(labeledComponent("性別", genderCombo));

        DarkTheme.styleAccentButton(calcButton);
        calcButton.addActionListener(e -> calculate());
        row.add(calcButton);

        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private JComponent labeledSpinner(String label, JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(70, 30));
        spinner.setMaximumSize(new Dimension(80, 30));
        return labeledComponent(label, spinner);
    }

    private JComponent labeledComponent(String label, JComponent component) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setForeground(DarkTheme.FG_SECONDARY);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputLabels.add(l);

        component.setAlignmentX(Component.CENTER_ALIGNMENT);

        wrap.add(l);
        wrap.add(Box.createVerticalStrut(4));
        wrap.add(component);
        return wrap;
    }

    private JComponent buildFourPillarsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        DarkTheme.styleCard(panel);

        fourPillarsSectionTitle.setForeground(DarkTheme.ACCENT);
        panel.add(fourPillarsSectionTitle, BorderLayout.NORTH);

        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        for (PillarPanel p : pillarPanels) {
            row.add(p);
        }
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildDaYunPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        DarkTheme.styleCard(panel);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        dayunTitleLabel.setForeground(DarkTheme.ACCENT);
        dayunTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dayunNoteLabel.setForeground(DarkTheme.FG_SECONDARY);
        dayunNoteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(dayunTitleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(dayunNoteLabel);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        for (int i = 0; i < BaziCalculator.DAYUN_PERIOD_COUNT; i++) {
            PillarPanel p = new PillarPanel("大運", true);
            dayunPanels.add(p);
            row.add(p);
            if (i < BaziCalculator.DAYUN_PERIOD_COUNT - 1) {
                row.add(Box.createHorizontalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(row,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(680, 240));

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        DarkTheme.styleCard(panel);

        summarySectionTitle.setForeground(DarkTheme.ACCENT);
        summarySectionTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        summaryLabel.setForeground(DarkTheme.FG_PRIMARY);
        summaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        noteLabel.setForeground(DarkTheme.FG_SECONDARY);
        noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(summarySectionTitle);
        panel.add(Box.createVerticalStrut(8));
        panel.add(summaryLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(noteLabel);
        return panel;
    }

    // ---------------------------------------------------------------- 字體縮放

    private void adjustFontScale(double delta) {
        fontScale = Math.max(FONT_SCALE_MIN, Math.min(FONT_SCALE_MAX, fontScale + delta));
        applyFontScale();
    }

    private void resetFontScale() {
        fontScale = 1.0;
        applyFontScale();
    }

    private void applyFontScale() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, (float) (TITLE_SIZE * fontScale)));
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont((float) (SUBTITLE_SIZE * fontScale)));
        for (JLabel l : inputLabels) {
            l.setFont(l.getFont().deriveFont((float) (INPUT_LABEL_SIZE * fontScale)));
        }
        for (JSpinner sp : spinners) {
            Component editor = sp.getEditor().getComponent(0);
            editor.setFont(editor.getFont().deriveFont((float) (SPINNER_SIZE * fontScale)));
        }
        genderCombo.setFont(genderCombo.getFont().deriveFont((float) (SPINNER_SIZE * fontScale)));
        calcButton.setFont(calcButton.getFont().deriveFont(Font.BOLD, (float) (BUTTON_SIZE * fontScale)));
        summaryLabel.setFont(summaryLabel.getFont().deriveFont((float) (SUMMARY_SIZE * fontScale)));
        noteLabel.setFont(noteLabel.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        dayunNoteLabel.setFont(dayunNoteLabel.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        for (JLabel l : sectionTitleLabels) {
            l.setFont(l.getFont().deriveFont(Font.BOLD, (float) (SECTION_TITLE_SIZE * fontScale)));
        }
        for (PillarPanel p : pillarPanels) {
            p.setFontScale(fontScale);
        }
        for (PillarPanel p : dayunPanels) {
            p.setFontScale(fontScale);
        }
        revalidate();
        repaint();
    }

    // ---------------------------------------------------------------- 排盤

    private BirthInput readInput() {
        int year = (int) yearSpinner.getValue();
        int month = (int) monthSpinner.getValue();
        int day = (int) daySpinner.getValue();
        int hour = (int) hourSpinner.getValue();
        int minute = (int) minuteSpinner.getValue();

        try {
            LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            JOptionPane.showMessageDialog(this, "日期不存在，請確認「日」是否符合該月份天數。",
                    "輸入錯誤", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Gender gender = (Gender) genderCombo.getSelectedItem();
        return new BirthInput(year, month, day, hour, minute, gender);
    }

    private void calculate() {
        BirthInput input = readInput();
        if (input == null) {
            return;
        }
        BaziResult result = calculator.calculate(input.year(), input.month(), input.day(), input.hour(), input.minute());
        renderResult(result);
        renderDaYun(result, input);
    }

    private void renderResult(BaziResult result) {
        yearPanel.setPillar(result.getYearPillar(), result.getYearTenGods());
        monthPanel.setPillar(result.getMonthPillar(), result.getMonthTenGods());
        dayPanel.setPillar(result.getDayPillar(), result.getDayTenGods());
        hourPanel.setPillar(result.getHourPillar(), result.getHourTenGods());

        summaryLabel.setText(String.format(
                "八字：%s年 %s月 %s日 %s時　｜　生肖：%s",
                result.getYearPillar().getGanZhi(),
                result.getMonthPillar().getGanZhi(),
                result.getDayPillar().getGanZhi(),
                result.getHourPillar().getGanZhi(),
                result.getZodiac()));

        StringBuilder note = new StringBuilder();
        note.append(String.format("年柱以立春為界，判定流年為西元 %d 年。", result.getEffectiveYear()));
        if (result.isEarlyZiShift()) {
            note.append("　23 時屬「晚子時」，日柱已順延至次日推算。");
        }
        noteLabel.setText(note.toString());
    }

    private void renderDaYun(BaziResult result, BirthInput input) {
        DaYunResult dayun = calculator.calculateDaYun(result, input.year(), input.month(), input.day(),
                input.hour(), input.minute(), input.gender());

        dayunNoteLabel.setText(String.format("%s命，年干%s，%s，起運約 %d 歲 %d 個月 %d 天",
                input.gender().getLabel(),
                result.getYearPillar().getStemIndex() % 2 == 0 ? "陽" : "陰",
                dayun.isForward() ? "順排" : "逆排",
                dayun.getStartAgeYears(), dayun.getStartAgeMonths(), dayun.getStartAgeDays()));

        List<DaYunPeriod> periods = dayun.getPeriods();
        for (int i = 0; i < dayunPanels.size() && i < periods.size(); i++) {
            DaYunPeriod period = periods.get(i);
            PillarPanel panel = dayunPanels.get(i);
            panel.setTitle(period.getStartAge() + "~" + period.getEndAge() + "歲");
            panel.setPillar(period.getPillar(), period.getTenGods());
        }
    }

    // ---------------------------------------------------------------- 儲存／載入

    private void saveResult() {
        BirthInput input = readInput();
        if (input == null) {
            return;
        }
        BaziResult result = calculator.calculate(input.year(), input.month(), input.day(), input.hour(), input.minute());

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("八字檔案 (*.bazi)", "bazi"));
        chooser.setSelectedFile(new File(String.format("八字_%04d%02d%02d.bazi",
                input.year(), input.month(), input.day())));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".bazi")) {
            file = new File(file.getParentFile(), file.getName() + ".bazi");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(this,
                file.getName() + " 已存在，是否覆蓋？", "確認覆蓋",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            BaziFileIO.save(file, input, result);
            JOptionPane.showMessageDialog(this, "已儲存至：" + file.getName(),
                    "儲存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "儲存失敗：" + ex.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadResult() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("八字檔案 (*.bazi)", "bazi"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            BirthInput input = BaziFileIO.load(file);
            yearSpinner.setValue(input.year());
            monthSpinner.setValue(input.month());
            daySpinner.setValue(input.day());
            hourSpinner.setValue(input.hour());
            minuteSpinner.setValue(input.minute());
            genderCombo.setSelectedItem(input.gender());
            calculate();
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "載入失敗：" + ex.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }
}
