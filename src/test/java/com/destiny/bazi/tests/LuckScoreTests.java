package com.destiny.bazi.tests;

import com.destiny.bazi.core.Gender;
import com.destiny.bazi.ziwei.LuckOverlay;
import com.destiny.bazi.ziwei.LuckScore;
import com.destiny.bazi.ziwei.ZiWeiCalculator;
import com.destiny.bazi.ziwei.ZiWeiChart;

import java.time.LocalDate;
import java.util.Random;

/**
 * lifeup 評分（好運指數、財運指數）測試。
 *
 * <p>預期值由 lifeup 的原始程式（LifeService＋LifeNumberService）實際計算得出：以農曆
 * 輸入排盤、取第 k 步大限，流年＝大限×60%＋流年×40%、流月＝流月×70%＋流年×30%。
 * 所選命盤在 lifeup 與本程式排出的星曜相同，且大限、流年、流月的天干都不是庚
 * （lifeup 漏排庚干的天魁天鉞，本程式已修正，庚干的分數因而可能不同）。</p>
 */
final class LuckScoreTests {

    private static final ZiWeiCalculator CALC = new ZiWeiCalculator();

    /**
     * 西曆年、月、日、時、男(1)/女(0)、大限索引、流年、流月、
     * 本命財運、大限好運、大限財運、流年好運、流年財運、流月好運。
     */
    private static final int[][] LIFEUP_RESULTS = {
            {1968, 9, 30, 5, 0, 1, 1989, 11, -30, 44, -111, 34, -15, 52}, // 戊申年八月初九 女 卯時（農曆）
            {1970, 1, 1, 0, 1, 2, 1994, 3, 148, 47, -24, 46, -44, 55}, // 己酉年十一月廿四 男 子時（農曆）
            {1985, 12, 9, 5, 0, 1, 1997, 8, 70, 66, -31, 62, 39, 62}, // 乙丑年十月廿八 女 卯時（農曆）
            {2003, 3, 17, 13, 1, 4, 2048, 11, 35, 18, 15, 25, 290, 48}, // 癸未年二月十五 男 未時（農曆）
            {2004, 6, 28, 19, 0, 4, 2047, 5, -4, 33, 19, 42, -365, 36}, // 甲申年五月十一 女 戌時（農曆）
            {1952, 3, 28, 13, 0, 1, 1974, 9, -35, 55, 407, 65, -55, 51}, // 壬辰年三月初三 女 未時（農曆）
            {1994, 2, 11, 21, 0, 5, 2055, 11, -149, 81, 35, 68, 35, 46}, // 甲戌年正月初二 女 亥時（農曆）
            {2004, 5, 24, 0, 0, 1, 2019, 9, 12, 27, 14, 48, 148, 46}, // 甲申年四月初六 女 子時（農曆）
            {1956, 7, 12, 21, 1, 5, 2012, 5, -290, 53, -39, 56, 15, 54}, // 丙申年六月初五 男 亥時（農曆）
            {1943, 10, 8, 11, 0, 5, 1998, 10, -34, 46, -1, 41, 402, 48}, // 癸未年九月初十 女 午時（農曆）
            {1979, 1, 31, 0, 0, 5, 2043, 8, -54, 80, -195, 77, 40, 68}, // 己未年正月初四 女 子時（農曆）
            {1954, 8, 31, 13, 1, 3, 1989, 3, -130, 24, 46, 23, 70, 59}, // 甲午年八月初四 男 未時（農曆）
            {1944, 2, 4, 19, 0, 1, 1956, 9, -225, 66, 2, 54, 60, 55}, // 甲申年正月十一 女 戌時（農曆）
            {1982, 6, 27, 19, 0, 2, 2009, 6, 1, 51, 21, 50, 75, 65}, // 壬戌年五月初七 女 戌時（農曆）
    };

    private LuckScoreTests() {
    }

    static void run() {
        for (int[] f : LIFEUP_RESULTS) {
            ZiWeiChart chart = CALC.calculate(f[0], f[1], f[2], f[3], 0, f[4] == 1 ? Gender.MALE : Gender.FEMALE);
            LuckOverlay decade = CALC.decade(chart, f[5]);
            LuckOverlay year = CALC.year(chart, f[6]);
            LuckOverlay month = CALC.month(chart, f[6], f[7]);
            String tag = LocalDate.of(f[0], f[1], f[2]) + " " + f[3] + "時 " + (f[4] == 1 ? "男" : "女");
            Check.equal(tag + " 本命財運指數", f[8], LuckScore.natalWealth(chart));
            Check.equal(tag + " 大限好運指數", f[9], LuckScore.decadeLuck(chart, decade));
            Check.equal(tag + " 大限財運指數", f[10], LuckScore.decadeWealth(chart, decade));
            Check.equal(tag + " 流年好運指數", f[11], LuckScore.yearLuck(chart, decade, year));
            Check.equal(tag + " 流年財運指數", f[12], LuckScore.yearWealth(chart, decade, year));
            Check.equal(tag + " 流月好運指數", f[13], LuckScore.monthLuck(chart, decade, year, month));
        }

        // 組合公式與範圍：好運指數介於 -1 與 101 之間
        Random random = new Random(11);
        for (int n = 0; n < 300; n++) {
            LocalDate d = LocalDate.of(1930, 1, 1).plusDays(random.nextInt(365 * 80));
            ZiWeiChart chart = CALC.calculate(d.getYear(), d.getMonthValue(), d.getDayOfMonth(), random.nextInt(23),
                    0, random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
            int k = random.nextInt(8);
            LuckOverlay decade = CALC.decade(chart, k);
            int y = chart.getDecades().get(k).startYear() + random.nextInt(10);
            LuckOverlay year = CALC.year(chart, y);
            LuckOverlay month = CALC.month(chart, y, 1 + random.nextInt(12));
            int dl = LuckScore.decadeLuck(chart, decade);
            int yl = LuckScore.yearLuck(chart, decade, year);
            int ml = LuckScore.monthLuck(chart, decade, year, month);
            Check.isTrue("好運指數範圍 " + d, dl >= -1 && dl <= 101 && yl >= -1 && yl <= 101 && ml >= -1 && ml <= 101,
                    dl + "/" + yl + "/" + ml);
            Check.equal("流年＝大限60%＋流年40% " + d,
                    dl * 60 / 100 + LuckScore.yearLuckBase(chart, decade, year) * 40 / 100, yl);
            Check.equal("流月＝流月70%＋流年30% " + d,
                    LuckScore.monthLuckBase(chart, decade, year, month) * 70 / 100 + yl * 30 / 100, ml);
        }

        // 評語（lifeup getLuckyDesc 的分級）
        Object[][] descriptions = {
                {101, "天選之人，你將得到老田爺的眷顧"}, {100, "天選之人，你將得到老田爺的眷顧"},
                {99, "強運之人，一帆風順處處遇好事"}, {80, "強運之人，一帆風順處處遇好事"},
                {79, "你稱得上是幸運之人"}, {60, "你稱得上是幸運之人"}, {59, "好事壞事參半"}, {40, "好事壞事參半"},
                {39, "運氣不佳，靜待天時"}, {20, "運氣不佳，靜待天時"}, {19, "你完全沒有運氣，請靠自己努力"},
                {0, "你完全沒有運氣，請靠自己努力"}, {-1, "你真的應該衰到爆了，願老天保佑你"}
        };
        for (Object[] d : descriptions) {
            Check.equal("評語 " + d[0], d[1], LuckScore.describe((int) d[0]));
        }
    }
}
