package com.destiny.bazi.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 二十四節氣時刻推算。
 *
 * <p>以太陽視黃經為準：節氣即太陽視黃經每 15° 的整數倍時刻（春分＝0°，
 * 依序遞增）。先以「回歸年平均法」（見 {@link #estimateTime}）粗略估計時刻，
 * 再以 VSOP87 地球日心黃經截斷級數（Meeus《Astronomical Algorithms》第 32 章
 * 附錄係數）加上 FK5 修正、章動與光行差求得太陽視黃經，並以 ΔT（地球時與
 * 世界時之差，Espenak &amp; Meeus 多項式）換算時間尺度，搭配牛頓法疊代求解
 * 精確交角時刻。與香港天文台等公布值相比誤差約在 1 分鐘以內。</p>
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
        for (int i = 0; i < 10; i++) {
            double longitude = sunApparentLongitudeDeg(jd + deltaTSeconds(jd) / 86400.0);
            double diff = normalizeSignedDegrees(targetLongitude - longitude);
            if (Math.abs(diff) < 1e-7) {
                break;
            }
            jd += diff / DEGREES_PER_DAY;
        }
        return fromJulianDate(jd);
    }

    // VSOP87 地球日心黃經截斷級數（Meeus 附錄 III），每項為 {A, B, C}，項值 A·cos(B + C·τ)，單位 1e-8 弧度。
    private static final double[][] VSOP_L0 = {
            {175347046, 0, 0}, {3341656, 4.6692568, 6283.07585}, {34894, 4.6261, 12566.1517},
            {3497, 2.7441, 5753.3849}, {3418, 2.8289, 3.5231}, {3136, 3.6277, 77713.7715},
            {2676, 4.4181, 7860.4194}, {2343, 6.1352, 3930.2097}, {1324, 0.7425, 11506.7698},
            {1273, 2.0371, 529.691}, {1199, 1.1096, 1577.3435}, {990, 5.233, 5884.927},
            {902, 2.045, 26.298}, {857, 3.508, 398.149}, {780, 1.179, 5223.694},
            {753, 2.533, 5507.553}, {505, 4.583, 18849.228}, {492, 4.205, 775.523},
            {357, 2.92, 0.067}, {317, 5.849, 11790.629}, {284, 1.899, 796.298},
            {271, 0.315, 10977.079}, {243, 0.345, 5486.778}, {206, 4.806, 2544.314},
            {205, 1.869, 5573.143}, {202, 2.458, 6069.777}, {156, 0.833, 213.299},
            {132, 3.411, 2942.463}, {126, 1.083, 20.775}, {115, 0.645, 0.98},
            {103, 0.636, 4694.003}, {102, 0.976, 15720.839}, {102, 4.267, 7.114},
            {99, 6.21, 2146.17}, {98, 0.68, 155.42}, {86, 5.98, 161000.69},
            {85, 1.3, 6275.96}, {85, 3.67, 71430.7}, {80, 1.81, 17260.15},
            {79, 3.04, 12036.46}, {75, 1.76, 5088.63}, {74, 3.5, 3154.69},
            {74, 4.68, 801.82}, {70, 0.83, 9437.76}, {62, 3.98, 8827.39},
            {61, 1.82, 7084.9}, {57, 2.78, 6286.6}, {56, 4.39, 14143.5},
            {56, 3.47, 6279.55}, {52, 0.19, 12139.55}, {52, 1.33, 1748.02},
            {51, 0.28, 5856.48}, {49, 0.49, 1194.45}, {41, 5.37, 8429.24},
            {41, 2.4, 19651.05}, {39, 6.17, 10447.39}, {37, 6.04, 10213.29},
            {37, 2.57, 1059.38}, {36, 1.71, 2352.87}, {36, 1.78, 6812.77},
            {33, 0.59, 17789.85}, {30, 0.44, 83996.85}, {30, 2.74, 1349.87},
            {25, 3.16, 4690.48}
    };
    private static final double[][] VSOP_L1 = {
            {628331966747.0, 0, 0}, {206059, 2.678235, 6283.07585}, {4303, 2.6351, 12566.1517},
            {425, 1.59, 3.523}, {119, 5.796, 26.298}, {109, 2.966, 1577.344},
            {93, 2.59, 18849.23}, {72, 1.14, 529.69}, {68, 1.87, 398.15},
            {67, 4.41, 5507.55}, {59, 2.89, 5223.69}, {56, 2.17, 155.42},
            {45, 0.4, 796.3}, {36, 0.47, 775.52}, {29, 2.65, 7.11},
            {21, 5.34, 0.98}, {19, 1.85, 5486.78}, {19, 4.97, 213.3},
            {17, 2.99, 6275.96}, {16, 0.03, 2544.31}, {16, 1.43, 2146.17},
            {15, 1.21, 10977.08}, {12, 2.83, 1748.02}, {12, 3.26, 5088.63},
            {12, 5.27, 1194.45}, {12, 2.08, 4694.0}, {11, 0.77, 553.57},
            {10, 1.3, 6286.6}, {10, 4.24, 1349.87}, {9, 2.7, 242.73},
            {9, 5.64, 951.72}, {8, 5.3, 2352.87}, {6, 2.65, 9437.76},
            {6, 4.67, 4690.48}
    };
    private static final double[][] VSOP_L2 = {
            {52919, 0, 0}, {8720, 1.0721, 6283.0758}, {309, 0.867, 12566.152},
            {27, 0.05, 3.52}, {16, 5.19, 26.3}, {16, 3.68, 155.42},
            {10, 0.76, 18849.23}, {9, 2.06, 77713.77}, {7, 0.83, 775.52},
            {5, 4.66, 1577.34}, {4, 1.03, 7.11}, {4, 3.44, 5573.14},
            {3, 5.14, 796.3}, {3, 6.05, 5507.55}, {3, 1.19, 242.73},
            {3, 6.12, 529.69}, {3, 0.31, 398.15}, {3, 2.28, 553.57},
            {2, 4.38, 5223.69}, {2, 3.75, 0.98}
    };
    private static final double[][] VSOP_L3 = {
            {289, 5.844, 6283.076}, {35, 0, 0}, {17, 5.49, 12566.15},
            {3, 5.2, 155.42}, {1, 4.72, 3.52}, {1, 5.3, 18849.23},
            {1, 5.97, 242.73}
    };
    private static final double[][] VSOP_L4 = {
            {114, 3.142, 0}, {8, 4.13, 6283.08}, {1, 3.84, 12566.15}
    };
    private static final double[][] VSOP_L5 = {
            {1, 3.14, 0}
    };
    private static final double[][][] VSOP_L = {VSOP_L0, VSOP_L1, VSOP_L2, VSOP_L3, VSOP_L4, VSOP_L5};

    /**
     * 太陽視黃經（degrees, 0~360）：VSOP87 地球日心黃經 + 180°，再加上 FK5
     * 修正、黃經章動與光行差。jde 以地球時（TT）為準。
     */
    private static double sunApparentLongitudeDeg(double jde) {
        double tau = (jde - 2451545.0) / 365250.0; // J2000.0 起算的儒略千年數
        double t = tau * 10;                       // 儒略世紀數

        double l = 0;
        double tauPower = 1;
        for (double[][] series : VSOP_L) {
            double sum = 0;
            for (double[] term : series) {
                sum += term[0] * Math.cos(term[1] + term[2] * tau);
            }
            l += sum * tauPower;
            tauPower *= tau;
        }
        double longitude = Math.toDegrees(l / 1e8) + 180.0;

        longitude -= 0.09033 / 3600.0; // 轉換至 FK5 座標系

        // 黃經章動（主要四項，單位角秒）
        double omega = Math.toRadians(125.04452 - 1934.136261 * t);
        double sunMean = Math.toRadians(280.4665 + 36000.7698 * t);
        double moonMean = Math.toRadians(218.3165 + 481267.8813 * t);
        double nutation = -17.20 * Math.sin(omega) - 1.32 * Math.sin(2 * sunMean)
                - 0.23 * Math.sin(2 * moonMean) + 0.21 * Math.sin(2 * omega);

        // 光行差：-20.4898" / R（R 為日地距離，以低精度公式估算即可）
        double m = Math.toRadians(357.52911 + 35999.05029 * t);
        double r = 1.000140612 - 0.016708617 * Math.cos(m) - 0.000139589 * Math.cos(2 * m);
        double aberration = -20.4898 / r;

        return normalizeDegrees(longitude + (nutation + aberration) / 3600.0);
    }

    /**
     * ΔT＝TT－UT（秒），採 Espenak &amp; Meeus（NASA）分段多項式（1600~2150 年），
     * 範圍外以長期拋物線估算。各段多項式只在自身區間內有效，超出後誤差會急遽發散。
     */
    private static double deltaTSeconds(double jd) {
        double y = 2000.0 + (jd - 2451545.0) / 365.25;
        double t;
        if (y < 1600 || y >= 2150) {
            double u = (y - 1820) / 100.0;
            return -20 + 32 * u * u;
        } else if (y < 1700) {
            t = y - 1600;
            return 120 - 0.9808 * t - 0.01532 * t * t + t * t * t / 7129;
        } else if (y < 1800) {
            t = y - 1700;
            return 8.83 + 0.1603 * t - 0.0059285 * t * t + 0.00013336 * t * t * t - Math.pow(t, 4) / 1174000;
        } else if (y < 1860) {
            t = y - 1800;
            return 13.72 - 0.332447 * t + 0.0068612 * t * t + 0.0041116 * t * t * t
                    - 0.00037436 * Math.pow(t, 4) + 0.0000121272 * Math.pow(t, 5)
                    - 0.0000001699 * Math.pow(t, 6) + 0.000000000875 * Math.pow(t, 7);
        } else if (y < 1900) {
            t = y - 1860;
            return 7.62 + 0.5737 * t - 0.251754 * t * t + 0.01680668 * t * t * t
                    - 0.0004473624 * Math.pow(t, 4) + Math.pow(t, 5) / 233174;
        } else if (y < 1920) {
            t = y - 1900;
            return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t - 0.000197 * Math.pow(t, 4);
        } else if (y < 1941) {
            t = y - 1920;
            return 21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t;
        } else if (y < 1961) {
            t = y - 1950;
            return 29.07 + 0.407 * t - t * t / 233 + t * t * t / 2547;
        } else if (y < 1986) {
            t = y - 1975;
            return 45.45 + 1.067 * t - t * t / 260 - t * t * t / 718;
        } else if (y < 2005) {
            t = y - 2000;
            return 63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t
                    + 0.000651814 * Math.pow(t, 4) + 0.00002373599 * Math.pow(t, 5);
        } else if (y < 2050) {
            t = y - 2000;
            return 62.92 + 0.32217 * t + 0.005589 * t * t;
        }
        return -20 + 32 * Math.pow((y - 1820) / 100.0, 2) - 0.5628 * (2150 - y);
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
