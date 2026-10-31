package com.destiny.bazi.core;

/**
 * 單一大運（十年一運）的干支、起訖虛歲與十神資訊。
 */
public final class DaYunPeriod {

    private final Pillar pillar;
    private final int startAge;
    private final PillarTenGods tenGods;

    public DaYunPeriod(Pillar pillar, int startAge, PillarTenGods tenGods) {
        this.pillar = pillar;
        this.startAge = startAge;
        this.tenGods = tenGods;
    }

    public Pillar getPillar() {
        return pillar;
    }

    public int getStartAge() {
        return startAge;
    }

    public int getEndAge() {
        return startAge + 9;
    }

    public PillarTenGods getTenGods() {
        return tenGods;
    }
}
