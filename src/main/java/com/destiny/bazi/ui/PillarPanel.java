package com.destiny.bazi.ui;

import com.destiny.bazi.core.Pillar;

import javax.swing.*;
import java.awt.*;

/**
 * 顯示單一柱（年/月/日/時）的卡片：標題、天干、地支，並依五行上色。
 */
public final class PillarPanel extends JPanel {

    private final JLabel titleLabel;
    private final JLabel stemLabel;
    private final JLabel branchLabel;
    private final JLabel stemElementLabel;
    private final JLabel branchElementLabel;

    public PillarPanel(String title) {
        super(new GridBagLayout());
        DarkTheme.styleCard(this);

        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(DarkTheme.FG_SECONDARY);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        stemLabel = bigCharLabel();
        branchLabel = bigCharLabel();

        stemElementLabel = new JLabel(" ", SwingConstants.CENTER);
        stemElementLabel.setFont(stemElementLabel.getFont().deriveFont(12f));
        branchElementLabel = new JLabel(" ", SwingConstants.CENTER);
        branchElementLabel.setFont(branchElementLabel.getFont().deriveFont(12f));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(2, 4, 2, 4);

        gc.gridy = 0;
        add(titleLabel, gc);
        gc.gridy = 1;
        gc.insets = new Insets(10, 4, 2, 4);
        add(stemLabel, gc);
        gc.gridy = 2;
        gc.insets = new Insets(0, 4, 8, 4);
        add(stemElementLabel, gc);
        gc.gridy = 3;
        gc.insets = new Insets(2, 4, 2, 4);
        add(branchLabel, gc);
        gc.gridy = 4;
        gc.insets = new Insets(0, 4, 4, 4);
        add(branchElementLabel, gc);
    }

    private JLabel bigCharLabel() {
        JLabel label = new JLabel(" ", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 46f));
        return label;
    }

    public void setPillar(Pillar pillar) {
        stemLabel.setText(pillar.getStem());
        stemLabel.setForeground(DarkTheme.elementColor(pillar.getStemElement()));
        stemElementLabel.setText("(" + pillar.getStemElement() + ")");
        stemElementLabel.setForeground(DarkTheme.FG_SECONDARY);

        branchLabel.setText(pillar.getBranch());
        branchLabel.setForeground(DarkTheme.elementColor(pillar.getBranchElement()));
        branchElementLabel.setText("(" + pillar.getBranchElement() + ")");
        branchElementLabel.setForeground(DarkTheme.FG_SECONDARY);
    }

    public void clear() {
        stemLabel.setText(" ");
        branchLabel.setText(" ");
        stemElementLabel.setText(" ");
        branchElementLabel.setText(" ");
    }
}
