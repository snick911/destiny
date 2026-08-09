package com.destiny.bazi.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 二十四節氣時刻推算。
 *
 * <p>以太陽視黃經為準：節氣即太陽視黃經每 15° 的整數倍時刻（春分＝0°，
 * 依序遞增）。先以「回歸年平均法」（見 {@link #estimateTime}）粗略估計時刻，
 * 再以低精度太陽座標公式（Meeus《Astronomical Algorithms》第 25 章，精度約
 * 0.01°，換算約數十秒至一分鐘量級）搭配牛頓法疊代求解精確交角時刻，較單純的
 * 回歸年平均法（未計入地球橢圓軌道造成的近日點/遠日點速度變化）更為準確。</p>
 *
 * <p>節氣索引 0~23 依序為：
 * 0 小寒 1 大寒 2 立春 3 雨水 4 驚蟄 5 春分 6 清明 7 穀雨
 * 8 立夏 9 小滿 10 芒種 11 夏至 12 小暑 13 大暑 14 立秋 15 處暑
 * 16 白露 17 秋分 18 寒露 19 霜降 20 立冬 21 小雪 22 大雪 23 冬至</p>
 *
 * <p>所有時刻皆以北京時間（UTC+8）表示，與本程式其餘部分的慣例一致。</p>
 */
public final class SolarTerm {

    public static final String[] NAMES = {
            "小寒", "大寒", "立春", "雨水", "驚蟄", "春分", "清明", "穀雨",
            "立夏", "小滿", "芒種", "夏至", "小暑", "大暑", "立秋", "處暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    // 粗估用：節氣相對於基準年"小寒"的分鐘偏移量（回歸年平均法，僅作為疊代法的起始猜測值）。
    private static final long[] TERM_OFFSETS = {
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921,
            173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
            353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    };

    private static final double MINUTES_PER_YEAR = 525948.76;

    /** 粗估用基準時刻：西元 1900 年 1 月 6 日 02:05（北京時間）。 */
    private static final LocalDateTime ESTIMATE_BASE = LocalDateTime.of(1900, 1, 6, 2, 5, 0);

    // 儒略日數換算用（與 BaziCalculator 相同的 Fliegel & Van Flandern 演算法）。
    private static final long JDN_EPOCH_OFFSET = toJulianDayNumber(LocalDate.of(1970, 1, 1));

    private static final double DEGREES_PER_DAY = 360.0 / 365.2422;

    public static final int LICHUN = 2;   // 立春：年柱交界
    public static final int JINGZHE = 4;  // 驚蟄
    public static final int QINGMING = 6; // 清明
    public static final int LIXIA = 8;    // 立夏
    public static final int MANGZHONG = 10; // 芒種
    public static final int XIAOSHU = 12; // 小暑
    public static final int LIQIU = 14;   // 立秋
    public static final int BAILU = 16;   // 白露
    public static final int HANLU = 18;   // 寒露
    public static final int LIDONG = 20;  // 立冬
    public static final int DAXUE = 22;   // 大雪
    public static final int XIAOHAN = 0;  // 小寒

    private SolarTerm() {
    }

    /**
     * 計算指定西元年份中，某節氣（0~23）發生的精確時刻（北京時間）。
     */
    public static LocalDateTime getTime(int year, int termIndex) {
        LocalDateTime estimate = estimateTime(year, termIndex);
        double targetLongitude = GanZhi.mod((termIndex - 5) * 15, 360);
        return solveForLongitude(estimate, targetLongitude);
    }

    /** 回歸年平均法的粗略估計，作為牛頓法疊代求解的初始猜測值。 */
    private static LocalDateTime estimateTime(int year, int termIndex) {
        long offsetMinutes = Math.round(MINUTES_PER_YEAR * (year - 1900)) + TERM_OFFSETS[termIndex];
        return ESTIMATE_BASE.plusMinutes(offsetMinutes);
    }

    /** 以牛頓法疊代求解太陽視黃經等於 targetLongitude（0~360°）的時刻。 */
    private static LocalDateTime solveForLongitude(LocalDateTime estimate, double targetLongitude) {
        double jd = toJulianDate(estimate);
        for (int i = 0; i < 8; i++) {
            double longitude = sunApparentLongitudeDeg(jd);
            double diff = normalizeSignedDegrees(targetLongitude - longitude);
            if (Math.abs(diff) < 1e-6) {
                break;
            }
            jd += diff / DEGREES_PER_DAY;
        }
        return fromJulianDate(jd);
    }

    /**
     * 太陽視黃經（degrees, 0~360），採用 Meeus《Astronomical Algorithms》
     * 第 25 章之低精度公式（精度約 0.01°），jd 以世界時（UT）為準。
     */
    private static double sunApparentLongitudeDeg(double jd) {
        double t = (jd - 2451545.0) / 36525.0; // J2000.0 起算的儒略世紀數

        double l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t; // 太陽平黃經
        double m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t;  // 太陽平近點角
        double mRad = Math.toRadians(normalizeDegrees(m));

        double c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(mRad)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * mRad)
                + 0.000289 * Math.sin(3 * mRad);

        double trueLongitude = l0 + c;
        double omega = 125.04 - 1934.136 * t;
        double apparentLongitude = trueLongitude - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));
        return normalizeDegrees(apparentLongitude);
    }

    private static double normalizeDegrees(double deg) {
        double d = deg % 360.0;
        return d < 0 ? d + 360.0 : d;
    }

    /** 將角度差正規化到 (-180, 180]，避免跨 0°/360° 邊界時牛頓法跳錯方向。 */
    private static double normalizeSignedDegrees(double deg) {
        double d = ((deg + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return d;
    }

    /** 將北京時間轉換為世界時（UT）儒略日數（含時分秒）。 */
    private static double toJulianDate(LocalDateTime beijingTime) {
        LocalDateTime utc = beijingTime.minusHours(8);
        long jdn = toJulianDayNumber(utc.toLocalDate());
        double dayFraction = utc.toLocalTime().toSecondOfDay() / 86400.0;
        return jdn - 0.5 + dayFraction;
    }

    /** 將世界時（UT）儒略日數轉換回北京時間。 */
    private static LocalDateTime fromJulianDate(double jd) {
        double shifted = jd + 0.5;
        long jdn = (long) Math.floor(shifted);
        double dayFraction = shifted - jdn;
        long totalSeconds = Math.round(dayFraction * 86400.0);
        if (totalSeconds >= 86400) {
            totalSeconds = 86399;
        }
        LocalDate date = LocalDate.ofEpochDay(jdn - JDN_EPOCH_OFFSET);
        LocalDateTime utc = LocalDateTime.of(date, LocalTime.ofSecondOfDay(totalSeconds));
        return utc.plusHours(8);
    }

    /**
     * 將西曆日期轉換為儒略日數（Julian Day Number，整數，Fliegel &amp; Van Flandern 演算法）。
     */
    private static long toJulianDayNumber(LocalDate date) {
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        int a = (m - 14) / 12;
        return (1461L * (y + 4800 + a)) / 4
                + (367L * (m - 2 - 12 * a)) / 12
                - (3L * ((y + 4900 + a) / 100)) / 4
                + d - 32075;
    }
}
