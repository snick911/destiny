package com.destiny.bazi.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * 流年運勢曲線（單一數列、單一縱軸）：橫軸為所選大限的十個流年，縱軸為指數。
 * 黑底配亮色折線；所選流年以較大的點與數值標示，今年以外圈標示；
 * 滑鼠停在點上顯示年份與數值，點選即切換到該流年。
 */
public final class LuckCurveChart extends JComponent {

    private static final int BASE_HEIGHT = 270;
    private static final Color GRID = new Color(0x44, 0x44, 0x44);
    private static final Color AXIS_TEXT = new Color(0xD0, 0xD0, 0xD0);
    private static final Color TEXT = Color.WHITE;

    private final String title;
    private final Color lineColor;
    private final int minFloor;
    private final int maxFloor;
    private final int[] fixedTicks;
    private final IntFunction<String> describer;

    private int[] years = new int[0];
    private int[] values = new int[0];
    private int selectedYear = Integer.MIN_VALUE;
    private int currentYear = Integer.MIN_VALUE;
    private String subtitle = "";
    private double fontScale = 1.0;
    private int hoverIndex = -1;
    private IntConsumer yearClickListener;

    /**
     * @param title      圖名（即數列名稱，單一數列不另設圖例）
     * @param lineColor  折線顏色
     * @param minFloor   縱軸下限至少涵蓋的值（資料更低時自動延伸）
     * @param maxFloor   縱軸上限至少涵蓋的值
     * @param fixedTicks 固定的縱軸刻度；null 表示依範圍自動取整
     * @param describer  滑鼠提示中附加的說明（例如好運指數評語），可為 null
     */
    public LuckCurveChart(String title, Color lineColor, int minFloor, int maxFloor, int[] fixedTicks,
                          IntFunction<String> describer) {
        this.title = title;
        this.lineColor = lineColor;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.fixedTicks = fixedTicks;
        this.describer = describer;
        setOpaque(true);
        setBackground(ZiWeiChartPanel.CHART_BG);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = indexAt(e.getPoint());
                if (idx != hoverIndex) {
                    hoverIndex = idx;
                    setCursor(Cursor.getPredefinedCursor(idx >= 0 && yearClickListener != null
                            ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = indexAt(e.getPoint());
                if (idx >= 0 && yearClickListener != null) {
                    yearClickListener.accept(years[idx]);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /** 設定資料：各流年與其指數、所選流年、今年，以及圖名下方的說明（例如所選大限）。 */
    public void setData(int[] years, int[] values, int selectedYear, int currentYear, String subtitle) {
        this.years = years.clone();
        this.values = values.clone();
        this.selectedYear = selectedYear;
        this.currentYear = currentYear;
        this.subtitle = subtitle == null ? "" : subtitle;
        hoverIndex = -1;
        repaint();
    }

    public void setYearClickListener(IntConsumer listener) {
        this.yearClickListener = listener;
    }

    public void setFontScale(double scale) {
        this.fontScale = scale;
        revalidate();
        repaint();
    }

    /** 測試與匯出用：目前的數值。 */
    int[] values() {
        return values.clone();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(scaled(400), scaled(BASE_HEIGHT));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(scaled(220), scaled(BASE_HEIGHT));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int idx = indexAt(e.getPoint());
        if (idx < 0) {
            return null;
        }
        String desc = describer == null ? "" : "<br>" + describer.apply(values[idx]);
        return "<html>" + years[idx] + " 年：" + title + " <b>" + values[idx] + "</b>" + desc + "</html>";
    }

    private int scaled(double v) {
        return (int) Math.round(v * fontScale);
    }

    // ------------------------------------------------------------------ 版面

    private record Plot(int left, int top, int right, int bottom, int min, int max) {
        int y(double v) {
            return (int) Math.round(bottom - (v - min) * (bottom - top) / (double) (max - min));
        }
    }

    private Plot plot() {
        Font tickFont = DarkTheme.BASE_FONT.deriveFont((float) (14 * fontScale));
        FontMetrics fm = getFontMetrics(tickFont);
        int min = minFloor;
        int max = maxFloor;
        for (int v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (fixedTicks == null) {
            int step = niceStep(max - min);
            min = Math.floorDiv(min, step) * step;
            max = -Math.floorDiv(-max, step) * step;
        }
        int labelWidth = Math.max(fm.stringWidth(String.valueOf(min)), fm.stringWidth(String.valueOf(max)));
        int top = scaled(14) + titleBlockHeight();
        return new Plot(scaled(12) + labelWidth + scaled(8), top, getWidth() - scaled(16),
                getHeight() - scaled(12) - fm.getHeight(), min, max);
    }

    private int titleBlockHeight() {
        FontMetrics titleFm = getFontMetrics(DarkTheme.BASE_FONT.deriveFont(Font.BOLD, (float) (16 * fontScale)));
        FontMetrics subFm = getFontMetrics(DarkTheme.BASE_FONT.deriveFont((float) (14 * fontScale)));
        return titleFm.getHeight() + subFm.getHeight() + scaled(8);
    }

    private static int niceStep(int range) {
        int[] steps = {10, 20, 25, 50, 100, 150, 200, 250, 500, 1000};
        for (int s : steps) {
            if (range / s <= 6) {
                return s;
            }
        }
        return 2000;
    }

    private int xAt(Plot p, int index) {
        if (years.length <= 1) {
            return (p.left() + p.right()) / 2;
        }
        int inset = scaled(14);
        return p.left() + inset + (p.right() - p.left() - inset * 2) * index / (years.length - 1);
    }

    private int indexAt(Point point) {
        if (years.length == 0) {
            return -1;
        }
        Plot p = plot();
        int best = -1;
        int bestDist = scaled(14);
        for (int i = 0; i < years.length; i++) {
            int dx = Math.abs(point.x - xAt(p, i));
            if (dx <= bestDist && point.y >= p.top() - scaled(10) && point.y <= p.bottom() + scaled(24)) {
                best = i;
                bestDist = dx;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ 繪製

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 圖名與說明
        Font titleFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, (float) (16 * fontScale));
        Font subFont = DarkTheme.BASE_FONT.deriveFont((float) (14 * fontScale));
        Font tickFont = DarkTheme.BASE_FONT.deriveFont((float) (14 * fontScale));
        Font valueFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, (float) (14 * fontScale));
        int x0 = scaled(12);
        int y = scaled(10);
        g2.setFont(titleFont);
        g2.setColor(TEXT);
        y += g2.getFontMetrics().getAscent();
        g2.drawString(title, x0, y);
        g2.setFont(subFont);
        g2.setColor(AXIS_TEXT);
        y += g2.getFontMetrics().getHeight();
        g2.drawString(subtitle, x0, y);
        if (years.length == 0) {
            g2.dispose();
            return;
        }

        Plot p = plot();
        g2.setFont(tickFont);
        FontMetrics tfm = g2.getFontMetrics();

        // 橫向格線與縱軸刻度（0 線較明顯）
        for (int tick : ticks(p)) {
            int ty = p.y(tick);
            g2.setColor(tick == 0 ? new Color(0x66, 0x66, 0x66) : GRID);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(p.left(), ty, p.right(), ty);
            g2.setColor(AXIS_TEXT);
            String label = String.valueOf(tick);
            g2.drawString(label, p.left() - scaled(6) - tfm.stringWidth(label), ty + tfm.getAscent() / 2 - 1);
        }

        // 橫軸年份（年數多時隔年標示，所選年份一定標示）
        int every = years.length > 10 ? 2 : 1;
        for (int i = 0; i < years.length; i++) {
            boolean selected = years[i] == selectedYear;
            if (i % every != 0 && !selected) {
                continue;
            }
            String label = String.valueOf(years[i]);
            g2.setColor(selected ? TEXT : AXIS_TEXT);
            g2.drawString(label, xAt(p, i) - tfm.stringWidth(label) / 2, p.bottom() + scaled(4) + tfm.getAscent());
        }

        // 折線（2px）
        Path2D path = new Path2D.Double();
        for (int i = 0; i < years.length; i++) {
            int px = xAt(p, i);
            int py = p.y(values[i]);
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke((float) Math.max(2, 2 * fontScale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        // 資料點：一般 8px；所選流年 12px 並標示數值；今年加外圈；滑鼠停留處加亮
        g2.setFont(valueFont);
        FontMetrics vfm = g2.getFontMetrics();
        for (int i = 0; i < years.length; i++) {
            int px = xAt(p, i);
            int py = p.y(values[i]);
            boolean selected = years[i] == selectedYear;
            int size = scaled(selected || i == hoverIndex ? 12 : 8);
            g2.setColor(getBackground());
            g2.fillOval(px - size / 2 - 2, py - size / 2 - 2, size + 4, size + 4);
            g2.setColor(lineColor);
            g2.fillOval(px - size / 2, py - size / 2, size, size);
            if (years[i] == currentYear) {
                int ring = size + scaled(8);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(TEXT);
                g2.drawOval(px - ring / 2, py - ring / 2, ring, ring);
            }
            if (selected || i == hoverIndex) {
                String label = String.valueOf(values[i]);
                int lw = vfm.stringWidth(label);
                int ly = py - size / 2 - scaled(6);
                if (ly - vfm.getAscent() < p.top() - scaled(10)) {
                    ly = py + size / 2 + scaled(4) + vfm.getAscent();
                }
                int lx = Math.max(p.left(), Math.min(px - lw / 2, p.right() - lw));
                g2.setColor(TEXT);
                g2.drawString(label, lx, ly);
            }
        }
        g2.dispose();
    }

    private List<Integer> ticks(Plot p) {
        List<Integer> ticks = new ArrayList<>();
        if (fixedTicks != null) {
            for (int t : fixedTicks) {
                if (t >= p.min() && t <= p.max()) {
                    ticks.add(t);
                }
            }
            return ticks;
        }
        int step = niceStep(p.max() - p.min());
        for (int t = p.min(); t <= p.max(); t += step) {
            ticks.add(t);
        }
        return ticks;
    }
}
