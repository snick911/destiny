package com.destiny.bazi.tests;

import com.destiny.bazi.SelfTest;

import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 測試進入點：依序執行所有測試，任何一項失敗即以結束碼 1 結束。
 *
 * <p>介面冒煙測試需要顯示環境；無顯示環境（headless）時略過並註明，
 * 可在 Linux 上以 {@code xvfb-run} 提供虛擬顯示。</p>
 */
public final class AllTests {

    private AllTests() {
    }

    public static void main(String[] args) throws java.io.IOException {
        // 人員資料改存到暫存資料夾，測試不會動到使用者真正的人員資料。
        Path dataDir = Files.createTempDirectory("bazi-data");
        System.setProperty(com.destiny.bazi.io.PersonStore.DATA_DIR_PROPERTY, dataDir.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (var paths = Files.walk(dataDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch (java.io.IOException ignored) {
                // 暫存資料夾清不掉不影響測試結果
            }
        }));

        Check.run("節氣時刻", CalendarTests::solarTerms);
        Check.run("四柱與傳統口訣表", CalendarTests::pillars);
        Check.run("十神與藏干表", CalendarTests::tenGodsAndHiddenStems);
        Check.run("流年與流月", CalendarTests::liuNianLiuYue);
        Check.run("大運與交運", DaYunTests::run);
        Check.run("五行質量與十神統計", DaYunTests::massAndCounts);
        Check.run("獨立實作交叉比對", CrossCheckTests::run);
        Check.run("農曆換算（對照曆表）", LunarCalendarTests::run);
        Check.run("紫微斗數排盤", ZiWeiTests::run);
        Check.run("紫微獨立實作交叉比對", ZiWeiTests::crossCheck);
        Check.run("運勢評分（對照 lifeup）", LuckScoreTests::run);
        Check.run("存檔與載入", FileIoTests::run);
        Check.run("人員資料", FileIoTests::personStore);
        Check.run("人員資料 CSV 匯入匯出", PersonCsvTests::run);
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("－ 介面冒煙測試               略過（無顯示環境）");
            System.out.println("－ 自我檢測（--self-test）      略過（無顯示環境）");
        } else {
            Check.run("介面冒煙測試", UiSmokeTests::run);
            Check.run("自我檢測（--self-test）", AllTests::selfTest);
        }

        System.out.println();
        if (Check.failures().isEmpty()) {
            System.out.println("全部通過，共 " + Check.passed() + " 項檢查。");
            System.exit(0);
        }
        System.out.println("失敗 " + Check.failures().size() + " 項：");
        Check.failures().stream().limit(50).forEach(f -> System.out.println("  - " + f));
        System.exit(1);
    }

    /** 與打包版本在 CI 上執行的 --self-test 相同，確認報告格式與結果。 */
    private static void selfTest() throws Exception {
        Path report = Files.createTempFile("bazi-selftest", ".txt");
        try {
            int code = SelfTest.run(report);
            String content = Files.readString(report, StandardCharsets.UTF_8);
            Check.equal("自我檢測結束碼", 0, code);
            Check.isTrue("自我檢測報告", content.endsWith("result=OK\n") || content.endsWith("result=OK\r\n"), content);
        } finally {
            Files.deleteIfExists(report);
        }
    }
}
