package com.destiny.bazi.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 極簡測試斷言：不依賴 JUnit 等第三方套件，失敗時記錄訊息並繼續執行其餘檢查，
 * 最後由 {@link AllTests} 統一回報並以非零結束碼結束。
 */
final class Check {

    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    private Check() {
    }

    static void equal(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            passed++;
        } else {
            failures.add(name + "：預期 " + expected + "，實際 " + actual);
        }
    }

    static void isTrue(String name, boolean condition, String detail) {
        if (condition) {
            passed++;
        } else {
            failures.add(name + "：" + detail);
        }
    }

    /** 執行一組測試，未預期的例外視為失敗。 */
    static void run(String suite, ThrowingRunnable body) {
        int failedBefore = failures.size();
        int passedBefore = passed;
        long start = System.nanoTime();
        try {
            body.run();
        } catch (Throwable t) {
            failures.add(suite + " 發生例外：" + t);
            t.printStackTrace();
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        int newFailures = failures.size() - failedBefore;
        System.out.printf("%s %-24s 通過 %6d 項，失敗 %d 項（%d ms）%n",
                newFailures == 0 ? "✔" : "✘", suite, passed - passedBefore, newFailures, ms);
    }

    static int passed() {
        return passed;
    }

    static List<String> failures() {
        return failures;
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
