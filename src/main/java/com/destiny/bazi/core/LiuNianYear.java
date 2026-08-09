package com.destiny.bazi.core;

/**
 * 單一流年（某西元年份）的干支與十神資訊。
 */
public final class LiuNianYear {

    private final int year;
    private final Pillar pillar;
    private final PillarTenGods tenGods;

    public LiuNianYear(int year, Pillar pillar, PillarTenGods tenGods) {
        this.year = year;
        this.pillar = pillar;
        this.tenGods = tenGods;
    }

    public int getYear() {
        return year;
    }

    public Pillar getPillar() {
        return pillar;
    }

    public PillarTenGods getTenGods() {
        return tenGods;
    }
}
