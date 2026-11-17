package com.destiny.bazi.ui;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.DaYunPeriod;
import com.destiny.bazi.core.DaYunResult;
import com.destiny.bazi.core.FiveElementMass;
import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.LiuNianYear;
import com.destiny.bazi.core.LiuYueMonth;
import com.destiny.bazi.core.MonthOrder;
import com.destiny.bazi.io.BaziFileIO;
import com.destiny.bazi.io.BirthInput;
import com.destiny.bazi.io.PersonStore;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 子平八字排盤主視窗：輸入西元年月日時，計算並顯示四柱與十神；
 * 並提供選單列以儲存／載入排盤結果，以及調整畫面字體大小。
 */
public final class BaziFrame extends JFrame {

    private static final float TITLE_SIZE = 28f;
    private static final float SUBTITLE_SIZE = 15f;
    private static final float INPUT_LABEL_SIZE = 15f;
    private static final float SPINNER_SIZE = 16f;
    private static final float BUTTON_SIZE = 16f;
    private static final float SECONDARY_BUTTON_SIZE = 15f;
    private static final float SELECT_COMBO_SIZE = 15f;
    private static final float SUMMARY_SIZE = 16f;
    private static final float NOTE_SIZE = 14f;
    private static final float SECTION_TITLE_SIZE = 17f;

    /** 輸入元件的基準尺寸（字體縮放時等比例調整，避免放大後文字被裁切）。 */
    private static final String BASE_SIZE_KEY = "destiny.baseSize";
    private static final int INPUT_HEIGHT = 34;

    private static final double FONT_SCALE_MIN = 0.7;
    private static final double FONT_SCALE_MAX = 1.8;
    private static final double FONT_SCALE_STEP = 0.1;

    private final BaziCalculator calculator = new BaziCalculator();

    private final JSpinner yearSpinner;
    private final JSpinner monthSpinner;
    private final JSpinner daySpinner;
    private final JSpinner minuteSpinner;
    private final JSpinner[] spinners;
    private final JComboBox<String> hourCombo = new JComboBox<>(buildHourOptions());
    private final JComboBox<Gender> genderCombo = new JComboBox<>(Gender.values());

    private final PillarPanel yearPanel = new PillarPanel("年柱");
    private final PillarPanel monthPanel = new PillarPanel("月柱");
    private final PillarPanel dayPanel = new PillarPanel("日柱");
    private final PillarPanel hourPanel = new PillarPanel("時柱");
    private final PillarPanel[] pillarPanels = {yearPanel, monthPanel, dayPanel, hourPanel};

    private final List<PillarPanel> dayunPanels = new ArrayList<>();
    private final JLabel dayunTitleLabel = new JLabel("大運", SwingConstants.CENTER);
    private final JLabel dayunNoteLabel = new JLabel(" ", SwingConstants.CENTER);

    private final JComboBox<String> dayunSelectCombo = fittedComboBox();
    private final JComboBox<String> liunianSelectCombo = fittedComboBox();
    private final JComboBox<String> liuyueSelectCombo = fittedComboBox();
    private final PillarPanel selectedDaYunPanel = new PillarPanel("大運");
    private final PillarPanel selectedLiuNianPanel = new PillarPanel("流年");
    private final PillarPanel selectedLiuYuePanel = new PillarPanel("流月");
    private final JLabel selectionSectionTitle = new JLabel("大運／流年／流月查詢", SwingConstants.CENTER);
    private final JLabel luckFiveElementMassTitle = new JLabel("五行質量（命盤＋大運＋流年）", SwingConstants.CENTER);
    private final FiveElementBarChart luckFiveElementMassChart = new FiveElementBarChart();
    private final TenGodStatsPanel luckTenGodStats = new TenGodStatsPanel();
    private boolean updatingCombos = false;
    /** 程式自行設定輸入欄位時（載入檔案、調整日上限）暫停自動重新排盤。 */
    private boolean suppressAutoCalc = false;

    /** 「今天」所屬的流年（以立春為界）與流月索引（0=正月寅），用於預設定位與標示目前大運。 */
    private int todayLiuNianYear;
    private int todayLiuYueIndex;

    private List<DaYunPeriod> currentPeriods = new ArrayList<>();
    private final List<Integer> currentLiuNianYears = new ArrayList<>();
    private Integer currentLiuYueYear;
    private BaziResult currentResult;
    private BirthInput currentInput;

    private final JLabel titleLabel = new JLabel("子平八字排盤", SwingConstants.CENTER);
    private final JLabel subtitleLabel = new JLabel("輸入出生年、月、日、時、性別，推算四柱、十神與大運", SwingConstants.CENTER);
    private final JLabel inputSectionTitle = new JLabel("排盤輸入", SwingConstants.CENTER);
    private final JLabel fourPillarsSectionTitle = new JLabel("四柱", SwingConstants.CENTER);
    private final JLabel summarySectionTitle = new JLabel("命盤摘要", SwingConstants.CENTER);
    private final List<JLabel> sectionTitleLabels = List.of(
            inputSectionTitle, fourPillarsSectionTitle, dayunTitleLabel, selectionSectionTitle, summarySectionTitle);
    private final List<JLabel> inputLabels = new ArrayList<>();
    private final JButton calcButton = new JButton("排盤");

