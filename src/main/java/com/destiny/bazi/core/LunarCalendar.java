package com.destiny.bazi.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 農曆（夏曆）換算，以天文計算推得，不依賴查表。
 *
 * <p>編算規則依 GB/T 33661-2017《農曆的編算和頒行》：</p>
 * <ul>
 *   <li>以北京時間（UTC+8）為準，日月合朔所在日為農曆每月初一。</li>
 *   <li>含「冬至」的月為十一月。</li>
 *   <li>自某年十一月起到次年十一月（不含）之間若有十三個月，其中第一個不含
 *       中氣的月為閏月，月序與前一個月相同。</li>
 * </ul>
 *
 * <p>朔的時刻採 Meeus《Astronomical Algorithms》第 49 章（平朔加上週期項與行星
 * 攝動修正，誤差約數十秒），以 ΔT 由地球時換算為世界時；中氣（大寒、雨水、
 * 春分…冬至，太陽視黃經 30° 的整數倍）時刻沿用 {@link SolarTerm}。</p>
 *
 * <p>民國初年（1912~1928 年）的曆書以北京地方平時（東經 116°25′，UTC+7:45:40）
 * 編算，1929 年起才改用東經 120° 標準時，因此這段期間的朔與中氣以地方平時判定
 * 所在日期（1914、1916、1920 年各有一個朔落在 UTC+8 的午夜後數分鐘，依地方平時
 * 則屬前一天，與當年曆書一致）。</p>
 *
 * <p>另有 3 個朔（2057-09-28、2089-09-04、2097-08-07）推算時刻距午夜不到 3 分鐘，
 * 已超出本程式朔時刻與 ΔT 外推的精度，改依香港天文台公布的曆表歸入次日。
 * 1900~2100 年逐日換算結果與該曆表完全一致。</p>
 */
public final class LunarCalendar {

    public static final String[] MONTH_NAMES = {
            "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"
    };

    private static final String[] DAY_DIGITS = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    /** Meeus 第 49 章：k = 0 為 2000 年 1 月 6 日的朔。 */
    private static final double NEW_MOON_EPOCH_JDE = 2451550.09766;
    private static final double SYNODIC_MONTH = 29.530588861;

    /** 農曆日期：年（正月所在的西元年）、月（1~12）、是否閏月、日（1~30）。 */
    public record LunarDate(int year, int month, boolean leap, int day) {

        /** 年干索引（甲=0）：農曆年以正月初一為界。 */
        public int yearStemIndex() {
            return GanZhi.mod(year - 4, 10);
        }

        /** 年支索引（子=0）：農曆年以正月初一為界。 */
        public int yearBranchIndex() {
            return GanZhi.mod(year - 4, 12);
        }

        public String yearGanZhi() {
            return GanZhi.STEMS[yearStemIndex()] + GanZhi.BRANCHES[yearBranchIndex()];
        }

        /** 例如「閏四月」「十二月」。 */
        public String monthName() {
            return (leap ? "閏" : "") + MONTH_NAMES[month - 1];
        }

        /** 例如「初一」「十五」「廿三」「三十」。 */
        public String dayName() {
            return LunarCalendar.dayName(day);
        }

        /** 例如「甲子年閏四月十五」。 */
        public String toChineseString() {
            return yearGanZhi() + "年" + monthName() + dayName();
        }
    }

    /** 一個農曆月：初一的日期、月序、是否閏月、所屬農曆年。 */
    private record Month(LocalDate start, int number, boolean leap, int lunarYear) {
    }

    /**
     * 一個「歲」：自前一年十一月（含冬至的月）起，到當年十一月之前的 12 或 13 個月，
     * end 為下一個十一月的初一。
     */
    private record Cycle(List<Month> months, LocalDate end) {
    }

    /** 1912~1928 年曆書採用的北京地方平時與 UTC+8 的時差（14 分 20 秒）。 */
    private static final long LOCAL_MEAN_TIME_LAG_SECONDS = 14 * 60 + 20;
    private static final int LOCAL_MEAN_TIME_FIRST_YEAR = 1912;
    private static final int STANDARD_TIME_ADOPTED_YEAR = 1929;

    /** 推算時刻緊貼午夜、依公布曆表歸入次日的朔（北京時間的推算日期）。 */
    private static final Set<LocalDate> NEW_MOONS_MOVED_TO_NEXT_DAY = Set.of(
            LocalDate.of(2057, 9, 28), LocalDate.of(2089, 9, 4), LocalDate.of(2097, 8, 7));

    /** 以「歲」中正月所在的西元年為鍵的快取。 */
    private static final Map<Integer, Cycle> CYCLES = new ConcurrentHashMap<>();

    private LunarCalendar() {
    }

