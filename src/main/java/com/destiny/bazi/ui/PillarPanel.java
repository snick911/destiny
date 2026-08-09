package com.destiny.bazi.ui;

import com.destiny.bazi.core.HiddenStemGod;
import com.destiny.bazi.core.Pillar;
import com.destiny.bazi.core.PillarTenGods;

import javax.swing.*;
import java.awt.*;

/**
 * 顯示單一柱（年/月/日/時）的卡片：標題、天干十神、天干、地支，並依五行上色，
 * 底部另列出地支藏干及其十神。
 */
public final class PillarPanel extends JPanel {

    private static final float TITLE_SIZE = 16f;
    private static final float TEN_GOD_SIZE = 13f;
    private static final float BIG_CHAR_SIZE = 46f;
    private static final float ELEMENT_SIZE = 12f;
    private static final float HIDDEN_SIZE = 11f;

    private final JLabel titleLabel;
    private final JLabel stemTenGodLabel;
    private final JLabel stemLabel;
    private final JLabel branchLabel;
    private final JLabel stemElementLabel;
    private final JLabel branchElementLabel;
    private final JLabel hiddenGodsLabel;

    public PillarPanel(String title) {
        super(new GridBagLayout());
        DarkTheme.styleCard(this);

        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(DarkTheme.FG_SECONDARY);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_SIZE));

        stemTenGodLabel = new JLabel(" ", SwingConstants.CENTER);
        stemTenGodLabel.setFont(stemTenGodLabel.getFont().deriveFont(Font.BOLD, TEN_GOD_SIZE));
        stemTenGodLabel.setForeground(DarkTheme.ACCENT);

        stemLabel = bigCharLabel();
        branchLabel = bigCharLabel();

        stemElementLabel = new JLabel(" ", SwingConstants.CENTER);
        stemElementLabel.setFont(stemElementLabel.getFont().deriveFont(ELEMENT_SIZE));
        branchElementLabel = new JLabel(" ", SwingConstants.CENTER);
        branchElementLabel.setFont(branchElementLabel.getFont().deriveFont(ELEMENT_SIZE));

        hiddenGodsLabel = new JLabel(" ", SwingConstants.CENTER);
        hiddenGodsLabel.setFont(hiddenGodsLabel.getFont().deriveFont(HIDDEN_SIZE));
        hiddenGodsLabel.setForeground(DarkTheme.FG_SECONDARY);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(2, 4, 2, 4);

        gc.gridy = 0;
        add(titleLabel, gc);
        gc.gridy = 1;
        gc.insets = new Insets(8, 4, 0, 4);
        add(stemTenGodLabel, gc);
        gc.gridy = 2;
        gc.insets = new Insets(2, 4, 2, 4);
        add(stemLabel, gc);
        gc.gridy = 3;
        gc.insets = new Insets(0, 4, 8, 4);
        add(stemElementLabel, gc);
        gc.gridy = 4;
        gc.insets = new Insets(2, 4, 2, 4);
        add(branchLabel, gc);
        gc.gridy = 5;
        gc.insets = new Insets(0, 4, 6, 4);
        add(branchElementLabel, gc);
        gc.gridy = 6;
        gc.insets = new Insets(0, 4, 4, 4);
        add(hiddenGodsLabel, gc);
    }

    private JLabel bigCharLabel() {
        JLabel label = new JLabel(" ", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, BIG_CHAR_SIZE));
        return label;
    }

    /** 依比例縮放本卡片內所有文字的字型大小。scale=1.0 為預設大小。 */
    public void setFontScale(double scale) {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, (float) (TITLE_SIZE * scale)));
        stemTenGodLabel.setFont(stemTenGodLabel.getFont().deriveFont(Font.BOLD, (float) (TEN_GOD_SIZE * scale)));
        stemLabel.setFont(stemLabel.getFont().deriveFont(Font.BOLD, (float) (BIG_CHAR_SIZE * scale)));
        branchLabel.setFont(branchLabel.getFont().deriveFont(Font.BOLD, (float) (BIG_CHAR_SIZE * scale)));
        stemElementLabel.setFont(stemElementLabel.getFont().deriveFont((float) (ELEMENT_SIZE * scale)));
        branchElementLabel.setFont(branchElementLabel.getFont().deriveFont((float) (ELEMENT_SIZE * scale)));
        hiddenGodsLabel.setFont(hiddenGodsLabel.getFont().deriveFont((float) (HIDDEN_SIZE * scale)));
    }

    public void setPillar(Pillar pillar, PillarTenGods tenGods) {
        stemLabel.setText(pillar.getStem());
        stemLabel.setForeground(DarkTheme.elementColor(pillar.getStemElement()));
        stemElementLabel.setText("(" + pillar.getStemElement() + ")");
        stemElementLabel.setForeground(DarkTheme.FG_SECONDARY);

        branchLabel.setText(pillar.getBranch());
        branchLabel.setForeground(DarkTheme.elementColor(pillar.getBranchElement()));
        branchElementLabel.setText("(" + pillar.getBranchElement() + ")");
        branchElementLabel.setForeground(DarkTheme.FG_SECONDARY);

        stemTenGodLabel.setText(tenGods.getStemTenGod());

        StringBuilder html = new StringBuilder("<html><center>");
        for (HiddenStemGod hg : tenGods.getBranchHiddenGods()) {
            if (html.length() > "<html><center>".length()) {
                html.append("<br>");
            }
            if (hg.isPrimary()) {
                html.append("<b>").append(hg.getStem()).append(' ').append(hg.getTenGod()).append("</b>");
            } else {
                html.append(hg.getStem()).append(' ').append(hg.getTenGod());
            }
        }
        html.append("</center></html>");
        hiddenGodsLabel.setText(html.toString());
    }

    public void clear() {
        stemLabel.setText(" ");
        branchLabel.setText(" ");
        stemElementLabel.setText(" ");
        branchElementLabel.setText(" ");
        stemTenGodLabel.setText(" ");
        hiddenGodsLabel.setText(" ");
    }
}
