package com.destiny.bazi.core;

/**
 * 排盤結果：四柱（年月日時）以及排盤過程中的輔助資訊。
 */
public final class BaziResult {

    private final Pillar yearPillar;
    private final Pillar monthPillar;
    private final Pillar dayPillar;
    private final Pillar hourPillar;
    private final int effectiveYear;
    private final boolean earlyZiShift;

    private final PillarTenGods yearTenGods;
    private final PillarTenGods monthTenGods;
    private final PillarTenGods dayTenGods;
    private final PillarTenGods hourTenGods;

    public BaziResult(Pillar yearPillar, Pillar monthPillar, Pillar dayPillar, Pillar hourPillar,
                       int effectiveYear, boolean earlyZiShift,
                       PillarTenGods yearTenGods, PillarTenGods monthTenGods,
                       PillarTenGods dayTenGods, PillarTenGods hourTenGods) {
        this.yearPillar = yearPillar;
        this.monthPillar = monthPillar;
        this.dayPillar = dayPillar;
        this.hourPillar = hourPillar;
        this.effectiveYear = effectiveYear;
        this.earlyZiShift = earlyZiShift;
        this.yearTenGods = yearTenGods;
        this.monthTenGods = monthTenGods;
        this.dayTenGods = dayTenGods;
        this.hourTenGods = hourTenGods;
    }

    public Pillar getYearPillar() {
        return yearPillar;
    }

    public Pillar getMonthPillar() {
        return monthPillar;
    }

    public Pillar getDayPillar() {
        return dayPillar;
    }

    public Pillar getHourPillar() {
        return hourPillar;
    }

    /** 以立春為界所判定的實際八字流年（可能與西曆年份相差 1）。 */
    public int getEffectiveYear() {
        return effectiveYear;
    }

    /** 是否因 23:00~23:59（晚子時）而將日柱推算至次日。 */
    public boolean isEarlyZiShift() {
        return earlyZiShift;
    }

    public String getZodiac() {
        return GanZhi.ZODIAC[yearPillar.getBranchIndex()];
    }

    public PillarTenGods getYearTenGods() {
        return yearTenGods;
    }

    public PillarTenGods getMonthTenGods() {
        return monthTenGods;
    }

    public PillarTenGods getDayTenGods() {
        return dayTenGods;
    }

    public PillarTenGods getHourTenGods() {
        return hourTenGods;
    }
}