    /** 將西曆日期（北京時間）換算為農曆日期。 */
    public static LunarDate fromSolar(LocalDate date) {
        Cycle cycle = cycle(date.getYear() + 1);
        if (date.isBefore(cycle.months().get(0).start())) {
            cycle = cycle(date.getYear());
        }
        List<Month> months = cycle.months();
        for (int i = months.size() - 1; i >= 0; i--) {
            Month m = months.get(i);
            if (!date.isBefore(m.start())) {
                int day = (int) ChronoUnit.DAYS.between(m.start(), date) + 1;
                return new LunarDate(m.lunarYear(), m.number(), m.leap(), day);
            }
        }
        throw new IllegalStateException("找不到 " + date + " 所屬的農曆月");
    }

    /** 將農曆日期換算為西曆日期；月或日不存在（例如當年沒有該閏月、小月三十）時拋出例外。 */
    public static LocalDate toSolar(int lunarYear, int month, boolean leap, int day) {
        for (int key : new int[]{lunarYear, lunarYear + 1}) {
            Cycle cycle = cycle(key);
            List<Month> months = cycle.months();
            for (int i = 0; i < months.size(); i++) {
                Month m = months.get(i);
                if (m.lunarYear() == lunarYear && m.number() == month && m.leap() == leap) {
                    LocalDate next = i + 1 < months.size() ? months.get(i + 1).start() : cycle.end();
                    int length = (int) ChronoUnit.DAYS.between(m.start(), next);
                    if (day < 1 || day > length) {
                        throw new IllegalArgumentException(
                                lunarYear + "年" + (leap ? "閏" : "") + MONTH_NAMES[month - 1] + "只有 " + length + " 天");
                    }
                    return m.start().plusDays(day - 1);
                }
            }
        }
        throw new IllegalArgumentException(lunarYear + "年沒有" + (leap ? "閏" : "") + MONTH_NAMES[month - 1]);
    }

    /** 該農曆年的閏月月序（1~12），沒有閏月時為 0。 */
    public static int leapMonth(int lunarYear) {
        for (int key : new int[]{lunarYear, lunarYear + 1}) {
            for (Month m : cycle(key).months()) {
                if (m.leap() && m.lunarYear() == lunarYear) {
                    return m.number();
                }
            }
        }
        return 0;
    }

    /** 農曆日的中文寫法：初一~初十、十一~十九、二十、廿一~廿九、三十。 */
    public static String dayName(int day) {
        if (day <= 10) {
            return "初" + DAY_DIGITS[day - 1];
        }
        if (day < 20) {
            return "十" + DAY_DIGITS[day - 11];
        }
        if (day == 20) {
            return "二十";
        }
        if (day < 30) {
            return "廿" + DAY_DIGITS[day - 21];
        }
        return "三十";
    }

    // ------------------------------------------------------------ 編排一「歲」的月份

    private static Cycle cycle(int lunarYear) {
        return CYCLES.computeIfAbsent(lunarYear, LunarCalendar::buildCycle);
    }

    private static Cycle buildCycle(int lunarYear) {
        LocalDate winterSolstice1 = calendarDate(SolarTerm.getTime(lunarYear - 1, 23));
        LocalDate winterSolstice2 = calendarDate(SolarTerm.getTime(lunarYear, 23));
        long k1 = newMoonIndexOnOrBefore(winterSolstice1);
        long k2 = newMoonIndexOnOrBefore(winterSolstice2);
        int count = (int) (k2 - k1);

        List<LocalDate> starts = new ArrayList<>();
        for (long k = k1; k <= k2; k++) {
            starts.add(newMoonDate(k));
        }

        int leapIndex = -1;
        if (count == 13) {
            List<LocalDate> principalTerms = principalTermDates(lunarYear);
            for (int i = 1; i < count; i++) {
                if (!containsAny(principalTerms, starts.get(i), starts.get(i + 1))) {
                    leapIndex = i;
                    break;
                }
            }
        }

        List<Month> months = new ArrayList<>();
        int number = 11;
        int year = lunarYear - 1;
        for (int i = 0; i < count; i++) {
            boolean leap = i == leapIndex;
            if (i > 0 && !leap) {
                number = number % 12 + 1;
                if (number == 1) {
                    year = lunarYear;
                }
            }
            months.add(new Month(starts.get(i), number, leap, year));
        }
        return new Cycle(List.copyOf(months), starts.get(count));
    }

    /** 前一年小雪起到當年冬至的所有中氣日期（涵蓋整個「歲」）。 */
    private static List<LocalDate> principalTermDates(int lunarYear) {
        List<LocalDate> dates = new ArrayList<>();
        for (int term : new int[]{19, 21, 23}) {
            dates.add(calendarDate(SolarTerm.getTime(lunarYear - 1, term)));
        }
        for (int term = 1; term < 24; term += 2) {
            dates.add(calendarDate(SolarTerm.getTime(lunarYear, term)));
        }
        return dates;
    }

