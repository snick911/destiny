package com.destiny.bazi.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
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
                return new Font(candidate, Font.PLAIN, 13);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    public static final Color BG_APP = new Color(0x1D, 0x21, 0x27);
    public static final Color BG_PANEL = new Color(0x25, 0x2A, 0x31);
    public static final Color BG_CARD = new Color(0x2C, 0x32, 0x3A);
    public static final Color BORDER = new Color(0x3B, 0x42, 0x4B);

    public static final Color FG_PRIMARY = new Color(0xD8, 0xDE, 0xE9);
    public static final Color FG_SECONDARY = new Color(0x9A, 0xA5, 0xB1);
    public static final Color ACCENT = new Color(0x88, 0xC0, 0xD0);
    public static final Color ACCENT_DIM = new Color(0x5E, 0x81, 0xAC);

    public static final Color ELEMENT_WOOD = new Color(0x9E, 0xCE, 0x9A);
    public static final Color ELEMENT_FIRE = new Color(0xE0, 0x8B, 0x79);
    public static final Color ELEMENT_EARTH = new Color(0xD3, 0xB1, 0x7D);
    public static final Color ELEMENT_METAL = new Color(0xC9, 0xCF, 0xDA);
    public static final Color ELEMENT_WATER = new Color(0x81, 0xA1, 0xC1);

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

    public static void styleAccentButton(JButton button) {
        button.setBackground(ACCENT_DIM);
        button.setForeground(FG_PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 22, 10, 22));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleCard(JComponent component) {
        component.setBackground(BG_CARD);
        component.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 10, 14, 10)));
    }
}
