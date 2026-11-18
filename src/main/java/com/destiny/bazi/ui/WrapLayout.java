package com.destiny.bazi.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * 寬度不足時會換行、且所需高度包含換行後各列的 {@link FlowLayout}。
 *
 * <p>FlowLayout 排版時雖會把放不下的元件換到下一列，回報的偏好高度卻永遠只有一列，
 * 外層因此只配置一列的高度，換到下一列的元件就被裁切而看不到（例如放大字體後的窄視窗）。
 * 這裡依容器目前的寬度計算換行後的高度；偏好寬度仍是排成一列所需的寬度，
 * 因此寬度足夠時（一般字體、或依內容所需大小繪製的匯出圖片）與原本的 FlowLayout 完全相同。</p>
 *
 * <p>容器寬度在同一次排版中才改變時（例如視窗由寬縮窄），外層先前取得的是舊寬度下的高度；
 * 排版後若發現所需高度不同，便在事件佇列中要求重新排版一次（與 JTextArea 自動換行的作法相同）。
 * 這需要記錄所屬容器上次回報的高度，因此每個容器使用各自的 WrapLayout。</p>
 */
final class WrapLayout extends FlowLayout {

    /** 上次回報給外層的偏好高度；-1 表示尚未回報。 */
    private int reportedHeight = -1;
    private boolean relayoutPending;

    WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension size = super.preferredLayoutSize(target);
            size.height = wrappedHeight(target);
            reportedHeight = size.height;
            return size;
        }
    }

    /** 最窄可縮到只容納最寬的單一元件；高度與偏好高度相同（元件一律以偏好尺寸擺放）。 */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            int widest = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    widest = Math.max(widest, m.getPreferredSize().width);
                }
            }
            Insets insets = target.getInsets();
            return new Dimension(widest + insets.left + insets.right + getHgap() * 2, wrappedHeight(target));
        }
    }

    @Override
    public void layoutContainer(Container target) {
        super.layoutContainer(target);
        synchronized (target.getTreeLock()) {
            if (relayoutPending || !(target instanceof JComponent component)) {
                return;
            }
            if (wrappedHeight(target) != reportedHeight) {
                relayoutPending = true;
                SwingUtilities.invokeLater(() -> {
                    relayoutPending = false;
                    component.revalidate();
                });
            }
        }
    }

    /**
     * 依容器目前寬度換行後所需的高度。換行規則與 {@link FlowLayout#layoutContainer} 相同：
     * 該列已有元件，且加上此元件後超出可用寬度（不計元件之間的間距）時才換行。
     * 容器尚未配置寬度時視為不限寬度，即排成一列。
     */
    private int wrappedHeight(Container target) {
        Insets insets = target.getInsets();
        int hgap = getHgap();
        int vgap = getVgap();
        int maxWidth = target.getWidth() > 0
                ? target.getWidth() - (insets.left + insets.right + hgap * 2)
                : Integer.MAX_VALUE;

        int x = 0;
        int rowHeight = 0;
        int height = 0;
        for (int i = 0; i < target.getComponentCount(); i++) {
            Component m = target.getComponent(i);
            if (!m.isVisible()) {
                continue;
            }
            Dimension d = m.getPreferredSize();
            if (x == 0 || x + d.width <= maxWidth) {
                if (x > 0) {
                    x += hgap;
                }
                x += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            } else {
                height += rowHeight + vgap;
                x = d.width;
                rowHeight = d.height;
            }
        }
        return height + rowHeight + insets.top + insets.bottom + vgap * 2;
    }
}
