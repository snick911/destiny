package com.destiny.bazi.ui;

import com.destiny.bazi.core.FiveElementMass;

import javax.swing.*;
import java.awt.*;

/**
 * 五行質量的水平長條圖：每列顯示一個五行的字元、依比例填色的長條，
 * 以及數值與佔比百分比。
 */
public final class FiveElementBarChart extends JPanel {

    private static final String[] ELEMENT_ORDER = {"木", "火", "土", "金", "水"};

    private final double[] values = new double[ELEMENT_ORDER.length];
    private double total = 0;
    private float baseFontSize = 13f;

    public FiveElementBarChart() {
        setOpaque(false);
    }

    public void setMass(FiveElementMass mass) {
        double sum = 0;
        for (int i = 0; i < ELEMENT_ORDER.length; i++) {
            values[i] = mass.get(ELEMENT_ORDER[i]);
            sum += values[i];
        }
        total = sum;
        revalidate();
        repaint();
    }

    public void setFontScale(double scale) {
        baseFontSize = (float) (13 * scale);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int rowHeight = rowHeight();
        return new Dimension(360, rowHeight * ELEMENT_ORDER.length + 4);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private int rowHeight() {
        return (int) Math.ceil(baseFontSize * 1.9);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font labelFont = getFont().deriveFont(Font.BOLD, baseFontSize);
        Font valueFont = getFont().deriveFont(baseFontSize * 0.82f);
        FontMetrics labelMetrics = g2.getFontMetrics(labelFont);
        FontMetrics valueMetrics = g2.getFontMetrics(valueFont);

        int labelWidth = labelMetrics.stringWidth("金") + 6;
        int valueWidth = valueMetrics.stringWidth("00.00 (100%)") + 6;
        int barHeight = Math.max(10, (int) (baseFontSize * 0.75));
        int rowHeight = rowHeight();

        int barX = labelWidth + 6;
        int barWidth = Math.max(20, getWidth() - barX - valueWidth - 6);
        double denominator = total > 0 ? total : 1;

        for (int i = 0; i < ELEMENT_ORDER.length; i++) {
            String element = ELEMENT_ORDER[i];
            Color color = DarkTheme.elementColor(element);
            int rowTop = i * rowHeight;
            int barY = rowTop + (rowHeight - barHeight) / 2;
            int textBaselineY = rowTop + (rowHeight + labelMetrics.getAscent() - labelMetrics.getDescent()) / 2;

            g2.setFont(labelFont);
            g2.setColor(color);
            g2.drawString(element, 0, textBaselineY);

            g2.setColor(DarkTheme.BORDER);
            g2.fillRoundRect(barX, barY, barWidth, barHeight, barHeight, barHeight);

            double ratio = values[i] / denominator;
            int filled = (int) Math.round(barWidth * Math.max(0, Math.min(1, ratio)));
            if (filled > 0) {
                g2.setColor(color);
                g2.fillRoundRect(barX, barY, Math.max(filled, barHeight), barHeight, barHeight, barHeight);
            }

            String valueText = String.format("%.2f (%.0f%%)", values[i], ratio * 100);
            g2.setFont(valueFont);
            g2.setColor(DarkTheme.FG_SECONDARY);
            g2.drawString(valueText, barX + barWidth + 6, textBaselineY);
        }

        g2.dispose();
    }
}
