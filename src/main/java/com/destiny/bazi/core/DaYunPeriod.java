package com.destiny.bazi.core;

/**
 * 單一大運（十年一運）的干支、起訖年份、歲數與十神資訊。
 *
 * <p>起始年份為該期大運交運當年（第一期即交運日期所在年份，其後每期加 10 年）；
 * 歲數以「年份－出生年」計（即當年滿幾歲），與流年清單的歲數一致。</p>
 */
public final class DaYunPeriod {

    private final Pillar pillar;
    private final int startYear;
    private final int startAge;
    private final PillarTenGods tenGods;

    public DaYunPeriod(Pillar pillar, int startYear, int startAge, PillarTenGods tenGods) {
        this.pillar = pillar;
        this.startYear = startYear;
        this.startAge = startAge;
        this.tenGods = tenGods;
    }

    public Pillar getPillar() {
        return pillar;
    }

    /** 交運當年（西元）。 */
    public int getStartYear() {
        return startYear;
    }

    public int getEndYear() {
        return startYear + 9;
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
