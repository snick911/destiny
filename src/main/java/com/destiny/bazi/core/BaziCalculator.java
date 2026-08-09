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

    private static final int GRAVE_YUQI_DAYS = 5;
    private static final int GRAVE_ZHONGQI_DAYS = 9;

    /** 節氣命中結果：節氣索引與其發生時刻。 */
    private record JieHit(int jieIndex, LocalDateTime time) {
    }

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
        JieHit monthJieHit = findCurrentJieHit(birth);
        int monthBranchIdx = JIE_TO_MONTH_BRANCH.get(monthJieHit.jieIndex());
        int yinMonthStemIdx = GanZhi.mod((yearStemIdx % 5) * 2 + 2, 10);
        int stepsFromYin = GanZhi.mod(monthBranchIdx - 2, 12);
        int monthStemIdx = GanZhi.mod(yinMonthStemIdx + stepsFromYin, 10);
        Pillar monthPillar = new Pillar(monthStemIdx, monthBranchIdx);

        // ---- 月令：辰戌丑未依入節天數分餘氣／中氣／本氣，其餘地支直接以主氣為月令 ----
        MonthOrder monthOrder = resolveMonthOrder(birth, monthJieHit.time(), monthBranchIdx);

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
                yearTenGods, monthTenGods, dayTenGods, hourTenGods, monthOrder);
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

    // 五行順序（用於計算旺相休囚死的相生相剋位移）：木生火生土生金生水生木。
    private static final String[] ELEMENT_ORDER = {"木", "火", "土", "金", "水"};

    // 依與月令五行的位移（0~4）對應：0=當我者旺、1=我生者相、2=我剋者死、3=剋我者囚、4=生我者休。
    private static final double[] SEASON_STATE_SCORE = {2.0, 1.5, 0.0, 0.5, 1.0};

    /**
     * 五行質量：先以原始命盤（四柱天干權重 1、地支藏干依比例分配權重，
     * 總和為 1）算出五行基本分，再依月令旺相休囚死為每個五行各加一次
     * 月令加分，兩者相加為五行總分。
     */
    public FiveElementMass calculateFiveElementMass(BaziResult result) {
        return calculateFiveElementMass(result, List.of());
    }

    /**
     * 五行質量（含大運／流年）：在原始命盤四柱之外，另將目前所選大運柱與
     * 流年柱的天干、地支藏干一併計入基本分，月令加分仍以命盤月令為準、
     * 每個五行各加一次。
     */
    public FiveElementMass calculateFiveElementMassWithLuck(BaziResult result, Pillar daYunPillar,
                                                              Pillar liuNianPillar) {
        return calculateFiveElementMass(result, List.of(daYunPillar, liuNianPillar));
    }

    private FiveElementMass calculateFiveElementMass(BaziResult result, List<Pillar> extraPillars) {
        Map<String, Double> mass = new LinkedHashMap<>();
        for (String element : ELEMENT_ORDER) {
            mass.put(element, 0.0);
        }

        // ---- 基本分：命盤四柱（及額外指定的大運／流年柱）天干與地支藏干的原始五行分量 ----
        List<Pillar> pillars = new ArrayList<>(List.of(
                result.getYearPillar(), result.getMonthPillar(), result.getDayPillar(), result.getHourPillar()));
        pillars.addAll(extraPillars);

        for (Pillar pillar : pillars) {
            mass.merge(pillar.getStemElement(), 1.0, Double::sum);

            int[] hidden = HiddenStems.of(pillar.getBranchIndex());
            double[] ratios = hiddenStemRatios(hidden.length);
            for (int i = 0; i < hidden.length; i++) {
                mass.merge(GanZhi.STEM_ELEMENTS[hidden[i]], ratios[i], Double::sum);
            }
        }

        // ---- 月令加分：依旺相休囚死，各五行各加一次月令加成 ----
        int seasonIdx = elementIndex(result.getMonthOrder().getElement());
        for (int i = 0; i < ELEMENT_ORDER.length; i++) {
            int diff = GanZhi.mod(i - seasonIdx, 5);
            mass.merge(ELEMENT_ORDER[i], SEASON_STATE_SCORE[diff], Double::sum);
        }

        return new FiveElementMass(mass);
    }

    /** 十神統計：命盤四柱天干（日主本身不計）與地支藏干的十神出現次數。 */
    public Map<String, Integer> calculateTenGodCounts(BaziResult result) {
        return calculateTenGodCounts(result, List.of());
    }

    /** 十神統計（含大運／流年）：在命盤四柱之外，另計入所選大運柱與流年柱的十神。 */
    public Map<String, Integer> calculateTenGodCountsWithLuck(BaziResult result, PillarTenGods daYunTenGods,
                                                                PillarTenGods liuNianTenGods) {
        return calculateTenGodCounts(result, List.of(daYunTenGods, liuNianTenGods));
    }

    private Map<String, Integer> calculateTenGodCounts(BaziResult result, List<PillarTenGods> extra) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String god : TenGod.ALL) {
            counts.put(god, 0);
        }

        List<PillarTenGods> all = new ArrayList<>(List.of(
                result.getYearTenGods(), result.getMonthTenGods(), result.getDayTenGods(), result.getHourTenGods()));
        all.addAll(extra);

        for (PillarTenGods pillarTenGods : all) {
            if (!pillarTenGods.getStemTenGod().equals(TenGod.RI_ZHU)) {
                counts.merge(pillarTenGods.getStemTenGod(), 1, Integer::sum);
            }
            for (HiddenStemGod hiddenGod : pillarTenGods.getBranchHiddenGods()) {
                counts.merge(hiddenGod.getTenGod(), 1, Integer::sum);
            }
        }

        return counts;
    }

    /** 地支藏干依藏干數的權重分配：單藏 1；雙藏 2/3、1/3；三藏 2/4、1/4、1/4。 */
    private double[] hiddenStemRatios(int count) {
        return switch (count) {
            case 1 -> new double[]{1.0};
            case 2 -> new double[]{2.0 / 3.0, 1.0 / 3.0};
            case 3 -> new double[]{2.0 / 4.0, 1.0 / 4.0, 1.0 / 4.0};
            default -> throw new IllegalStateException("未預期的藏干數量: " + count);
        };
    }

    private int elementIndex(String element) {
        for (int i = 0; i < ELEMENT_ORDER.length; i++) {
            if (ELEMENT_ORDER[i].equals(element)) {
                return i;
            }
        }
        throw new IllegalArgumentException("未知的五行: " + element);
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

    /** 找出出生時刻所在的「節」區間（節氣索引與交節時刻）。 */
    private JieHit findCurrentJieHit(LocalDateTime birth) {
        int y = birth.getYear();
        JieHit best = null;

        for (int yy = y - 1; yy <= y + 1; yy++) {
            for (int jie : JIE_TO_MONTH_BRANCH.keySet()) {
                LocalDateTime t = SolarTerm.getTime(yy, jie);
                if (!t.isAfter(birth) && (best == null || t.isAfter(best.time()))) {
                    best = new JieHit(jie, t);
                }
            }
        }
        return best;
    }

    /**
     * 月令判定：辰戌丑未（四墓庫月）依入節天數分三階段——前 5 天為上個節氣
     * 殘留的餘氣當令，中間 9 天為該支所藏墓庫之氣（中氣）當令，之後才是
     * 純粹的本氣（戊／己土）當令；其餘地支不分階段，直接以該支本氣為月令。
     */
    private MonthOrder resolveMonthOrder(LocalDateTime birth, LocalDateTime monthStart, int monthBranchIdx) {
        double daysIntoMonth = Duration.between(monthStart, birth).toMinutes() / 1440.0;
        long dayOrdinal = (long) Math.floor(daysIntoMonth) + 1;

        int[] gravePhases = gravePhaseStems(monthBranchIdx);
        int stemIdx;
        String phase;
        if (gravePhases != null) {
            if (daysIntoMonth < GRAVE_YUQI_DAYS) {
                stemIdx = gravePhases[0];
                phase = "餘氣";
            } else if (daysIntoMonth < GRAVE_YUQI_DAYS + GRAVE_ZHONGQI_DAYS) {
                stemIdx = gravePhases[1];
                phase = "中氣";
            } else {
                stemIdx = gravePhases[2];
                phase = "本氣";
            }
        } else {
            stemIdx = HiddenStems.of(monthBranchIdx)[0];
            phase = "主氣";
        }
        return new MonthOrder(GanZhi.STEMS[stemIdx], GanZhi.STEM_ELEMENTS[stemIdx], phase, dayOrdinal);
    }

    /** 辰戌丑未（四墓庫月）餘氣／中氣／本氣天干索引；其餘地支回傳 null。 */
    private int[] gravePhaseStems(int branchIdx) {
        return switch (branchIdx) {
            case 4 -> new int[]{1, 9, 4};   // 辰：乙(餘氣) 癸(中氣，水庫) 戊(本氣)
            case 7 -> new int[]{3, 1, 5};   // 未：丁(餘氣) 乙(中氣，木庫) 己(本氣)
            case 10 -> new int[]{7, 3, 4};  // 戌：辛(餘氣) 丁(中氣，火庫) 戊(本氣)
            case 1 -> new int[]{9, 7, 5};   // 丑：癸(餘氣) 辛(中氣，金庫) 己(本氣)
            default -> null;
        };
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
