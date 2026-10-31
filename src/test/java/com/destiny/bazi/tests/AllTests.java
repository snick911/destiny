package com.destiny.bazi.tests;

import java.awt.GraphicsEnvironment;

/**
 * 測試進入點：依序執行所有測試，任何一項失敗即以結束碼 1 結束。
 *
 * <p>介面冒煙測試需要顯示環境；無顯示環境（headless）時略過並註明，
 * 可在 Linux 上以 {@code xvfb-run} 提供虛擬顯示。</p>
 */
public final class AllTests {

    private AllTests() {
    }

    public static void main(String[] args) {
        Check.run("節氣時刻", CalendarTests::solarTerms);
        Check.run("四柱與傳統口訣表", CalendarTests::pillars);
        Check.run("十神與藏干表", CalendarTests::tenGodsAndHiddenStems);
        Check.run("流年與流月", CalendarTests::liuNianLiuYue);
        Check.run("大運與交運", DaYunTests::run);
        Check.run("五行質量與十神統計", DaYunTests::massAndCounts);
        Check.run("獨立實作交叉比對", CrossCheckTests::run);
        Check.run("存檔與載入", FileIoTests::run);
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("－ 介面冒煙測試               略過（無顯示環境）");
        } else {
            Check.run("介面冒煙測試", UiSmokeTests::run);
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
}
