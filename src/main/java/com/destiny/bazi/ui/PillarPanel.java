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

    private final float titleSize;
    private final float tenGodSize;
    private final float bigCharSize;
    private final float elementSize;
    private final float hiddenSize;

    private final JLabel titleLabel;
    private final JLabel stemTenGodLabel;
    private final JLabel stemLabel;
    private final JLabel branchLabel;
    private final JLabel stemElementLabel;
    private final JLabel branchElementLabel;
    private final JLabel hiddenGodsLabel;

    public PillarPanel(String title) {
        this(title, false);
    }

    /** compact=true 用於顯示較多欄位的大運列，採用較小的預設字型。 */
    public PillarPanel(String title, boolean compact) {
        super(new GridBagLayout());
        DarkTheme.styleCard(this);

        titleSize = compact ? 12f : 16f;
        tenGodSize = compact ? 11f : 13f;
        bigCharSize = compact ? 28f : 46f;
        elementSize = compact ? 10f : 12f;
        hiddenSize = compact ? 10f : 11f;

        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(DarkTheme.FG_SECONDARY);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleSize));

        stemTenGodLabel = new JLabel(" ", SwingConstants.CENTER);
        stemTenGodLabel.setFont(stemTenGodLabel.getFont().deriveFont(Font.BOLD, tenGodSize));
        stemTenGodLabel.setForeground(DarkTheme.ACCENT);

        stemLabel = bigCharLabel();
        branchLabel = bigCharLabel();

        stemElementLabel = new JLabel(" ", SwingConstants.CENTER);
        stemElementLabel.setFont(stemElementLabel.getFont().deriveFont(elementSize));
        branchElementLabel = new JLabel(" ", SwingConstants.CENTER);
        branchElementLabel.setFont(branchElementLabel.getFont().deriveFont(elementSize));

        hiddenGodsLabel = new JLabel(" ", SwingConstants.CENTER);
        hiddenGodsLabel.setFont(hiddenGodsLabel.getFont().deriveFont(hiddenSize));
        hiddenGodsLabel.setForeground(DarkTheme.FG_SECONDARY);

        // 所有列一律以 NORTH 對齊、weighty=0，僅底部藏干列吸收多餘高度，
        // 確保各柱的天干／地支字元無論藏干行數多寡都能水平對齊。
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
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
        gc.weighty = 1.0;
        add(hiddenGodsLabel, gc);
    }

    private JLabel bigCharLabel() {
        JLabel label = new JLabel(" ", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, bigCharSize));
        return label;
    }

    /** 依比例縮放本卡片內所有文字的字型大小。scale=1.0 為預設大小。 */
    public void setFontScale(double scale) {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, (float) (titleSize * scale)));
        stemTenGodLabel.setFont(stemTenGodLabel.getFont().deriveFont(Font.BOLD, (float) (tenGodSize * scale)));
        stemLabel.setFont(stemLabel.getFont().deriveFont(Font.BOLD, (float) (bigCharSize * scale)));
        branchLabel.setFont(branchLabel.getFont().deriveFont(Font.BOLD, (float) (bigCharSize * scale)));
        stemElementLabel.setFont(stemElementLabel.getFont().deriveFont((float) (elementSize * scale)));
        branchElementLabel.setFont(branchElementLabel.getFont().deriveFont((float) (elementSize * scale)));
        hiddenGodsLabel.setFont(hiddenGodsLabel.getFont().deriveFont((float) (hiddenSize * scale)));
    }

    /** 更新標題文字（大運卡片需依起訖歲數動態更新標題）。 */
    public void setTitle(String title) {
        titleLabel.setText(title);
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
