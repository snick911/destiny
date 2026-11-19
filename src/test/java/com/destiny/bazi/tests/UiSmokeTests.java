package com.destiny.bazi.tests;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.ui.BaziFrame;
import com.destiny.bazi.ui.DarkTheme;
import com.destiny.bazi.ui.LuckCurveChart;
import com.destiny.bazi.ziwei.LuckOverlay;
import com.destiny.bazi.ziwei.LuckScore;
import com.destiny.bazi.ziwei.ZiWeiCalculator;
import com.destiny.bazi.ziwei.ZiWeiChart;

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
 * 放大／縮小字體檢查大運卡片不被裁切、最窄視窗下輸入區換行後仍完整顯示，
 * 並繪製匯出圖片；再切換到紫微斗數，
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
            Check.equal("縮小字體不低於預設大小", 1.0, field(frame, "fontScale"));
            checkMinimumFontSize(frame, "八字模式");

            checkInputCardAtMinimumWidth(frame);

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
            Check.isTrue("人員查詢視窗有匯入 CSV 按鈕", ((JButton) field(dialog, "importButton")).isShowing(), "按鈕未顯示");
            Check.isTrue("人員查詢視窗有匯出 CSV 按鈕", ((JButton) field(dialog, "exportButton")).isShowing(), "按鈕未顯示");
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

        // 命盤與 lifeup 同大小（每宮 200×200）、黑底；視窗夠寬時以原尺寸顯示
        Check.equal("命盤原尺寸寬度（每宮 200）", 800, chart.getPreferredSize().width);
        Check.equal("命盤原尺寸高度（每宮 200）", 800, chart.getPreferredSize().height);
        if (frame.getGraphicsConfiguration().getBounds().width >= 1000) {
            Check.equal("切到紫微時命盤不被縮小", 800, chart.getWidth());
        }
        BufferedImage chartImage = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = chartImage.createGraphics();
        chart.printAll(cg);
        cg.dispose();
        int black = 0;
        int total = 0;
        for (int x = 0; x < chartImage.getWidth(); x += 3) {
            for (int y = 0; y < chartImage.getHeight(); y += 3) {
                total++;
                if ((chartImage.getRGB(x, y) & 0xFFFFFF) == 0) {
                    black++;
                }
            }
        }
        Check.isTrue("命盤黑底", black * 2 > total, "黑色像素 " + black + " / " + total);

        // 中宮：各層干支與好運指數（lifeup 評分）
        depth.setSelectedIndex(3);
        @SuppressWarnings("unchecked")
        List<String[]> centerRows = (List<String[]>) call(chart, "luckRows");
        Check.equal("流月盤中宮行運列數", 6, centerRows.size());
        for (int i = 1; i < centerRows.size(); i += 2) {
            Check.isTrue("中宮好運指數列", centerRows.get(i)[0].equals("好運指數")
                    && centerRows.get(i)[1].matches("-?\\d+　.+"), String.join(" ", centerRows.get(i)));
        }
        depth.setSelectedIndex(0);
        @SuppressWarnings("unchecked")
        List<String[]> natalRows = (List<String[]>) call(chart, "luckRows");
        Check.equal("本命盤中宮不顯示行運", 0, natalRows.size());

        // 運勢指數區塊：摘要與所選大限十個流年的兩條曲線
        String scoreText = ((JLabel) field(frame, "scoreSummaryLabel")).getText();
        Check.isTrue("運勢指數摘要", scoreText.contains("本命財運指數") && scoreText.contains("大限 丙寅 好運指數")
                && scoreText.contains("流月"), scoreText);
        ZiWeiCalculator calc = new ZiWeiCalculator();
        ZiWeiChart zw = calc.calculate(1984, 7, 27, 0, 0, Gender.MALE);
        LuckOverlay dec = calc.decade(zw, 7);
        LuckCurveChart luckCurve = (LuckCurveChart) field(frame, "yearLuckChart");
        LuckCurveChart wealthCurve = (LuckCurveChart) field(frame, "yearWealthChart");
        int[] luckValues = (int[]) call(luckCurve, "values");
        int[] wealthValues = (int[]) call(wealthCurve, "values");
        Check.equal("流年好運曲線點數", 10, luckValues.length);
        for (int i = 0; i < luckValues.length; i++) {
            LuckOverlay yo = calc.year(zw, zw.getDecades().get(7).startYear() + i);
            Check.equal("流年好運曲線第" + (i + 1) + "點", LuckScore.yearLuck(zw, dec, yo), luckValues[i]);
            Check.equal("流年財運曲線第" + (i + 1) + "點", LuckScore.yearWealth(zw, dec, yo), wealthValues[i]);
        }
        // 點選曲線上的第一個年份：流年選單切到該年
        frame.validate();
        int clickX = -1;
        int midY = luckCurve.getHeight() / 2;
        for (int x = 0; x < luckCurve.getWidth() && clickX < 0; x++) {
            MouseEvent probe = new MouseEvent(luckCurve, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, midY, 0, false);
            if (luckCurve.getToolTipText(probe) != null) {
                clickX = x + 4;
            }
        }
        Check.isTrue("曲線有可點選的年份", clickX >= 0, "找不到資料點");
        luckCurve.dispatchEvent(new MouseEvent(luckCurve, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, clickX, midY, 1, false, MouseEvent.BUTTON1));
        Check.isTrue("點選曲線切換流年", String.valueOf(year.getSelectedItem()).startsWith("2058年"),
                String.valueOf(year.getSelectedItem()));
        Check.isTrue("點選曲線後盤面疊到流年", depth.getSelectedIndex() >= 2, "盤面 " + depth.getSelectedIndex());

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
        checkMinimumFontSize(frame, "紫微模式");

        BufferedImage image = checkExportImage(frame, "紫微匯出圖片");
        Check.isTrue("紫微匯出圖片含完整命盤", image.getHeight() > chart.getPreferredSize().height,
                image.getHeight() + " <= " + chart.getPreferredSize().height);

        invoke(frame, "setZiWeiMode", false);
        frame.validate();
        Check.equal("切回八字標題", "子平八字排盤", title.getText());
        Check.isTrue("切回八字後紫微命盤隱藏", !chart.isShowing(), "命盤仍顯示");
        checkDaYunNotClipped(frame, "切回八字");
    }

    /** 畫面上所有顯示中的文字元件字級都不小於 14（基準字級，字體縮放為預設時）。 */
    private static void checkMinimumFontSize(BaziFrame frame, String name) {
        frame.validate();
        List<String> tooSmall = new java.util.ArrayList<>();
        collectSmallFonts(frame.getContentPane(), tooSmall);
        Check.isTrue(name + " 最小字級 14", tooSmall.isEmpty(), String.join("、", tooSmall));
    }

    private static void collectSmallFonts(Container container, List<String> tooSmall) {
        for (Component c : container.getComponents()) {
            if (!c.isShowing()) {
                continue;
            }
            boolean text = c instanceof JLabel || c instanceof AbstractButton || c instanceof JComboBox
                    || c instanceof javax.swing.text.JTextComponent;
            if (text && c.getFont() != null && c.getFont().getSize2D() < 13.99f) {
                String label = c instanceof JLabel l ? l.getText()
                        : c instanceof AbstractButton b ? b.getText() : c.getClass().getSimpleName();
                tooSmall.add(label + "（" + c.getFont().getSize2D() + "）");
            }
            if (c instanceof Container child) {
                collectSmallFonts(child, tooSmall);
            }
        }
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

    /** 輸入區中依序檢查的輸入元件（第二列尾端的性別與排盤按鈕最容易被換到下一列）。 */
    private static final List<String> INPUT_FIELDS = List.of("nameField", "savePersonButton", "openPeopleButton",
            "yearSpinner", "monthSpinner", "daySpinner", "hourCombo", "minuteSpinner", "genderCombo", "calcButton");

    /**
     * 最窄視窗、最大字體時輸入區換行，換行後的元件仍須完整位於輸入卡片內（FlowLayout 只回報一列高度，
     * 換到下一列的「分、性別、排盤」曾被裁切而看不到）。依序檢查：視窗縮到最小寬度後逐步放大字體；
     * 字體最大時視窗由寬（輸入列排成一列）縮到最窄（換成兩列），即換行列數在同一次排版中改變。
     */
    private static void checkInputCardAtMinimumWidth(BaziFrame frame) throws Exception {
        Dimension original = frame.getSize();
        Dimension min = frame.getMinimumSize();
        JComponent year = (JComponent) field(frame, "yearSpinner");
        JComponent calc = (JComponent) field(frame, "calcButton");
        try {
            resize(frame, min);
            Check.equal("視窗縮到最小寬度", min.width, frame.getWidth());
            for (int step = 0; step < 12; step++) {
                invoke(frame, "adjustFontScale", 0.1);
                checkInputsInsideCard(frame, "最小寬度放大字體第" + (step + 1) + "次");
            }
            Check.equal("字體放到最大", 1.8, field(frame, "fontScale"));
            Check.isTrue("最小寬度、最大字體時輸入列換行", !sameLine(year, calc), "排盤按鈕仍與年在同一列");

            // 排盤按鈕捲動到可視範圍後應完整可見
            calc.scrollRectToVisible(new Rectangle(calc.getSize()));
            Rectangle visible = calc.getVisibleRect();
            Check.isTrue("捲動後排盤按鈕完整可見", visible.width == calc.getWidth() && visible.height == calc.getHeight(),
                    "可見範圍 " + visible + "，按鈕大小 " + calc.getSize());
            ((JComponent) field(frame, "exportPanel")).scrollRectToVisible(new Rectangle(0, 0, 1, 1));

            resize(frame, new Dimension(1250, min.height));
            Check.isTrue("寬視窗、最大字體時輸入列不換行", sameLine(year, calc),
                    "視窗寬 " + frame.getWidth() + " 時排盤按鈕已換行");
            resize(frame, min);
            Check.equal("視窗由寬縮到最小寬度", min.width, frame.getWidth());
            checkInputsInsideCard(frame, "最大字體由寬視窗縮到最小寬度");
        } finally {
            invoke(frame, "resetFontScale");
            resize(frame, original);
        }
    }

    private static void checkInputsInsideCard(BaziFrame frame, String name) throws ReflectiveOperationException {
        frame.validate();
        JComponent export = (JComponent) field(frame, "exportPanel");
        for (String fieldName : INPUT_FIELDS) {
            JComponent c = (JComponent) field(frame, fieldName);
            Container card = c.getParent();
            while (card != null && card.getParent() != export) {
                card = card.getParent();
            }
            String clipped = card == null ? "不在輸入卡片內" : clipping(c, card);
            Check.isTrue(name + " " + fieldName + " 完整位於輸入卡片內", c.isShowing() && clipped == null,
                    clipped == null ? "未顯示" : clipped);
        }
    }

    /** 由元件逐層往上到輸入卡片的內容區（扣除邊框），超出任一層的範圍時傳回說明；完整位於其內則傳回 null。 */
    private static String clipping(Component c, Container card) {
        Rectangle r = new Rectangle(0, 0, c.getWidth(), c.getHeight());
        if (r.isEmpty()) {
            return "大小為 " + c.getSize();
        }
        for (Component cur = c; cur != card; cur = cur.getParent()) {
            Container parent = cur.getParent();
            r.translate(cur.getX(), cur.getY());
            Rectangle area = new Rectangle(0, 0, parent.getWidth(), parent.getHeight());
            if (parent == card) {
                Insets in = card.getInsets();
                area = new Rectangle(in.left, in.top,
                        card.getWidth() - in.left - in.right, card.getHeight() - in.top - in.bottom);
            }
            if (!area.contains(r)) {
                return r + " 超出 " + parent.getClass().getSimpleName() + " 範圍 " + area;
            }
        }
        return null;
    }

    /** 兩個輸入元件（連同上方標籤）是否排在同一列。 */
    private static boolean sameLine(JComponent a, JComponent b) {
        Component wa = a.getParent();
        Component wb = b.getParent();
        return wa.getParent() == wb.getParent()
                && wb.getY() < wa.getY() + wa.getHeight() && wa.getY() < wb.getY() + wb.getHeight();
    }

    /** 改變視窗大小並完成排版，包含換行列數改變後延後到事件佇列的那次重新排版。 */
    private static void resize(BaziFrame frame, Dimension size) {
        frame.setSize(size);
        frame.validate();
        flushEvents();
        frame.validate();
    }

    /**
     * 處理事件佇列中已排入的工作。測試全程在 EDT 上執行，排入佇列的工作不會自行執行，
     * 因此以 SecondaryLoop 在 EDT 上暫時處理事件，直到排在其後的結束事件為止。
     */
    private static void flushEvents() {
        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(loop::exit));
        loop.enter();
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
