package com.destiny.bazi.ui;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;

/**
 * 子平八字排盤主視窗：輸入西元年月日時，計算並顯示四柱。
 */
public final class BaziFrame extends JFrame {

    private final BaziCalculator calculator = new BaziCalculator();

    private final JSpinner yearSpinner;
    private final JSpinner monthSpinner;
    private final JSpinner daySpinner;
    private final JSpinner hourSpinner;
    private final JSpinner minuteSpinner;

    private final PillarPanel yearPanel = new PillarPanel("年柱");
    private final PillarPanel monthPanel = new PillarPanel("月柱");
    private final PillarPanel dayPanel = new PillarPanel("日柱");
    private final PillarPanel hourPanel = new PillarPanel("時柱");

    private final JLabel summaryLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel noteLabel = new JLabel(" ", SwingConstants.CENTER);

    public BaziFrame() {
        super("子平八字排盤");

        int nowYear = Year.now().getValue();
        yearSpinner = new JSpinner(new SpinnerNumberModel(nowYear, 1900, 2100, 1));
        monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        for (JSpinner sp : new JSpinner[]{yearSpinner, monthSpinner, daySpinner, hourSpinner, minuteSpinner}) {
            sp.setEditor(new JSpinner.NumberEditor(sp, "#"));
            sp.getEditor().getComponent(0).setBackground(DarkTheme.BG_CARD);
            sp.getEditor().getComponent(0).setForeground(DarkTheme.FG_PRIMARY);
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildContent());
        setMinimumSize(new Dimension(760, 520));
        pack();
        setLocationRelativeTo(null);

        calculate();
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(DarkTheme.BG_APP);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(buildInputPanel(), BorderLayout.NORTH);
        center.add(buildOutputPanel(), BorderLayout.CENTER);
        center.add(buildSummaryPanel(), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("子平八字排盤");
        title.setForeground(DarkTheme.ACCENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("輸入出生年、月、日、時，推算年柱、月柱、日柱、時柱");
        subtitle.setForeground(DarkTheme.FG_SECONDARY);
        subtitle.setFont(subtitle.getFont().deriveFont(13f));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        DarkTheme.styleCard(panel);

        panel.add(labeledSpinner("年", yearSpinner));
        panel.add(labeledSpinner("月", monthSpinner));
        panel.add(labeledSpinner("日", daySpinner));
        panel.add(labeledSpinner("時", hourSpinner));
        panel.add(labeledSpinner("分", minuteSpinner));

        JButton calcButton = new JButton("排盤");
        DarkTheme.styleAccentButton(calcButton);
        calcButton.addActionListener(e -> calculate());
        panel.add(calcButton);

        return panel;
    }

    private JComponent labeledSpinner(String label, JSpinner spinner) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setForeground(DarkTheme.FG_SECONDARY);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);

        spinner.setPreferredSize(new Dimension(70, 30));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        spinner.setMaximumSize(new Dimension(80, 30));

        wrap.add(l);
        wrap.add(Box.createVerticalStrut(4));
        wrap.add(spinner);
        return wrap;
    }

    private JComponent buildOutputPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);
        panel.add(yearPanel);
        panel.add(monthPanel);
        panel.add(dayPanel);
        panel.add(hourPanel);
        return panel;
    }

    private JComponent buildSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        DarkTheme.styleCard(panel);

        summaryLabel.setForeground(DarkTheme.FG_PRIMARY);
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(14f));
        summaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        noteLabel.setForeground(DarkTheme.FG_SECONDARY);
        noteLabel.setFont(noteLabel.getFont().deriveFont(12f));
        noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(summaryLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(noteLabel);
        return panel;
    }

    private void calculate() {
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
            return;
        }

        BaziResult result = calculator.calculate(year, month, day, hour, minute);

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
}
