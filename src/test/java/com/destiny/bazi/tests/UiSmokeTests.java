package com.destiny.bazi.tests;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.ui.BaziFrame;
import com.destiny.bazi.ui.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

/**
 * 介面冒煙測試：實際開啟主視窗，輸入命盤後逐一切換所有大運、流年、流月，
 * 放大／縮小字體檢查大運卡片不被裁切，並繪製匯出圖片；再切換到紫微斗數，
 * 逐一切換大限、流年、流月，點選宮位、縮放字體並匯出圖片。
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

            // 「日」上限依年月自動調整，超出時改為月底
            JSpinner yearSp = (JSpinner) field(frame, "yearSpinner");
            JSpinner monthSp = (JSpinner) field(frame, "monthSpinner");
            JSpinner daySp = (JSpinner) field(frame, "daySpinner");
            yearSp.setValue(1951);
            monthSp.setValue(1);
            daySp.setValue(31);
            monthSp.setValue(2);
            Check.equal("1951年2月日上限", 28, ((SpinnerNumberModel) daySp.getModel()).getMaximum());
            Check.equal("超出月底的日改為月底", 28, daySp.getValue());
            yearSp.setValue(1952);
            Check.equal("閏年2月日上限", 29, ((SpinnerNumberModel) daySp.getModel()).getMaximum());
            monthSp.setValue(3);
            Check.equal("3月日上限", 31, ((SpinnerNumberModel) daySp.getModel()).getMaximum());

            // 改變輸入欄位即自動重新排盤（不必按「排盤」）
            String summary = ((JLabel) field(frame, "summaryLabel")).getText();
            daySp.setValue(15);
            Check.isTrue("改日期自動重排", !summary.equals(((JLabel) field(frame, "summaryLabel")).getText()),
                    "命盤摘要未更新：" + summary);

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

            // 預設定位到今天所在的大運、流年、流月（本測試命盤的大運涵蓋 1959~2038 年）
            int thisYear = LocalDate.now().getYear();
            if (thisYear > 1960 && thisYear < 2038) {
                Check.isTrue("預設選到目前大運", String.valueOf(dayun.getSelectedItem()).contains("● 目前"),
                        String.valueOf(dayun.getSelectedItem()));
                Check.isTrue("預設選到今年", String.valueOf(liunian.getSelectedItem()).contains("● 今年"),
                        String.valueOf(liunian.getSelectedItem()));
                Check.isTrue("預設選到本月", String.valueOf(liuyue.getSelectedItem()).contains("● 本月"),
                        String.valueOf(liuyue.getSelectedItem()));
                int currentIdx = dayun.getSelectedIndex();
                String badge = ((JLabel) field(dayunPanels.get(currentIdx), "badgeLabel")).getText();
                Check.isTrue("目前大運卡片標記", badge.contains("目前"), badge);
            }

            // 點選大運卡片即選取該運，且只有該卡片呈選取狀態
            for (int i = 0; i < dayunPanels.size(); i++) {
                JComponent card = (JComponent) dayunPanels.get(i);
                card.dispatchEvent(new MouseEvent(card, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, 5, 5, 1, false, MouseEvent.BUTTON1));
                Check.equal("點選第" + (i + 1) + "張大運卡片", i, dayun.getSelectedIndex());
                for (int j = 0; j < dayunPanels.size(); j++) {
                    Check.equal("第" + (j + 1) + "張卡片選取狀態", i == j, field(dayunPanels.get(j), "selected"));
                }
            }

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

            // 姓名、儲存人員、從人員查詢視窗帶入
            JTextField nameField = (JTextField) field(frame, "nameField");
            nameField.setText("測試人員甲");
            String summaryWithName = ((JLabel) field(frame, "summaryLabel")).getText();
            Check.isTrue("命盤摘要顯示姓名", summaryWithName.startsWith("姓名：測試人員甲"), summaryWithName);
            invoke(frame, "savePerson");
            com.destiny.bazi.io.PersonStore store = (com.destiny.bazi.io.PersonStore) field(frame, "personStore");
            com.destiny.bazi.io.BirthInput saved = store.find("測試人員甲");
            Check.equal("儲存人員", new com.destiny.bazi.io.BirthInput(1950, 9, 4, 4, 31, Gender.FEMALE, "測試人員甲"), saved);
            String status = ((JLabel) field(frame, "statusLabel")).getText();
            Check.isTrue("儲存後狀態提示", status.contains("已儲存「測試人員甲」"), status);

            nameField.setText("");
            invoke(frame, "savePerson");
            status = ((JLabel) field(frame, "statusLabel")).getText();
            Check.isTrue("未填姓名時提示", status.contains("請先輸入姓名"), status);

            // 換成別的命盤後，從人員查詢視窗帶入剛才儲存的人員
            ((JSpinner) field(frame, "yearSpinner")).setValue(2001);
            invoke(frame, "openPeople");
            JDialog dialog = (JDialog) field(frame, "personDialog");
            Check.isTrue("人員查詢視窗開啟", dialog.isVisible(), "視窗未顯示");
            JTable table = (JTable) field(dialog, "table");
            JTextField search = (JTextField) field(dialog, "searchField");
            search.setText("不存在的名字");
            Check.equal("搜尋無結果", 0, table.getRowCount());
            search.setText("1950-09");
            Check.equal("依日期搜尋", 1, table.getRowCount());
            search.setText("測試人員");
            Check.equal("依姓名搜尋", 1, table.getRowCount());
            Check.equal("八字欄位", "庚寅 甲申 壬寅 壬寅", table.getValueAt(0, 3));
            search.postActionEvent();
            Check.isTrue("帶入後關閉視窗", !dialog.isVisible(), "視窗仍開啟");
            Check.equal("帶入姓名", "測試人員甲", nameField.getText());
            Check.equal("帶入出生年", 1950, ((JSpinner) field(frame, "yearSpinner")).getValue());
            Check.equal("帶入後重新排盤", "1959~1968年（9~18歲）　癸未",
                    String.valueOf(dayun.getItemAt(0)).replace("　● 目前", ""));
            store.remove("測試人員甲");

            // 繪製匯出圖片（與「匯出圖片」相同方式），確認有實際內容
            checkExportImage(frame, "八字匯出圖片");

            ziWei(frame);
        } finally {
            frame.dispose();
        }
    }

    /** 以「匯出圖片」相同的方法繪製整頁，確認圖片有實際內容。 */
    private static BufferedImage checkExportImage(BaziFrame frame, String name) throws ReflectiveOperationException {
        BufferedImage image = (BufferedImage) call(frame, "renderExportImage");
        int background = DarkTheme.BG_APP.getRGB();
        int drawn = 0;
        for (int x = 0; x < image.getWidth(); x += 4) {
            for (int y = 0; y < image.getHeight(); y += 4) {
                if (image.getRGB(x, y) != background) {
                    drawn++;
                }
            }
        }
        Check.isTrue(name + "有內容", drawn > 1000, "非背景像素僅 " + drawn);
        return image;
    }

    /** 紫微斗數：切換模式、逐一切換大限／流年／流月、點選宮位、字體縮放與匯出圖片。 */
    private static void ziWei(BaziFrame frame) throws Exception {
        JLabel title = (JLabel) field(frame, "titleLabel");
        JComponent chart = (JComponent) field(frame, "ziWeiChartPanel");
        invoke(frame, "setZiWeiMode", true);
        frame.validate();
        Check.equal("紫微模式標題", "紫微斗數排盤", title.getText());
        Check.isTrue("紫微命盤顯示", chart.isShowing(), "命盤未顯示");
        Check.isTrue("紫微模式切換按鈕", ((JToggleButton) field(frame, "ziweiModeButton")).isSelected(), "未選取");

        ((JTextField) field(frame, "nameField")).setText("");
        ((JSpinner) field(frame, "yearSpinner")).setValue(1984);
        ((JSpinner) field(frame, "monthSpinner")).setValue(7);
        ((JSpinner) field(frame, "daySpinner")).setValue(27);
        ((JSpinner) field(frame, "minuteSpinner")).setValue(0);
        ((JComboBox<?>) field(frame, "hourCombo")).setSelectedIndex(0);
        ((JComboBox<?>) field(frame, "genderCombo")).setSelectedItem(Gender.MALE);
        invoke(frame, "calculate");

        String note = ((JLabel) field(frame, "ziweiNoteLabel")).getText();
        Check.isTrue("紫微說明列", note.contains("甲子年六月廿九") && note.contains("土五局"), note);

        JComboBox<?> depth = (JComboBox<?>) field(frame, "ziweiDepthCombo");
        JComboBox<?> decade = (JComboBox<?>) field(frame, "ziweiDecadeCombo");
        JComboBox<?> year = (JComboBox<?>) field(frame, "ziweiYearCombo");
        JComboBox<?> month = (JComboBox<?>) field(frame, "ziweiMonthCombo");
        Check.equal("盤面選項數", 4, depth.getItemCount());
        Check.equal("大限選項數", 12, decade.getItemCount());
        Check.isTrue("第一限文字", String.valueOf(decade.getItemAt(0)).startsWith("辛未 5~14歲（1988~1997）"),
                String.valueOf(decade.getItemAt(0)));
        int thisYear = LocalDate.now().getYear();
        if (thisYear > 1990 && thisYear < 2100) {
            Check.isTrue("預設選到目前大限", String.valueOf(decade.getSelectedItem()).contains("● 目前"),
                    String.valueOf(decade.getSelectedItem()));
            Check.isTrue("預設選到今年", String.valueOf(year.getSelectedItem()).contains("● 今年"),
                    String.valueOf(year.getSelectedItem()));
            Check.isTrue("預設選到本月", String.valueOf(month.getSelectedItem()).contains("● 本月"),
                    String.valueOf(month.getSelectedItem()));
        }

        // 選擇大限／流年／流月時，盤面自動疊到該層
        depth.setSelectedIndex(0);
        Check.isTrue("本命盤不疊大限", field(chart, "decade") == null, "仍有大限");
        decade.setSelectedIndex(2);
        Check.equal("選大限後盤面", 1, depth.getSelectedIndex());
        Check.isTrue("大限盤不疊流年", field(chart, "decade") != null && field(chart, "year") == null, "疊層不符");
        year.setSelectedIndex(1);
        Check.equal("選流年後盤面", 2, depth.getSelectedIndex());
        month.setSelectedIndex(5);
        Check.equal("選流月後盤面", 3, depth.getSelectedIndex());
        Check.isTrue("流月盤三層皆疊上", field(chart, "month") != null && field(chart, "year") != null, "疊層不符");

        for (int i = 0; i < decade.getItemCount(); i++) {
            decade.setSelectedIndex(i);
            Check.equal("第" + (i + 1) + "限流年項數", i == 0 ? 14 : 10, year.getItemCount());
            for (int j = 0; j < year.getItemCount(); j++) {
                year.setSelectedIndex(j);
                Check.equal("流月項數", 12, month.getItemCount());
                for (int k = 0; k < month.getItemCount(); k++) {
                    month.setSelectedIndex(k);
                }
            }
        }
        decade.setSelectedIndex(0);
        Check.isTrue("第一限含起限前的出生年", String.valueOf(year.getItemAt(0)).startsWith("1984年 甲子（1歲）"),
                String.valueOf(year.getItemAt(0)));

        // 點選左下角的寅宮：切換到該宮的大限（75~84 歲，第八限）
        frame.validate();
        int w = chart.getWidth();
        int h = chart.getHeight();
        chart.dispatchEvent(new MouseEvent(chart, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, w / 8, h * 7 / 8, 1, false, MouseEvent.BUTTON1));
        Check.equal("點選寅宮切換大限", 7, decade.getSelectedIndex());
        Check.isTrue("點選後大限文字", String.valueOf(decade.getSelectedItem()).startsWith("丙寅 75~84歲"),
                String.valueOf(decade.getSelectedItem()));

        // 字體放到最大、縮到最小：命盤寬度不超出所在區塊，繪製不出錯
        for (int step = 0; step < 27; step++) {
            invoke(frame, "adjustFontScale", step < 12 ? 0.1 : -0.1);
            frame.validate();
            Check.isTrue("命盤寬度不超出區塊（第" + (step + 1) + "次）",
                    chart.getWidth() <= chart.getParent().getWidth(), chart.getWidth() + " > " + chart.getParent().getWidth());
            BufferedImage img = new BufferedImage(Math.max(1, chart.getWidth()), Math.max(1, chart.getHeight()),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            chart.printAll(g);
            g.dispose();
        }
        invoke(frame, "resetFontScale");

        BufferedImage image = checkExportImage(frame, "紫微匯出圖片");
        Check.isTrue("紫微匯出圖片含完整命盤", image.getHeight() > chart.getPreferredSize().height,
                image.getHeight() + " <= " + chart.getPreferredSize().height);

        invoke(frame, "setZiWeiMode", false);
        frame.validate();
        Check.equal("切回八字標題", "子平八字排盤", title.getText());
        Check.isTrue("切回八字後紫微命盤隱藏", !chart.isShowing(), "命盤仍顯示");
        checkDaYunNotClipped(frame, "切回八字");
    }

    private static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                return m.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(name);
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