    private static boolean containsAny(List<LocalDate> dates, LocalDate start, LocalDate endExclusive) {
        for (LocalDate d : dates) {
            if (!d.isBefore(start) && d.isBefore(endExclusive)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ 朔（新月）時刻

    /** 合朔時刻落在 date 當天或之前的最後一個朔的序號 k。 */
    private static long newMoonIndexOnOrBefore(LocalDate date) {
        double jd = SolarTerm.toJulianDate(date.atTime(12, 0));
        long k = (long) Math.floor((jd - NEW_MOON_EPOCH_JDE) / SYNODIC_MONTH);
        while (newMoonDate(k).isAfter(date)) {
            k--;
        }
        while (!newMoonDate(k + 1).isAfter(date)) {
            k++;
        }
        return k;
    }

    private static LocalDate newMoonDate(long k) {
        LocalDate date = calendarDate(newMoonTime(k));
        return NEW_MOONS_MOVED_TO_NEXT_DAY.contains(date) ? date.plusDays(1) : date;
    }

    /** 天象時刻（北京時間）在曆書上所屬的日期：1912~1928 年改以北京地方平時判定。 */
    private static LocalDate calendarDate(LocalDateTime beijingTime) {
        int year = beijingTime.getYear();
        if (year >= LOCAL_MEAN_TIME_FIRST_YEAR && year < STANDARD_TIME_ADOPTED_YEAR) {
            return beijingTime.minusSeconds(LOCAL_MEAN_TIME_LAG_SECONDS).toLocalDate();
        }
        return beijingTime.toLocalDate();
    }

    /** 第 k 個朔（k = 0 約為 2000 年 1 月 6 日）的北京時間。 */
    static LocalDateTime newMoonTime(long k) {
        double jde = newMoonJde(k);
        double jdUt = jde - SolarTerm.deltaTSeconds(jde) / 86400.0;
        return SolarTerm.fromJulianDate(jdUt);
    }

    /** Meeus 第 49 章：朔的儒略日（地球時 TT）。 */
    private static double newMoonJde(long k) {
        double t = k / 1236.85;
        double t2 = t * t;
        double t3 = t2 * t;
        double t4 = t3 * t;

        double jde = NEW_MOON_EPOCH_JDE + SYNODIC_MONTH * k
                + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4;

        double e = 1 - 0.002516 * t - 0.0000074 * t2;
        double m = Math.toRadians(2.5534 + 29.10535670 * k - 0.0000014 * t2 - 0.00000011 * t3);
        double mp = Math.toRadians(201.5643 + 385.81693528 * k + 0.0107582 * t2 + 0.00001238 * t3
                - 0.000000058 * t4);
        double f = Math.toRadians(160.7108 + 390.67050284 * k - 0.0016118 * t2 - 0.00000227 * t3
                + 0.000000011 * t4);
        double omega = Math.toRadians(124.7746 - 1.56375588 * k + 0.0020672 * t2 + 0.00000215 * t3);

        double correction = -0.40720 * Math.sin(mp)
                + 0.17241 * e * Math.sin(m)
                + 0.01608 * Math.sin(2 * mp)
                + 0.01039 * Math.sin(2 * f)
                + 0.00739 * e * Math.sin(mp - m)
                - 0.00514 * e * Math.sin(mp + m)
                + 0.00208 * e * e * Math.sin(2 * m)
                - 0.00111 * Math.sin(mp - 2 * f)
                - 0.00057 * Math.sin(mp + 2 * f)
                + 0.00056 * e * Math.sin(2 * mp + m)
                - 0.00042 * Math.sin(3 * mp)
                + 0.00042 * e * Math.sin(m + 2 * f)
                + 0.00038 * e * Math.sin(m - 2 * f)
                - 0.00024 * e * Math.sin(2 * mp - m)
                - 0.00017 * Math.sin(omega)
                - 0.00007 * Math.sin(mp + 2 * m)
                + 0.00004 * Math.sin(2 * mp - 2 * f)
                + 0.00004 * Math.sin(3 * m)
                + 0.00003 * Math.sin(mp + m - 2 * f)
                + 0.00003 * Math.sin(2 * mp + 2 * f)
                - 0.00003 * Math.sin(mp + m + 2 * f)
                + 0.00003 * Math.sin(mp - m + 2 * f)
                - 0.00002 * Math.sin(mp - m - 2 * f)
                - 0.00002 * Math.sin(3 * mp + m)
                + 0.00002 * Math.sin(4 * mp);

        // 行星攝動修正（A1~A14）
        double[][] planetary = {
                {299.77, 0.107408, 0.000325}, {251.88, 0.016321, 0.000165}, {251.83, 26.651886, 0.000164},
                {349.42, 36.412478, 0.000126}, {84.66, 18.206239, 0.000110}, {141.74, 53.303771, 0.000062},
                {207.14, 2.453732, 0.000060}, {154.84, 7.306860, 0.000056}, {34.52, 27.261239, 0.000047},
                {207.19, 0.121824, 0.000042}, {291.34, 1.844379, 0.000040}, {161.72, 24.198154, 0.000037},
                {239.56, 25.513099, 0.000035}, {331.55, 3.592518, 0.000023}
        };
        double additional = 0;
        for (int i = 0; i < planetary.length; i++) {
            double angle = planetary[i][0] + planetary[i][1] * k - (i == 0 ? 0.009173 * t2 : 0);
            additional += planetary[i][2] * Math.sin(Math.toRadians(angle));
        }
        return jde + correction + additional;
    }
}
