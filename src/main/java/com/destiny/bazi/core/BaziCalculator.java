package com.destiny.bazi.core;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    public static final int DAYUN_PERIOD_COUNT = 8;

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

        // ---- 十神：以日干為「日主」，推算四柱天干與地支藏干的十神 ----
        PillarTenGods yearTenGods = tenGodsFor(dayStemIdx, yearPillar, false);
        PillarTenGods monthTenGods = tenGodsFor(dayStemIdx, monthPillar, false);
        PillarTenGods dayTenGods = tenGodsFor(dayStemIdx, dayPillar, true);
        PillarTenGods hourTenGods = tenGodsFor(dayStemIdx, hourPillar, false);

        return new BaziResult(yearPillar, monthPillar, dayPillar, hourPillar, effectiveYear, earlyZiShift,
                yearTenGods, monthTenGods, dayTenGods, hourTenGods);
    }

    /**
     * 大運排盤：依「陽男陰女順排、陰男陽女逆排」判定方向，
     * 以出生時刻與最近節氣的距離換算起運歲數（三日折一歲），
     * 並從月柱起算，依方向逐期推算干支與十神。
     */
    public DaYunResult calculateDaYun(BaziResult baziResult, int year, int month, int day, int hour, int minute,
                                       Gender gender) {
        LocalDateTime birth = LocalDateTime.of(year, month, day, hour, minute);

        boolean yearStemYang = baziResult.getYearPillar().getStemIndex() % 2 == 0;
        boolean forward = (yearStemYang && gender == Gender.MALE) || (!yearStemYang && gender == Gender.FEMALE);

        LocalDateTime boundary = findAdjacentJieTime(birth, forward);
        long diffMinutes = Math.abs(Duration.between(birth, boundary).toMinutes());
        double diffDays = diffMinutes / 1440.0;
        double totalYearsExact = diffDays / 3.0;

        int startYears = (int) totalYearsExact;
        double monthsExact = (totalYearsExact - startYears) * 12;
        int startMonths = (int) monthsExact;
        int startDays = (int) Math.round((monthsExact - startMonths) * 30);
        if (startDays >= 30) {
            startDays -= 30;
            startMonths += 1;
        }
        if (startMonths >= 12) {
            startMonths -= 12;
            startYears += 1;
        }

        int direction = forward ? 1 : -1;
        int monthStemIdx = baziResult.getMonthPillar().getStemIndex();
        int monthBranchIdx = baziResult.getMonthPillar().getBranchIndex();
        int dayStemIdx = baziResult.getDayPillar().getStemIndex();

        List<DaYunPeriod> periods = new ArrayList<>();
        for (int k = 1; k <= DAYUN_PERIOD_COUNT; k++) {
            int stemIdx = GanZhi.mod(monthStemIdx + direction * k, 10);
            int branchIdx = GanZhi.mod(monthBranchIdx + direction * k, 12);
            Pillar pillar = new Pillar(stemIdx, branchIdx);
            int periodStartAge = startYears + (k - 1) * 10;
            PillarTenGods tenGods = tenGodsFor(dayStemIdx, pillar, false);
            periods.add(new DaYunPeriod(pillar, periodStartAge, tenGods));
        }

        return new DaYunResult(forward, startYears, startMonths, startDays, periods);
    }

    /**
     * 流年排盤：依指定西元年份直接推算該年的年柱干支與十神
     * （不再套用立春界線，因為呼叫端已明確指定所要查詢的流年）。
     */
    public LiuNianYear calculateLiuNian(int calendarYear, int dayStemIdx) {
        int stemIdx = GanZhi.mod(calendarYear - 4, 10);
        int branchIdx = GanZhi.mod(calendarYear - 4, 12);
        Pillar pillar = new Pillar(stemIdx, branchIdx);
        PillarTenGods tenGods = tenGodsFor(dayStemIdx, pillar, false);
        return new LiuNianYear(calendarYear, pillar, tenGods);
    }

    /**
     * 流月排盤：在指定流年內，依月支固定順序（寅卯辰巳午未申酉戌亥子丑，
     * 即正月~十二月）與該流年年干以「五虎遁」推算各月干支及十神。
     *
     * @param liuNianYear 流年西元年份
     * @param monthIndex  0=正月（寅）～11=十二月（丑）
     */
    public LiuYueMonth calculateLiuYue(int liuNianYear, int monthIndex, int dayStemIdx) {
        int yearStemIdx = GanZhi.mod(liuNianYear - 4, 10);
        int yinMonthStemIdx = GanZhi.mod((yearStemIdx % 5) * 2 + 2, 10);
        int branchIdx = GanZhi.mod(2 + monthIndex, 12);
        int stemIdx = GanZhi.mod(yinMonthStemIdx + monthIndex, 10);
        Pillar pillar = new Pillar(stemIdx, branchIdx);
        PillarTenGods tenGods = tenGodsFor(dayStemIdx, pillar, false);
        return new LiuYueMonth(liuNianYear, monthIndex, pillar, tenGods);
    }

    /** 找出出生時刻之前（after=false）或之後（after=true）最接近的節氣時刻。 */
    private LocalDateTime findAdjacentJieTime(LocalDateTime birth, boolean after) {
        int y = birth.getYear();
        LocalDateTime best = null;

        for (int yy = y - 1; yy <= y + 1; yy++) {
            for (int jie : JIE_TO_MONTH_BRANCH.keySet()) {
                LocalDateTime t = SolarTerm.getTime(yy, jie);
                if (after) {
                    if (t.isAfter(birth) && (best == null || t.isBefore(best))) {
                        best = t;
                    }
                } else {
                    if (!t.isAfter(birth) && (best == null || t.isAfter(best))) {
                        best = t;
                    }
                }
            }
        }
        return best;
    }

    /** 計算單一柱的天干十神（日柱本身標示為「日主」）與地支藏干十神。 */
    private PillarTenGods tenGodsFor(int dayStemIdx, Pillar pillar, boolean isDayPillar) {
        String stemTenGod = isDayPillar ? TenGod.RI_ZHU : TenGod.of(dayStemIdx, pillar.getStemIndex());

        int[] hidden = HiddenStems.of(pillar.getBranchIndex());
        List<HiddenStemGod> hiddenGods = new ArrayList<>();
        for (int i = 0; i < hidden.length; i++) {
            int stemIdx = hidden[i];
            hiddenGods.add(new HiddenStemGod(
                    GanZhi.STEMS[stemIdx],
                    TenGod.of(dayStemIdx, stemIdx),
                    i == 0));
        }
        return new PillarTenGods(stemTenGod, hiddenGods);
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
