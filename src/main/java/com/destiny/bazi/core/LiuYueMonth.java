package com.destiny.bazi.core;

/**
 * 某一流年中，單一流月（正月~十二月）的干支與十神資訊。
 */
public final class LiuYueMonth {

    /** 依月支順序（寅卯辰巳午未申酉戌亥子丑）對應的月份名稱。 */
    public static final String[] MONTH_NAMES = {
            "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"
    };

    private final int year;
    private final int monthIndex;
    private final Pillar pillar;
    private final PillarTenGods tenGods;

    public LiuYueMonth(int year, int monthIndex, Pillar pillar, PillarTenGods tenGods) {
        this.year = year;
        this.monthIndex = monthIndex;
        this.pillar = pillar;
        this.tenGods = tenGods;
    }

    public int getYear() {
        return year;
    }

    /** 0=正月（寅）～11=十二月（丑）。 */
    public int getMonthIndex() {
        return monthIndex;
    }

    public String getMonthName() {
        return MONTH_NAMES[monthIndex];
    }

    public Pillar getPillar() {
        return pillar;
    }

    public PillarTenGods getTenGods() {
        return tenGods;
    }
}
