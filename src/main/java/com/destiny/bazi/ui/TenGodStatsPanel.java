package com.destiny.bazi.ui;

import com.destiny.bazi.core.TenGod;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 十神統計：以 5 列 2 欄的網格顯示十神各自的出現次數。
 */
public final class TenGodStatsPanel extends JPanel {

    private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();
    private float baseFontSize = 12f;

    public TenGodStatsPanel() {
        super(new GridLayout(5, 2, 12, 2));
        setOpaque(false);
        for (String god : TenGod.ALL) {
            JLabel label = new JLabel(god + "：0", SwingConstants.LEFT);
            label.setForeground(DarkTheme.FG_SECONDARY);
            label.setFont(label.getFont().deriveFont(baseFontSize));
            valueLabels.put(god, label);
            add(label);
        }
    }

    public void setCounts(Map<String, Integer> counts) {
        for (String god : TenGod.ALL) {
            int count = counts.getOrDefault(god, 0);
            JLabel label = valueLabels.get(god);
            label.setText(god + "：" + count);
            label.setForeground(count > 0 ? DarkTheme.ACCENT : DarkTheme.FG_SECONDARY);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void setFontScale(double scale) {
        baseFontSize = (float) (12 * scale);
        for (JLabel label : valueLabels.values()) {
            label.setFont(label.getFont().deriveFont(baseFontSize));
        }
        revalidate();
        repaint();
    }
}
