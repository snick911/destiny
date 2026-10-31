package com.destiny.bazi;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.DaYunResult;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.ui.BaziFrame;
import com.destiny.bazi.ui.DarkTheme;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 自我檢測（{@code --self-test <報告檔>}）：在實際安裝／打包後的環境中開啟主視窗、
 * 排一張已知命盤，並將結果寫入報告檔，用於驗證打包版本（例如 Windows 免安裝版）
 * 可正常啟動。全部正常時結束碼為 0，報告最後一行為 {@code result=OK}。
 */
public final class SelfTest {

    public static final String FLAG = "--self-test";

    private static final String SAMPLE_TEXT = "子平八字排盤";

    private SelfTest() {
    }

    /** 執行自我檢測並寫出報告，回傳結束碼（0 為正常）。不可在事件分派執行緒上呼叫。 */
    public static int run(Path report) {
        List<String> lines = new ArrayList<>();
        boolean ok;
        try {
            lines.add("version=" + AppInfo.VERSION);
            lines.add("os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            lines.add("java=" + System.getProperty("java.runtime.version"));
            lines.add("headless=" + GraphicsEnvironment.isHeadless());

            String[] title = new String[1];
            boolean[] showing = new boolean[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                } catch (Exception ignored) {
                    // 與 Main 相同：沒有 Nimbus 時沿用預設外觀
                }
                DarkTheme.applyGlobal();
                BaziFrame frame = new BaziFrame();
                frame.setVisible(true);
                title[0] = frame.getTitle();
                showing[0] = frame.isShowing();
                frame.dispose();
            });
            boolean titleOk = (AppInfo.NAME + " v" + AppInfo.VERSION).equals(title[0]);
            lines.add("window.title=" + title[0]);
            lines.add("window.showing=" + showing[0]);

            String font = DarkTheme.BASE_FONT.getFamily();
            boolean fontOk = DarkTheme.BASE_FONT.canDisplayUpTo(SAMPLE_TEXT) == -1;
            lines.add("font=" + font + "（可顯示中文：" + fontOk + "）");

            BaziCalculator calculator = new BaziCalculator();
            BaziResult chart = calculator.calculate(1893, 12, 26, 8, 0);
            String pillars = chart.getYearPillar() + " " + chart.getMonthPillar() + " "
                    + chart.getDayPillar() + " " + chart.getHourPillar();
            boolean chartOk = "癸巳 甲子 丁酉 甲辰".equals(pillars);
            lines.add("chart.1893-12-26T08:00=" + pillars);

            BaziResult sample = calculator.calculate(1950, 9, 4, 4, 31);
            DaYunResult dayun = calculator.calculateDaYun(sample, 1950, 9, 4, 4, 31, Gender.FEMALE);
            boolean dayunOk = LocalDate.of(1959, 8, 1).equals(dayun.getStartDate())
                    && dayun.getPeriods().get(0).getStartYear() == 1959;
            lines.add("dayun.1950-09-04T04:31.F=" + dayun.getStartDate() + " / " + dayun.getPeriods().get(0).getStartYear());

            ok = titleOk && showing[0] && fontOk && chartOk && dayunOk;
        } catch (Exception e) {
            lines.add("error=" + e);
            ok = false;
        }
        lines.add("result=" + (ok ? "OK" : "FAIL"));

        try {
            Files.write(report, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return 2;
        }
        return ok ? 0 : 1;
    }
}