    private final JTextField nameField = new JTextField();
    private final JButton savePersonButton = new JButton("儲存人員");
    private final JButton openPeopleButton = new JButton("人員查詢…");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Timer statusTimer = new Timer(4000, e -> statusLabel.setText(" "));
    private final PersonStore personStore = PersonStore.openDefault();
    private PersonDialog personDialog;

    private final JLabel summaryLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel noteLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel monthOrderLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel fiveElementMassTitle = new JLabel("五行質量（基本分＋月令加分）", SwingConstants.CENTER);
    private final FiveElementBarChart fiveElementMassChart = new FiveElementBarChart();
    private final TenGodStatsPanel fiveElementTenGodStats = new TenGodStatsPanel();

    private JPanel exportPanel;

    private double fontScale = 1.0;

    public BaziFrame() {
        super(AppInfo.NAME + " v" + AppInfo.VERSION);

        int nowYear = Year.now().getValue();
        yearSpinner = new JSpinner(new SpinnerNumberModel(nowYear, 1900, 2100, 1));
        monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        spinners = new JSpinner[]{yearSpinner, monthSpinner, daySpinner, minuteSpinner};
        for (JSpinner sp : spinners) {
            sp.setEditor(new JSpinner.NumberEditor(sp, "#"));
            DarkTheme.styleSpinner(sp);
            // 輸入合法數字即生效（不必按 Enter 或移開焦點），配合下方的即時重新排盤。
            JFormattedTextField field = ((JSpinner.DefaultEditor) sp.getEditor()).getTextField();
            if (field.getFormatter() instanceof DefaultFormatter formatter) {
                formatter.setCommitsOnValidEdit(true);
            }
        }
        // 年、月變動時先依該月天數調整「日」的上限，再重新排盤；任何輸入變動都立即重排。
        yearSpinner.addChangeListener(e -> {
            updateDayLimit();
            autoCalculate();
        });
        monthSpinner.addChangeListener(e -> {
            updateDayLimit();
            autoCalculate();
        });
        daySpinner.addChangeListener(e -> autoCalculate());
        minuteSpinner.addChangeListener(e -> autoCalculate());

        for (JComboBox<?> combo : List.of(hourCombo, genderCombo, dayunSelectCombo, liunianSelectCombo, liuyueSelectCombo)) {
            DarkTheme.styleComboBox(combo);
        }
        hourCombo.addActionListener(e -> autoCalculate());
        genderCombo.addActionListener(e -> autoCalculate());
        dayPanel.setEmphasized(true);

        DarkTheme.styleTextField(nameField);
        nameField.setToolTipText("選填；填寫後可按「儲存人員」存入人員資料");
        nameField.addActionListener(e -> savePerson());
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                refreshSummary();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                refreshSummary();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                refreshSummary();
            }
        });
        statusTimer.setRepeats(false);
        dayunSelectCombo.addActionListener(e -> {
            if (!updatingCombos) {
                onDaYunSelectionChanged();
            }
        });
        liunianSelectCombo.addActionListener(e -> {
            if (!updatingCombos) {
                onLiuNianSelectionChanged();
            }
        });
        liuyueSelectCombo.addActionListener(e -> {
            if (!updatingCombos) {
                onLiuYueSelectionChanged();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        setMinimumSize(new Dimension(780, 600));
        pack();
        setLocationRelativeTo(null);

        updateDayLimit();
        applyFontScale();
        calculate();
    }

    private void autoCalculate() {
        if (!suppressAutoCalc) {
            calculate();
        }
    }

    /** 依目前年、月將「日」的上限設為該月天數，超出時自動改為月底，避免出現不存在的日期。 */
    private void updateDayLimit() {
        int year = (int) yearSpinner.getValue();
        int month = (int) monthSpinner.getValue();
        int length = YearMonth.of(year, month).lengthOfMonth();
        SpinnerNumberModel model = (SpinnerNumberModel) daySpinner.getModel();
        boolean previous = suppressAutoCalc;
        suppressAutoCalc = true;
        try {
            model.setMaximum(length);
            if ((int) model.getValue() > length) {
                model.setValue(length);
            }
        } finally {
            suppressAutoCalc = previous;
        }
    }

    /**
     * 查詢區用的下拉選單：最大不超過所需寬度（視窗很寬時不被拉長），
     * 空間不足時可縮小到 60px（截斷尾端文字），寬度隨字體縮放即時更新。
     */
    private static JComboBox<String> fittedComboBox() {
        return new JComboBox<>() {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(60, getPreferredSize().height);
            }
        };
    }

    /** 產生「時」下拉選單的選項：0~23 時，並附上對應的時辰地支。 */
    private static String[] buildHourOptions() {
        String[] options = new String[24];
        for (int h = 0; h < 24; h++) {
            int branchIdx = ((h + 1) / 2) % 12;
            options[h] = h + "時（" + GanZhi.BRANCHES[branchIdx] + "）";
        }
        return options;
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

        JMenuItem savePerson = menuItem("儲存人員", KeyEvent.VK_D, e -> savePerson());
        JMenuItem openPeople = menuItem("人員查詢...", KeyEvent.VK_F, e -> openPeople());
        JMenuItem save = menuItem("儲存結果...", KeyEvent.VK_S, e -> saveResult());
        JMenuItem load = menuItem("載入結果...", KeyEvent.VK_O, e -> loadResult());
        JMenuItem exportImage = menuItem("匯出圖片...", KeyEvent.VK_E, e -> exportImage());
        JMenuItem exit = menuItem("結束", -1, e -> dispose());

        menu.add(savePerson);
        menu.add(openPeople);
        menu.addSeparator();
        menu.add(save);
        menu.add(load);
        menu.addSeparator();
        menu.add(exportImage);
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
                AppInfo.NAME + "　版本 " + AppInfo.VERSION + "\n\n"
                        + "年柱以立春為界、月柱以十二節劃分、日柱以儒略日數推算、\n"
                        + "時柱以五鼠遁推算，並附十神與地支藏干十神。\n"
                        + "大運依性別與年干陰陽判定順排／逆排，起運歲數以\n"
                        + "出生時刻與最近節氣的距離換算（三日折一歲），\n"
                        + "出生日期加上起運歲數即為交運日期，大運自交運當年起算。\n"
                        + "選定大運後可進一步選擇流年、流月，查看該年/月的\n"
                        + "干支、十神與地支藏干。",
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
        center.add(buildSummaryPanel());
        center.add(Box.createVerticalStrut(22));
        center.add(buildDaYunPanel());
        center.add(Box.createVerticalStrut(22));
        center.add(buildSelectionPanel());
        exportPanel = center;

        // 內容寬度跟隨視窗（只垂直捲動），並靠上排列：視窗高於內容時不拉伸各區塊卡片。
        JPanel holder = new WidthTrackingPanel();
        holder.setOpaque(false);
        holder.add(center, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(holder);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(DarkTheme.BG_APP);
        scrollPane.getViewport().setBackground(DarkTheme.BG_APP);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        DarkTheme.styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(780, 680));
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

        // 第一列：姓名與人員資料操作；第二列：出生資料與排盤；第三列：操作結果提示。
        JPanel personRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        personRow.setOpaque(false);
        setBaseSize(nameField, 200);
        personRow.add(labeledComponent("姓名", nameField));
        DarkTheme.styleSecondaryButton(savePersonButton);
        DarkTheme.styleSecondaryButton(openPeopleButton);
        savePersonButton.setToolTipText("以姓名為準存入人員資料，同名者會詢問是否覆蓋（Ctrl+D）");
        openPeopleButton.setToolTipText("開啟人員查詢視窗，選擇人員帶入資料（Ctrl+F）");
        savePersonButton.addActionListener(e -> savePerson());
        openPeopleButton.addActionListener(e -> openPeople());
        personRow.add(labeledComponent(" ", savePersonButton));
        personRow.add(labeledComponent(" ", openPeopleButton));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        row.setOpaque(false);

        row.add(labeledSpinner("年", yearSpinner, 84));
        row.add(labeledSpinner("月", monthSpinner, 64));
        row.add(labeledSpinner("日", daySpinner, 64));

        setBaseSize(hourCombo, 140);
        row.add(labeledComponent("時", hourCombo));

        row.add(labeledSpinner("分", minuteSpinner, 64));

        setBaseSize(genderCombo, 72);
        row.add(labeledComponent("性別", genderCombo));

        DarkTheme.styleAccentButton(calcButton);
        calcButton.addActionListener(e -> calculate());
        row.add(labeledComponent(" ", calcButton));

        statusLabel.setForeground(DarkTheme.ACCENT);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(personRow);
        rows.add(row);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rows.add(statusLabel);

        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private JComponent labeledSpinner(String label, JSpinner spinner, int width) {
        setBaseSize(spinner, width);
        return labeledComponent(label, spinner);
    }

    /** 記錄輸入元件在字體縮放 1.0 時的寬度，實際尺寸於 applyFontScale 依比例設定。 */
    private static void setBaseSize(JComponent c, int width) {
        c.putClientProperty(BASE_SIZE_KEY, width);
    }

    private void scaleInputSize(JComponent c) {
        Object base = c.getClientProperty(BASE_SIZE_KEY);
        if (base instanceof Integer width) {
            Dimension size = new Dimension((int) Math.round(width * fontScale),
                    (int) Math.round(INPUT_HEIGHT * fontScale));
            c.setPreferredSize(size);
            c.setMaximumSize(size);
        }
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

        DaYunRow row = new DaYunRow(BaziCalculator.DAYUN_PERIOD_COUNT);
        for (int i = 0; i < BaziCalculator.DAYUN_PERIOD_COUNT; i++) {
            PillarPanel p = new PillarPanel("大運", true);
            int index = i;
            p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.setToolTipText("點選查看此大運的流年、流月");
            p.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (index < dayunSelectCombo.getItemCount()) {
                        dayunSelectCombo.setSelectedIndex(index);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    p.setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    p.setHovered(false);
                }
            });
            dayunPanels.add(p);
            row.add(p);
        }

        // 視窗夠寬時卡片平均分配寬度、不出現捲軸；不夠寬（或字體放大）時才可水平捲動。
        // 高度依卡片實際所需，需要捲軸時再加上捲軸高度，避免卡片底部的藏干被裁切。
        JScrollPane scroll = new JScrollPane(row,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override
            public Dimension getPreferredSize() {
                int height = row.getPreferredSize().height + 4;
                if (!row.fitsViewport()) {
                    height += getHorizontalScrollBar().getPreferredSize().height;
                }
                return new Dimension(680, height);
            }
        };
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        DarkTheme.styleScrollPane(scroll);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        // 視窗寬度改變而跨越「放得下／放不下」時，重新計算高度（加入或移除捲軸空間）。
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            private boolean lastFits = row.fitsViewport();

            @Override
            public void componentResized(ComponentEvent e) {
                boolean fits = row.fitsViewport();
                if (fits != lastFits) {
                    lastFits = fits;
                    // 捲動區本身是 validate root，需從外層卡片重新排版，高度變化才會生效。
                    SwingUtilities.invokeLater(() -> {
                        if (scroll.getParent() instanceof JComponent parent) {
                            parent.revalidate();
                            parent.repaint();
                        }
                    });
                }
            }
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        DarkTheme.styleCard(panel);

        selectionSectionTitle.setForeground(DarkTheme.ACCENT);
        panel.add(selectionSectionTitle, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // 三個選單依各自所需寬度按比例分配：視窗夠寬時完整顯示並置中，
        // 視窗較窄時等比例縮小（僅截斷尾端標記文字），不會被擠出畫面。
        JPanel comboRow = new JPanel(new GridBagLayout());
        comboRow.setOpaque(false);
        comboRow.setBorder(new EmptyBorder(6, 0, 6, 0));
        // 以最長可能的選項文字決定寬度（含「目前／今年／本月」標記），避免文字被截斷，
        // 也避免切換命盤時選單寬度跳動；寬度會隨字體縮放自動調整。
        dayunSelectCombo.setPrototypeDisplayValue("2100~2109年（100~109歲）　甲子　● 目前");
        liunianSelectCombo.setPrototypeDisplayValue("2100年（100歲）　甲子　● 今年");
        liuyueSelectCombo.setPrototypeDisplayValue("十二月　甲子　● 本月");
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 7, 0, 7);
        String[] comboLabels = {"選擇大運", "選擇流年", "選擇流月"};
        List<JComboBox<String>> selectCombos = List.of(dayunSelectCombo, liunianSelectCombo, liuyueSelectCombo);
        for (int i = 0; i < selectCombos.size(); i++) {
            gc.gridx = i;
            gc.weightx = selectCombos.get(i).getPreferredSize().width;
            comboRow.add(labeledComponent(comboLabels[i], selectCombos.get(i)), gc);
        }

        JPanel panelsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        panelsRow.setOpaque(false);
        panelsRow.add(selectedDaYunPanel);
        panelsRow.add(selectedLiuNianPanel);
        panelsRow.add(selectedLiuYuePanel);

        luckFiveElementMassTitle.setForeground(DarkTheme.FG_SECONDARY);
        luckFiveElementMassTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel luckMassRow = new JPanel();
        luckMassRow.setOpaque(false);
        luckMassRow.setLayout(new BoxLayout(luckMassRow, BoxLayout.X_AXIS));
        luckMassRow.add(Box.createHorizontalGlue());
        luckMassRow.add(luckFiveElementMassChart);
        luckMassRow.add(Box.createHorizontalStrut(24));
        luckMassRow.add(luckTenGodStats);
        luckMassRow.add(Box.createHorizontalGlue());
        luckMassRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(comboRow);
        content.add(Box.createVerticalStrut(10));
        content.add(panelsRow);
        content.add(Box.createVerticalStrut(10));
        content.add(luckFiveElementMassTitle);
        content.add(Box.createVerticalStrut(4));
        content.add(luckMassRow);

        panel.add(content, BorderLayout.CENTER);
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

        monthOrderLabel.setForeground(DarkTheme.FG_SECONDARY);
        monthOrderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        fiveElementMassTitle.setForeground(DarkTheme.FG_SECONDARY);
        fiveElementMassTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel massRow = new JPanel();
        massRow.setOpaque(false);
        massRow.setLayout(new BoxLayout(massRow, BoxLayout.X_AXIS));
        massRow.add(Box.createHorizontalGlue());
        massRow.add(fiveElementMassChart);
        massRow.add(Box.createHorizontalStrut(24));
        massRow.add(fiveElementTenGodStats);
        massRow.add(Box.createHorizontalGlue());
        massRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(summarySectionTitle);
        panel.add(Box.createVerticalStrut(8));
        panel.add(summaryLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(noteLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(monthOrderLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(fiveElementMassTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(massRow);
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
        for (JComponent c : List.of(nameField, yearSpinner, monthSpinner, daySpinner, minuteSpinner,
                hourCombo, genderCombo)) {
            scaleInputSize(c);
        }
        nameField.setFont(nameField.getFont().deriveFont((float) (SPINNER_SIZE * fontScale)));
        for (JButton b : List.of(savePersonButton, openPeopleButton)) {
            b.setFont(b.getFont().deriveFont((float) (SECONDARY_BUTTON_SIZE * fontScale)));
        }
        statusLabel.setFont(statusLabel.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        if (personDialog != null) {
            personDialog.setFontScale(fontScale);
        }
        hourCombo.setFont(hourCombo.getFont().deriveFont((float) (SPINNER_SIZE * fontScale)));
        genderCombo.setFont(genderCombo.getFont().deriveFont((float) (SPINNER_SIZE * fontScale)));
        // 查詢選單選項文字較長，字級略小於輸入欄位，讓一般視窗寬度下能完整顯示。
        dayunSelectCombo.setFont(dayunSelectCombo.getFont().deriveFont((float) (SELECT_COMBO_SIZE * fontScale)));
        liunianSelectCombo.setFont(liunianSelectCombo.getFont().deriveFont((float) (SELECT_COMBO_SIZE * fontScale)));
        liuyueSelectCombo.setFont(liuyueSelectCombo.getFont().deriveFont((float) (SELECT_COMBO_SIZE * fontScale)));
        calcButton.setFont(calcButton.getFont().deriveFont(Font.BOLD, (float) (BUTTON_SIZE * fontScale)));
        summaryLabel.setFont(summaryLabel.getFont().deriveFont((float) (SUMMARY_SIZE * fontScale)));
        noteLabel.setFont(noteLabel.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        monthOrderLabel.setFont(monthOrderLabel.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        fiveElementMassTitle.setFont(fiveElementMassTitle.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        luckFiveElementMassTitle.setFont(luckFiveElementMassTitle.getFont().deriveFont((float) (NOTE_SIZE * fontScale)));
        fiveElementMassChart.setFontScale(fontScale);
        luckFiveElementMassChart.setFontScale(fontScale);
        fiveElementTenGodStats.setFontScale(fontScale);
        luckTenGodStats.setFontScale(fontScale);
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
        selectedDaYunPanel.setFontScale(fontScale);
        selectedLiuNianPanel.setFontScale(fontScale);
        selectedLiuYuePanel.setFontScale(fontScale);
        revalidate();
        repaint();
    }

    // ---------------------------------------------------------------- 排盤

    private BirthInput readInput() {
        int year = (int) yearSpinner.getValue();
        int month = (int) monthSpinner.getValue();
        int day = (int) daySpinner.getValue();
        int hour = hourCombo.getSelectedIndex();
        int minute = (int) minuteSpinner.getValue();

        try {
            LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            JOptionPane.showMessageDialog(this, "日期不存在，請確認「日」是否符合該月份天數。",
                    "輸入錯誤", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Gender gender = (Gender) genderCombo.getSelectedItem();
        return new BirthInput(year, month, day, hour, minute, gender, nameField.getText());
    }

    private void calculate() {
        BirthInput input = readInput();
        if (input == null) {
            return;
        }
        updateToday();
        BaziResult result = calculator.calculate(input.year(), input.month(), input.day(), input.hour(), input.minute());
        renderResult(result);
        DaYunResult dayun = renderDaYun(result, input);

        currentResult = result;
        currentInput = input;
        currentPeriods = dayun.getPeriods();
        populateDaYunCombo();
    }

    /** 以今天的日期排盤，取得今天所屬的流年（立春為界）與流月（節氣為界）。 */
    private void updateToday() {
        LocalDateTime now = LocalDateTime.now();
        BaziResult today = calculator.calculate(now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute());
        todayLiuNianYear = today.getEffectiveYear();
        // 月支索引：寅=2 為正月（索引 0），丑=1 為十二月（索引 11）。
        todayLiuYueIndex = (today.getMonthPillar().getBranchIndex() + 10) % 12;
    }

    private boolean isCurrentPeriod(DaYunPeriod period) {
        return todayLiuNianYear >= period.getStartYear() && todayLiuNianYear <= period.getEndYear();
    }

    private void renderResult(BaziResult result) {
        yearPanel.setPillar(result.getYearPillar(), result.getYearTenGods());
        monthPanel.setPillar(result.getMonthPillar(), result.getMonthTenGods());
        dayPanel.setPillar(result.getDayPillar(), result.getDayTenGods());
        hourPanel.setPillar(result.getHourPillar(), result.getHourTenGods());

        currentResult = result;
        refreshSummary();

        StringBuilder note = new StringBuilder();
        note.append(String.format("年柱以立春為界，判定流年為西元 %d 年。", result.getEffectiveYear()));
        if (result.isEarlyZiShift()) {
            note.append("　23 時屬「晚子時」，日柱已順延至次日推算。");
        }
        noteLabel.setText(note.toString());

        MonthOrder mo = result.getMonthOrder();
        monthOrderLabel.setText(String.format(
                "月令：%s（%s月%s：%s%s當令，入節第 %d 天）",
                mo.getElement(),
                result.getMonthPillar().getBranch(),
                mo.getPhase(),
                mo.getStem(),
                mo.getElement(),
                mo.getDayOfMonth()));

        FiveElementMass mass = calculator.calculateFiveElementMass(result);
        fiveElementMassChart.setMass(mass);
        fiveElementTenGodStats.setCounts(calculator.calculateTenGodCounts(result));
    }

    /** 命盤摘要第一行：有姓名時一併顯示（姓名變動時即時更新，不需重新排盤）。 */
    private void refreshSummary() {
        if (currentResult == null) {
            return;
        }
        String name = BirthInput.normalizeName(nameField.getText());
        summaryLabel.setText((name.isEmpty() ? "" : "姓名：" + name + "　｜　") + String.format(
                "八字：%s年 %s月 %s日 %s時　｜　生肖：%s",
                currentResult.getYearPillar().getGanZhi(),
                currentResult.getMonthPillar().getGanZhi(),
                currentResult.getDayPillar().getGanZhi(),
                currentResult.getHourPillar().getGanZhi(),
                currentResult.getZodiac()));
    }

    private DaYunResult renderDaYun(BaziResult result, BirthInput input) {
        DaYunResult dayun = calculator.calculateDaYun(result, input.year(), input.month(), input.day(),
                input.hour(), input.minute(), input.gender());

        LocalDate startDate = dayun.getStartDate();
        dayunNoteLabel.setText(String.format("%s命，年干%s，%s，起運約 %d 歲 %d 個月 %d 天，約 %d 年 %d 月 %d 日交運",
                input.gender().getLabel(),
                result.getYearPillar().getStemIndex() % 2 == 0 ? "陽" : "陰",
                dayun.isForward() ? "順排" : "逆排",
                dayun.getStartAgeYears(), dayun.getStartAgeMonths(), dayun.getStartAgeDays(),
                startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth()));

        List<DaYunPeriod> periods = dayun.getPeriods();
        for (int i = 0; i < dayunPanels.size() && i < periods.size(); i++) {
            DaYunPeriod period = periods.get(i);
            PillarPanel panel = dayunPanels.get(i);
            panel.setTitle("<html><center>" + period.getStartYear() + "~" + period.getEndYear()
                    + "<br>" + period.getStartAge() + "~" + period.getEndAge() + "歲</center></html>");
            panel.setPillar(period.getPillar(), period.getTenGods());
            panel.setBadge(isCurrentPeriod(period) ? "● 目前" : null);
        }
        return dayun;
    }

    // ------------------------------------------------------- 大運／流年選擇查詢

    private void populateDaYunCombo() {
        updatingCombos = true;
        dayunSelectCombo.removeAllItems();
        int selectIdx = 0;
        for (int i = 0; i < currentPeriods.size(); i++) {
            DaYunPeriod period = currentPeriods.get(i);
            boolean current = isCurrentPeriod(period);
            if (current) {
                selectIdx = i;
            }
            dayunSelectCombo.addItem(period.getStartYear() + "~" + period.getEndYear() + "年（"
                    + period.getStartAge() + "~" + period.getEndAge() + "歲）　" + period.getPillar().getGanZhi()
                    + (current ? "　● 目前" : ""));
        }
        // 尚未起運時停在第一運；已走完全部大運時停在最後一運。
        if (!currentPeriods.isEmpty() && todayLiuNianYear > currentPeriods.get(currentPeriods.size() - 1).getEndYear()) {
            selectIdx = currentPeriods.size() - 1;
        }
        if (dayunSelectCombo.getItemCount() > 0) {
            dayunSelectCombo.setSelectedIndex(selectIdx);
        }
        updatingCombos = false;
        onDaYunSelectionChanged();
    }

    private void onDaYunSelectionChanged() {
        int idx = dayunSelectCombo.getSelectedIndex();
        if (idx < 0 || idx >= currentPeriods.size()) {
            return;
        }
        DaYunPeriod period = currentPeriods.get(idx);
        for (int i = 0; i < dayunPanels.size(); i++) {
            dayunPanels.get(i).setSelected(i == idx);
        }
        selectedDaYunPanel.setTitle("大運 " + period.getStartYear() + "~" + period.getEndYear() + "年");
        selectedDaYunPanel.setPillar(period.getPillar(), period.getTenGods());

        populateLiuNianCombo(period);
    }

    private void populateLiuNianCombo(DaYunPeriod period) {
        updatingCombos = true;
        liunianSelectCombo.removeAllItems();
        currentLiuNianYears.clear();
        int dayStemIdx = currentResult.getDayPillar().getStemIndex();
        for (int calendarYear = period.getStartYear(); calendarYear <= period.getEndYear(); calendarYear++) {
            int age = calendarYear - currentInput.year();
            currentLiuNianYears.add(calendarYear);
            LiuNianYear liuNian = calculator.calculateLiuNian(calendarYear, dayStemIdx);
            boolean thisYear = calendarYear == todayLiuNianYear;
            liunianSelectCombo.addItem(calendarYear + "年（" + age + "歲）　" + liuNian.getPillar().getGanZhi()
                    + (thisYear ? "　● 今年" : ""));
        }
        if (liunianSelectCombo.getItemCount() > 0) {
            int thisYearIdx = currentLiuNianYears.indexOf(todayLiuNianYear);
            liunianSelectCombo.setSelectedIndex(Math.max(0, thisYearIdx));
        }
        updatingCombos = false;
        onLiuNianSelectionChanged();
    }

    private void onLiuNianSelectionChanged() {
        int idx = liunianSelectCombo.getSelectedIndex();
        int dayunIdx = dayunSelectCombo.getSelectedIndex();
        if (idx < 0 || idx >= currentLiuNianYears.size() || currentResult == null
                || dayunIdx < 0 || dayunIdx >= currentPeriods.size()) {
            return;
        }
        int calendarYear = currentLiuNianYears.get(idx);
        int dayStemIdx = currentResult.getDayPillar().getStemIndex();
        LiuNianYear liuNian = calculator.calculateLiuNian(calendarYear, dayStemIdx);
        selectedLiuNianPanel.setTitle("流年 " + calendarYear + "年");
        selectedLiuNianPanel.setPillar(liuNian.getPillar(), liuNian.getTenGods());

        DaYunPeriod period = currentPeriods.get(dayunIdx);
        FiveElementMass luckMass = calculator.calculateFiveElementMassWithLuck(
                currentResult, period.getPillar(), liuNian.getPillar());
        luckFiveElementMassChart.setMass(luckMass);
        luckTenGodStats.setCounts(calculator.calculateTenGodCountsWithLuck(
                currentResult, period.getTenGods(), liuNian.getTenGods()));

        populateLiuYueCombo(calendarYear);
    }

    private void populateLiuYueCombo(int liuNianYear) {
        updatingCombos = true;
        liuyueSelectCombo.removeAllItems();
        currentLiuYueYear = liuNianYear;
        int dayStemIdx = currentResult.getDayPillar().getStemIndex();
        for (int m = 0; m < 12; m++) {
            LiuYueMonth liuYue = calculator.calculateLiuYue(liuNianYear, m, dayStemIdx);
            boolean thisMonth = liuNianYear == todayLiuNianYear && m == todayLiuYueIndex;
            liuyueSelectCombo.addItem(liuYue.getMonthName() + "　" + liuYue.getPillar().getGanZhi()
                    + (thisMonth ? "　● 本月" : ""));
        }
        if (liuyueSelectCombo.getItemCount() > 0) {
            liuyueSelectCombo.setSelectedIndex(liuNianYear == todayLiuNianYear ? todayLiuYueIndex : 0);
        }
        updatingCombos = false;
        onLiuYueSelectionChanged();
    }

    private void onLiuYueSelectionChanged() {
        int idx = liuyueSelectCombo.getSelectedIndex();
        if (idx < 0 || currentResult == null || currentLiuYueYear == null) {
            return;
        }
        int dayStemIdx = currentResult.getDayPillar().getStemIndex();
        LiuYueMonth liuYue = calculator.calculateLiuYue(currentLiuYueYear, idx, dayStemIdx);
        selectedLiuYuePanel.setTitle("流月 " + currentLiuYueYear + "年" + liuYue.getMonthName());
        selectedLiuYuePanel.setPillar(liuYue.getPillar(), liuYue.getTenGods());
    }

    // ---------------------------------------------------------------- 匯出圖片

    private void exportImage() {
        if (exportPanel == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG 圖片 (*.png)", "png"));
        chooser.setSelectedFile(new File(defaultFileBaseName() + ".png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(this,
                file.getName() + " 已存在，是否覆蓋？", "確認覆蓋",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        // 依內容實際所需大小繪製，不受目前視窗大小或捲動位置影響。
        Dimension originalSize = exportPanel.getSize();
        Dimension targetSize = exportPanel.getPreferredSize();

        BufferedImage image = new BufferedImage(targetSize.width, targetSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(DarkTheme.BG_APP);
        g2.fillRect(0, 0, targetSize.width, targetSize.height);

        exportPanel.setSize(targetSize);
        exportPanel.validate();
        exportPanel.printAll(g2);
        g2.dispose();

        exportPanel.setSize(originalSize);
        exportPanel.validate();

        try {
            ImageIO.write(image, "png", file);
            JOptionPane.showMessageDialog(this, "已匯出至：" + file.getName(),
                    "匯出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "匯出失敗：" + ex.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
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
        chooser.setSelectedFile(new File(defaultFileBaseName() + ".bazi"));
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
            applyInput(BaziFileIO.load(file));
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "載入失敗：" + ex.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------- 人員資料

    /** 預設檔名：有姓名時以「姓名_出生日期」命名，否則為「八字_出生日期」。 */
    private String defaultFileBaseName() {
        String name = BirthInput.normalizeName(nameField.getText()).replaceAll("[\\\\/:*?\"<>|]", "_");
        return String.format("%s_%04d%02d%02d", name.isEmpty() ? "八字" : name,
                (int) yearSpinner.getValue(), (int) monthSpinner.getValue(), (int) daySpinner.getValue());
    }

    /** 把一筆輸入（含姓名）填入畫面並排盤；逐欄設定時暫停自動重排，最後只排盤一次。 */
    private void applyInput(BirthInput input) {
        suppressAutoCalc = true;
        try {
            nameField.setText(input.name());
            yearSpinner.setValue(input.year());
            monthSpinner.setValue(input.month());
            updateDayLimit();
            daySpinner.setValue(input.day());
            hourCombo.setSelectedIndex(input.hour());
            minuteSpinner.setValue(input.minute());
            genderCombo.setSelectedItem(input.gender());
        } finally {
            suppressAutoCalc = false;
        }
        calculate();
    }

    /** 以姓名為鍵把目前輸入存入人員資料；同名但出生資料不同時先詢問是否覆蓋。 */
    private void savePerson() {
        BirthInput input = readInput();
        if (input == null) {
            return;
        }
        if (!input.hasName()) {
            showStatus("請先輸入姓名，再儲存人員");
            nameField.requestFocusInWindow();
            return;
        }
        try {
            BirthInput existing = personStore.find(input.name());
            if (existing != null && existing.sameBirth(input) && existing.name().equals(input.name())) {
                showStatus("「" + input.name() + "」的資料已是最新，不需再儲存");
                return;
            }
            if (existing != null && !existing.sameBirth(input) && JOptionPane.showConfirmDialog(this,
                    "人員資料中已有「" + existing.name() + "」，是否以目前輸入的出生資料覆蓋？",
                    "覆蓋人員資料", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            boolean replaced = personStore.put(input);
            showStatus((replaced ? "已更新「" : "已儲存「") + input.name() + "」至人員資料");
            if (personDialog != null) {
                personDialog.refresh();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "儲存人員失敗：" + ex.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openPeople() {
        if (personDialog == null) {
            personDialog = new PersonDialog(this, personStore, person -> {
                applyInput(person);
                showStatus("已帶入「" + person.name() + "」");
            });
            personDialog.setFontScale(fontScale);
        }
        personDialog.open();
    }

    /** 在輸入區下方顯示操作結果，數秒後自動清除（取代需要按確定的訊息視窗）。 */
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusTimer.restart();
    }

    /**
     * 大運卡片列：以等寬網格排列；捲動區夠寬時跟隨捲動區寬度（不出現捲軸），
     * 不夠寬時維持卡片所需寬度並改為水平捲動。
     */
    private static final class DaYunRow extends JPanel implements Scrollable {

        DaYunRow(int columns) {
            super(new GridLayout(1, columns, 8, 0));
            setOpaque(false);
        }

        boolean fitsViewport() {
            return getParent() instanceof JViewport viewport
                    && viewport.getWidth() > 0
                    && viewport.getWidth() >= getPreferredSize().width;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return fitsViewport();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** 寬度永遠等於捲動區可視寬度的內容面板：較窄的視窗會壓縮內容，而不是出現水平捲動。 */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {

        WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }
}
