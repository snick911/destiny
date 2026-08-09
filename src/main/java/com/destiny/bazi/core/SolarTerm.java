package com.destiny.bazi.core;

import java.time.LocalDateTime;

/**
 * 二十四節氣時刻推算。
 *
 * <p>採用天文平均法：以西元 1900 年為基準年，每個節氣相對於基準年"小寒"的分鐘偏移量
 * 記錄於 {@link #TERM_OFFSETS}，並以回歸年平均長度（525948.76 分鐘）推算任意年份。
 * 此法對 1900~2100 年間的節氣時刻誤差通常在數十分鐘內，足以判斷八字排盤所需的
 * 年柱（立春）與月柱（十二節）交界。</p>
 *
 * <p>節氣索引 0~23 依序為：
 * 0 小寒 1 大寒 2 立春 3 雨水 4 驚蟄 5 春分 6 清明 7 穀雨
 * 8 立夏 9 小滿 10 芒種 11 夏至 12 小暑 13 大暑 14 立秋 15 處暑
 * 16 白露 17 秋分 18 寒露 19 霜降 20 立冬 21 小雪 22 大雪 23 冬至</p>
 */
public final class SolarTerm {

    public static final String[] NAMES = {
            "小寒", "大寒", "立春", "雨水", "驚蟄", "春分", "清明", "穀雨",
            "立夏", "小滿", "芒種", "夏至", "小暑", "大暑", "立秋", "處暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    private static final long[] TERM_OFFSETS = {
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921,
            173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
            353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    };

    private static final double MINUTES_PER_YEAR = 525948.76;

    /** 基準時刻：西元 1900 年 1 月 6 日 02:05（當地時間，用於八字排盤預設以北京時間計算）。 */
    private static final LocalDateTime BASE = LocalDateTime.of(1900, 1, 6, 2, 5, 0);

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
     * 計算指定西元年份中，某節氣（0~23）發生的時刻。
     */
    public static LocalDateTime getTime(int year, int termIndex) {
        long offsetMinutes = Math.round(MINUTES_PER_YEAR * (year - 1900)) + TERM_OFFSETS[termIndex];
        return BASE.plusMinutes(offsetMinutes);
    }
}
