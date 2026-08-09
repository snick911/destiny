package com.destiny.bazi.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 子平八字排盤核心邏輯。
 *
 * <p>年柱：以「立春」為界（而非西曆 1 月 1 日或農曆春節）。</p>
 * <p>月柱：以十二個「節」（非「氣」）劃分月份交界，月支固定為
 * 寅卯辰巳午未申酉戌亥子丑，月干則依年干以「五虎遁」推算。</p>
 * <p>日柱：以儒略日數（Julian Day Number）對 60 取餘推算干支，
 * 並依「晚子時」慣例，23:00~23:59 出生者日柱歸入次日。</p>
 * <p>時柱：時支依出生時刻對應十二時辰，時干依日干以「五鼠遁」推算。</p>
 */
public final class BaziCalculator {

    // 節氣索引 -> 月支索引（寅=2 為正月起點）
    private static final Map<Integer, Integer> JIE_TO_MONTH_BRANCH = new LinkedHashMap<>();

    static {
        JIE_TO_MONTH_BRANCH.put(SolarTerm.LICHUN, 2);     // 立春 -> 寅月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.JINGZHE, 3);    // 驚蟄 -> 卯月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.QINGMING, 4);   // 清明 -> 辰月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.LIXIA, 5);      // 立夏 -> 巳月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.MANGZHONG, 6);  // 芒種 -> 午月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.XIAOSHU, 7);    // 小暑 -> 未月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.LIQIU, 8);      // 立秋 -> 申月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.BAILU, 9);      // 白露 -> 酉月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.HANLU, 10);     // 寒露 -> 戌月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.LIDONG, 11);    // 立冬 -> 亥月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.DAXUE, 0);      // 大雪 -> 子月
        JIE_TO_MONTH_BRANCH.put(SolarTerm.XIAOHAN, 1);    // 小寒 -> 丑月
    }

    public BaziResult calculate(int year, int month, int day, int hour, int minute) {
        LocalDateTime birth = LocalDateTime.of(year, month, day, hour, minute);

        // ---- 年柱：以立春為界 ----
        LocalDateTime lichunThisYear = SolarTerm.getTime(year, SolarTerm.LICHUN);
        int effectiveYear = birth.isBefore(lichunThisYear) ? year - 1 : year;
        int yearStemIdx = GanZhi.mod(effectiveYear - 4, 10);
        int yearBranchIdx = GanZhi.mod(effectiveYear - 4, 12);
        Pillar yearPillar = new Pillar(yearStemIdx, yearBranchIdx);

        // ---- 月柱：尋找出生時刻所屬的節氣區間 ----
        int monthBranchIdx = findMonthBranch(birth);
        int yinMonthStemIdx = GanZhi.mod((yearStemIdx % 5) * 2 + 2, 10);
        int stepsFromYin = GanZhi.mod(monthBranchIdx - 2, 12);
        int monthStemIdx = GanZhi.mod(yinMonthStemIdx + stepsFromYin, 10);
        Pillar monthPillar = new Pillar(monthStemIdx, monthBranchIdx);

        // ---- 日柱：儒略日數對 60 取餘；23 時後歸入次日（晚子時） ----
        boolean earlyZiShift = hour == 23;
        LocalDate dayForPillar = birth.toLocalDate();
        if (earlyZiShift) {
            dayForPillar = dayForPillar.plusDays(1);
        }
        long jdn = toJulianDayNumber(dayForPillar);
        int dayStemIdx = GanZhi.mod(jdn + 9, 10);
        int dayBranchIdx = GanZhi.mod(jdn + 1, 12);
        Pillar dayPillar = new Pillar(dayStemIdx, dayBranchIdx);

        // ---- 時柱：時支依時刻，時干依日干「五鼠遁」 ----
        int hourBranchIdx = ((hour + 1) / 2) % 12;
        int ziHourStemIdx = GanZhi.mod((dayStemIdx % 5) * 2, 10);
        int hourStemIdx = GanZhi.mod(ziHourStemIdx + hourBranchIdx, 10);
        Pillar hourPillar = new Pillar(hourStemIdx, hourBranchIdx);

        return new BaziResult(yearPillar, monthPillar, dayPillar, hourPillar, effectiveYear, earlyZiShift);
    }

    /** 找出出生時刻所在的「節」區間，回傳對應的月支索引。 */
    private int findMonthBranch(LocalDateTime birth) {
        int y = birth.getYear();
        LocalDateTime best = null;
        int bestJie = SolarTerm.XIAOHAN;

        for (int yy = y - 1; yy <= y + 1; yy++) {
            for (int jie : JIE_TO_MONTH_BRANCH.keySet()) {
                LocalDateTime t = SolarTerm.getTime(yy, jie);
                if (!t.isAfter(birth) && (best == null || t.isAfter(best))) {
                    best = t;
                    bestJie = jie;
                }
            }
        }
        return JIE_TO_MONTH_BRANCH.get(bestJie);
    }

    /**
     * 將西曆日期轉換為儒略日數（Julian Day Number，整數，Fliegel &amp; Van Flandern 演算法）。
     */
    private static long toJulianDayNumber(LocalDate date) {
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        int a = (m - 14) / 12;
        long jdn = (1461L * (y + 4800 + a)) / 4
                + (367L * (m - 2 - 12 * a)) / 12
                - (3L * ((y + 4900 + a) / 100)) / 4
                + d - 32075;
        return jdn;
    }
}
