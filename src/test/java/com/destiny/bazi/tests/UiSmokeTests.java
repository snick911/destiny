package com.destiny.bazi.tests;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.ui.BaziFrame;
import com.destiny.bazi.ui.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 介面冒煙測試：實際開啟主視窗，輸入命盤後逐一切換所有大運、流年、流月，
 * 放大／縮小字體檢查大運卡片不被裁切，並繪製匯出圖片。
 * 透過反射存取主視窗的私有欄位，僅作為測試用途。
 */
final class UiSmokeTests {

    private UiSmokeTests() {
    }

    static void run() throws Exception {
        Throwable[] error = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                runOnEdt();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError("介面測試發生例外", error[0]);
        }
    }

    private static void runOnEdt() throws Exception {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // 與 Main 相同：沒有 Nimbus 時沿用預設外觀
        }
        DarkTheme.applyGlobal();
        BaziFrame frame = new BaziFrame();
        try {
            frame.setVisible(true);
            Check.equal("視窗標題", AppInfo.NAME + " v" + AppInfo.VERSION, frame.getTitle());

            ((JSpinner) field(frame, "yearSpinner")).setValue(1950);
            ((JSpinner) field(frame, "monthSpinner")).setValue(9);
            ((JSpinner) field(frame, "daySpinner")).setValue(4);
            ((JSpinner) field(frame, "minuteSpinner")).setValue(31);
            ((JComboBox<?>) field(frame, "genderCombo")).setSelectedItem(Gender.FEMALE);
            ((JComboBox<?>) field(frame, "hourCombo")).setSelectedIndex(4);
            invoke(frame, "calculate");

            String note = ((JLabel) field(frame, "dayunNoteLabel")).getText();
            Check.isTrue("大運說明列交運日期", note.contains("1959 年 8 月 1 日交運"), note);
            List<?> dayunPanels = (List<?>) field(frame, "dayunPanels");
            String firstTitle = ((JLabel) field(dayunPanels.get(0), "titleLabel")).getText();
            Check.isTrue("首運卡片標題", firstTitle.contains("1959~1968") && firstTitle.contains("9~18歲"), firstTitle);

            JComboBox<?> dayun = (JComboBox<?>) field(frame, "dayunSelectCombo");
            JComboBox<?> liunian = (JComboBox<?>) field(frame, "liunianSelectCombo");
            JComboBox<?> liuyue = (JComboBox<?>) field(frame, "liuyueSelectCombo");
            Check.equal("大運選單項數", 8, dayun.getItemCount());
            Check.equal("首運選單文字", "1959~1968年（9~18歲）　癸未", dayun.getItemAt(0));

            for (int i = 0; i < dayun.getItemCount(); i++) {
                dayun.setSelectedIndex(i);
                int startYear = 1959 + 10 * i;
                Check.equal("第" + (i + 1) + "運流年項數", 10, liunian.getItemCount());
                Check.isTrue("第" + (i + 1) + "運首個流年", String.valueOf(liunian.getItemAt(0)).startsWith(startYear + "年（"
                        + (startYear - 1950) + "歲）"), String.valueOf(liunian.getItemAt(0)));
                for (int j = 0; j < liunian.getItemCount(); j++) {
                    liunian.setSelectedIndex(j);
                    Check.equal("流月項數", 12, liuyue.getItemCount());
                    for (int k = 0; k < liuyue.getItemCount(); k++) {
                        liuyue.setSelectedIndex(k);
                    }
                }
            }

            // 字體放到最大、縮到最小，大運卡片都不應被裁切
            for (int step = 0; step < 12; step++) {
                invoke(frame, "adjustFontScale", 0.1);
                checkDaYunNotClipped(frame, "放大字體第" + (step + 1) + "次");
            }
            for (int step = 0; step < 15; step++) {
                invoke(frame, "adjustFontScale", -0.1);
                checkDaYunNotClipped(frame, "縮小字體第" + (step + 1) + "次");
            }
            invoke(frame, "resetFontScale");

            // 繪製匯出圖片（與「匯出圖片」相同方式），確認有實際內容
            JComponent export = (JComponent) field(frame, "exportPanel");
            Dimension size = export.getPreferredSize();
            BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(DarkTheme.BG_APP);
            g.fillRect(0, 0, size.width, size.height);
            export.setSize(size);
            export.validate();
            export.printAll(g);
            g.dispose();
            int background = DarkTheme.BG_APP.getRGB();
            int drawn = 0;
            for (int x = 0; x < size.width; x += 4) {
                for (int y = 0; y < size.height; y += 4) {
                    if (image.getRGB(x, y) != background) {
                        drawn++;
                    }
                }
            }
            Check.isTrue("匯出圖片有內容", drawn > 1000, "非背景像素僅 " + drawn);
        } finally {
            frame.dispose();
        }
    }

    private static void checkDaYunNotClipped(BaziFrame frame, String name) {
        frame.validate();
        JScrollPane strip = findDaYunStrip(frame.getContentPane());
        Check.isTrue(name + " 找到大運區塊", strip != null, "找不到大運捲動區");
        if (strip != null) {
            int visible = strip.getViewport().getExtentSize().height;
            int needed = strip.getViewport().getView().getPreferredSize().height;
            Check.isTrue(name + " 大運卡片完整顯示", visible >= needed, "可見高度 " + visible + " < 所需 " + needed);
        }
    }

    /** 大運區塊是唯一一個不顯示垂直捲軸的捲動區。 */
    private static JScrollPane findDaYunStrip(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JScrollPane sp && sp.getVerticalScrollBarPolicy() == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
                return sp;
            }
            if (c instanceof Container child) {
                JScrollPane found = findDaYunStrip(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invoke(Object target, String name, Object... args) throws ReflectiveOperationException {
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                m.invoke(target, args);
                return;
            }
        }
        throw new NoSuchMethodException(name);
    }
}
