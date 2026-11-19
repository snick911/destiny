package com.destiny.bazi.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 護眼暗色主題：以低對比、去飽和的深色調為主，搭配依五行區分的柔和點綴色，
 * 避免長時間閱讀造成的視覺疲勞；並優先選用可正確顯示繁體中文的字型。
 */
public final class DarkTheme {

    // 依常見作業系統優先順序列出的繁體中文字型；找不到時退回系統無襯線字型。
    private static final String[] TC_FONT_CANDIDATES = {
            "Microsoft JhengHei UI", "Microsoft JhengHei", "PMingLiU",
            "PingFang TC", "Heiti TC",
            "Noto Sans TC", "Noto Sans CJK TC", "Noto Sans CJK TC Regular",
            "AR PL UMing TW", "AR PL UKai TW", "WenQuanYi Zen Hei"
    };

    /** 全域基準字型（優先繁體中文字型，找不到則退回系統無襯線字型）。 */
    public static final Font BASE_FONT = resolveTraditionalChineseFont();

    private static Font resolveTraditionalChineseFont() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : TC_FONT_CANDIDATES) {
            if (available.contains(candidate)) {
                return new Font(candidate, Font.PLAIN, 14);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    }

    public static final Color BG_APP = new Color(0x1D, 0x21, 0x27);
    public static final Color BG_PANEL = new Color(0x25, 0x2A, 0x31);
    public static final Color BG_CARD = new Color(0x2C, 0x32, 0x3A);
    public static final Color BG_CARD_HIGHLIGHT = new Color(0x33, 0x3A, 0x44);
    public static final Color BORDER = new Color(0x3B, 0x42, 0x4B);
    public static final Color SCROLL_THUMB = new Color(0x4C, 0x55, 0x61);

    // 文字與五行皆用明亮色（底色維持深色）：主要文字接近純白、五行用亮綠／亮紅／亮黃／白／亮藍
    public static final Color FG_PRIMARY = new Color(0xF4, 0xF6, 0xFA);
    public static final Color FG_SECONDARY = new Color(0xCC, 0xD4, 0xDE);
    public static final Color ACCENT = new Color(0x6E, 0xD8, 0xFF);
    public static final Color ACCENT_DIM = new Color(0x3F, 0x7F, 0xD0);

    public static final Color ELEMENT_WOOD = new Color(0x66, 0xFF, 0x77);
    public static final Color ELEMENT_FIRE = new Color(0xFF, 0x55, 0x55);
    public static final Color ELEMENT_EARTH = new Color(0xFF, 0xDD, 0x55);
    public static final Color ELEMENT_METAL = new Color(0xFF, 0xFF, 0xFF);
    public static final Color ELEMENT_WATER = new Color(0x5C, 0xB8, 0xFF);

    private DarkTheme() {
    }

    public static Color elementColor(String element) {
        return switch (element) {
            case "木" -> ELEMENT_WOOD;
            case "火" -> ELEMENT_FIRE;
            case "土" -> ELEMENT_EARTH;
            case "金" -> ELEMENT_METAL;
            case "水" -> ELEMENT_WATER;
            default -> FG_PRIMARY;
        };
    }

    /**
     * 套用全域深色 UIManager 預設值，讓 JSpinner、JButton 等原生元件也符合暗色主題，
     * 並套用繁體中文字型（Nimbus 透過 defaultFont 一次套用；其餘元件逐一指定作為備援）。
     */
    public static void applyGlobal() {
        UIManager.put("defaultFont", BASE_FONT);
        String[] fontKeys = {
                "Label.font", "Button.font", "TextField.font", "Spinner.font", "ComboBox.font",
                "Menu.font", "MenuItem.font", "MenuBar.font", "OptionPane.font", "OptionPane.messageFont",
                "ToolTip.font", "TitledBorder.font", "FileChooser.font", "PopupMenu.font", "Panel.font"
        };
        for (String key : fontKeys) {
            UIManager.put(key, BASE_FONT);
        }

        UIManager.put("control", BG_PANEL);
        UIManager.put("info", BG_CARD);
        UIManager.put("nimbusBase", BG_APP);
        UIManager.put("nimbusBlueGrey", BG_PANEL);
        UIManager.put("nimbusLightBackground", BG_CARD);
        UIManager.put("text", FG_PRIMARY);
        UIManager.put("nimbusSelectionBackground", ACCENT_DIM);

        UIManager.put("Panel.background", BG_APP);
        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.messageForeground", FG_PRIMARY);
        UIManager.put("Label.foreground", FG_PRIMARY);
        UIManager.put("Spinner.background", BG_CARD);
        UIManager.put("Spinner.foreground", FG_PRIMARY);
        UIManager.put("ComboBox.background", BG_CARD);
        UIManager.put("ComboBox.foreground", FG_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_DIM);
        UIManager.put("ComboBox.selectionForeground", FG_PRIMARY);
        UIManager.put("ComboBox.buttonBackground", BG_CARD);
        UIManager.put("TextField.background", BG_CARD);
        UIManager.put("TextField.foreground", FG_PRIMARY);
        UIManager.put("TextField.caretForeground", FG_PRIMARY);
        UIManager.put("Button.background", BG_CARD);
        UIManager.put("Button.foreground", FG_PRIMARY);
        UIManager.put("Button.select", ACCENT_DIM);
        UIManager.put("ToolTip.background", BG_CARD);
        UIManager.put("ToolTip.foreground", FG_PRIMARY);
        UIManager.put("Separator.foreground", BORDER);

        UIManager.put("MenuBar.background", BG_PANEL);
        UIManager.put("MenuBar.foreground", FG_PRIMARY);
        UIManager.put("Menu.background", BG_PANEL);
        UIManager.put("Menu.foreground", FG_PRIMARY);
        UIManager.put("Menu.selectionBackground", ACCENT_DIM);
        UIManager.put("Menu.selectionForeground", FG_PRIMARY);
        UIManager.put("MenuItem.background", BG_CARD);
        UIManager.put("MenuItem.foreground", FG_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", ACCENT_DIM);
        UIManager.put("MenuItem.selectionForeground", FG_PRIMARY);
        UIManager.put("MenuItem.acceleratorForeground", FG_SECONDARY);
        UIManager.put("PopupMenu.background", BG_CARD);
        UIManager.put("PopupMenu.foreground", FG_PRIMARY);
        UIManager.put("FileChooser.background", BG_PANEL);
        UIManager.put("FileChooser.foreground", FG_PRIMARY);
    }

    public static void styleMenuBar(JMenuBar menuBar) {
        menuBar.setBackground(BG_PANEL);
        menuBar.setForeground(FG_PRIMARY);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
    }

    public static void styleMenu(JMenu menu) {
        menu.setBackground(BG_PANEL);
        menu.setForeground(FG_PRIMARY);
    }

    public static void styleMenuItem(JMenuItem item) {
        item.setBackground(BG_CARD);
        item.setForeground(FG_PRIMARY);
    }

    /** 扁平強調按鈕（主要動作）：不使用 Nimbus 的漸層外觀，滑鼠移入／按下時改變底色。 */
    public static void styleAccentButton(JButton button) {
        styleFlatButton(button, ACCENT_DIM, null);
        button.setBorder(new EmptyBorder(10, 22, 10, 22));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
    }

    /** 扁平次要按鈕：卡片底色加細框線，滑鼠移入時轉亮。 */
    public static void styleSecondaryButton(JButton button) {
        styleFlatButton(button, BG_CARD_HIGHLIGHT, BORDER);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    /**
     * 扁平切換按鈕（用於「八字／紫微斗數」模式切換）：選取時以強調色填滿，
     * 未選取時為卡片底色加細框線。
     */
    public static void styleToggleButton(JToggleButton button) {
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel m = b.getModel();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = m.isSelected() ? ACCENT_DIM : BG_CARD_HIGHLIGHT;
                g2.setColor(m.isPressed() ? base.darker() : m.isRollover() && !m.isSelected() ? base.brighter() : base);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                g2.setColor(m.isSelected() ? ACCENT : BORDER);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
                g2.dispose();
                super.paint(g, c);
            }
        });
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setRolloverEnabled(true);
        button.setForeground(FG_PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(7, 22, 7, 22));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static void styleFlatButton(JButton button, Color base, Color outline) {
        Color hover = base.brighter();
        Color pressed = base.darker();
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel m = b.getModel();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(!b.isEnabled() ? BG_CARD : m.isPressed() ? pressed : m.isRollover() ? hover : base);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                if (outline != null) {
                    g2.setColor(outline);
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paint(g, c);
            }
        });
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setRolloverEnabled(true);
        button.setForeground(FG_PRIMARY);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** 暗色文字輸入框：細框線加內距，取得焦點時框線轉為強調色。 */
    public static void styleTextField(JTextField field) {
        field.setBackground(BG_CARD);
        field.setForeground(FG_PRIMARY);
        field.setCaretColor(FG_PRIMARY);
        field.setSelectionColor(ACCENT_DIM);
        field.setSelectedTextColor(FG_PRIMARY);
        field.setBorder(textFieldBorder(BORDER));
        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(textFieldBorder(ACCENT_DIM));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(textFieldBorder(BORDER));
            }
        });
    }

    private static javax.swing.border.Border textFieldBorder(Color color) {
        return BorderFactory.createCompoundBorder(new LineBorder(color, 1, true), new EmptyBorder(3, 8, 3, 8));
    }

    /** 暗色表格：交錯列底色、左右內距、選取列以強調色標示，表頭改為扁平樣式。 */
    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(FG_PRIMARY);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ACCENT_DIM);
        table.setSelectionForeground(FG_PRIMARY);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, false, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setBackground(isSelected ? ACCENT_DIM : row % 2 == 0 ? BG_CARD : BG_PANEL);
                setForeground(FG_PRIMARY);
                return this;
            }
        });
        table.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setFont(t.getFont().deriveFont(Font.BOLD));
                setOpaque(true);
                setBackground(BG_PANEL);
                setForeground(ACCENT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(6, 10, 6, 10)));
                return this;
            }
        });
        table.getTableHeader().setBackground(BG_PANEL);
        table.getTableHeader().setReorderingAllowed(false);
    }

    // ------------------------------------------------------------ 扁平化原生元件

    /** 將捲動區的捲軸換成扁平暗色樣式（無上下箭頭、圓角滑塊）。 */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUI(new FlatScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new FlatScrollBarUI());
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
    }

    /** 下拉選單改為扁平暗色：細框線、簡潔箭頭、取得焦點時框線轉為強調色。 */
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new ChevronButton(SwingConstants.SOUTH);
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                // 焦點改由框線表示，不以選取底色填滿目前值，避免一片藍色。
                super.paintCurrentValue(g, bounds, false);
            }
        });
        combo.setBackground(BG_CARD);
        combo.setForeground(FG_PRIMARY);
        combo.setRenderer(new PaddedComboRenderer());
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installFocusBorder(combo);
    }

    /** 數字欄改為扁平暗色：細框線、上下兩個簡潔箭頭按鈕。 */
    public static void styleSpinner(JSpinner spinner) {
        spinner.setUI(new BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                ChevronButton b = new ChevronButton(SwingConstants.NORTH);
                installNextButtonListeners(b);
                return b;
            }

            @Override
            protected Component createPreviousButton() {
                ChevronButton b = new ChevronButton(SwingConstants.SOUTH);
                installPreviousButtonListeners(b);
                return b;
            }
        });
        spinner.setBackground(BG_CARD);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JFormattedTextField field = editor.getTextField();
            field.setBackground(BG_CARD);
            field.setForeground(FG_PRIMARY);
            field.setCaretColor(FG_PRIMARY);
            field.setBorder(new EmptyBorder(2, 6, 2, 4));
            field.setHorizontalAlignment(JTextField.CENTER);
            installFocusBorder(spinner, field);
        }
    }

    private static void installFocusBorder(JComponent target) {
        installFocusBorder(target, target);
    }

    private static void installFocusBorder(JComponent target, Component focusSource) {
        target.setBorder(new LineBorder(BORDER, 1, true));
        focusSource.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                target.setBorder(new LineBorder(ACCENT_DIM, 1, true));
            }

            @Override
            public void focusLost(FocusEvent e) {
                target.setBorder(new LineBorder(BORDER, 1, true));
            }
        });
    }

    /** 下拉選單項目加上左右內距，清單中滑鼠所在項目以強調色底標示。 */
    private static final class PaddedComboRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(new EmptyBorder(3, 8, 3, 8));
            setBackground(isSelected ? ACCENT_DIM : BG_CARD);
            setForeground(FG_PRIMARY);
            return this;
        }
    }

    /** 只畫一個 V 形箭頭的扁平按鈕，供下拉選單與數字欄使用。 */
    private static final class ChevronButton extends JButton {
        private final int direction;

        ChevronButton(int direction) {
            this.direction = direction;
            setUI(new BasicButtonUI());
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(18, 12);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel m = getModel();
            if (m.isRollover() || m.isPressed()) {
                g2.setColor(m.isPressed() ? ACCENT_DIM : BG_CARD_HIGHLIGHT);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            int w = getWidth();
            int h = getHeight();
            int size = Math.max(3, Math.min(w, h) / 4);
            int cx = w / 2;
            int cy = h / 2;
            Path2D path = new Path2D.Float();
            if (direction == SwingConstants.NORTH) {
                path.moveTo(cx - size, cy + size / 2.0);
                path.lineTo(cx, cy - size / 2.0);
                path.lineTo(cx + size, cy + size / 2.0);
            } else {
                path.moveTo(cx - size, cy - size / 2.0);
                path.lineTo(cx, cy + size / 2.0);
                path.lineTo(cx + size, cy - size / 2.0);
            }
            g2.setColor(m.isRollover() ? FG_PRIMARY : FG_SECONDARY);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
            g2.dispose();
        }
    }

    /** 扁平捲軸：透明軌道、圓角滑塊，滑鼠移入或拖曳時滑塊變亮，不顯示兩端箭頭。 */
    private static final class FlatScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private static JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // 軌道透明，與背景融為一體。
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging || isThumbRollover() ? ACCENT_DIM : SCROLL_THUMB);
            int pad = 2;
            int arc = Math.min(r.width, r.height) - pad * 2;
            g2.fillRoundRect(r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2, arc, arc);
            g2.dispose();
        }
    }

    public static void styleCard(JComponent component) {
        component.setBackground(BG_CARD);
        component.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 10, 14, 10)));
    }
}
