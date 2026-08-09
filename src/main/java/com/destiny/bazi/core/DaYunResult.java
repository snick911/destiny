package com.destiny.bazi.core;

import java.util.List;

/**
 * 大運排盤結果：順排／逆排方向、起運歲數，以及各期大運。
 */
public final class DaYunResult {

    private final boolean forward;
    private final int startAgeYears;
    private final int startAgeMonths;
    private final int startAgeDays;
    private final List<DaYunPeriod> periods;

    public DaYunResult(boolean forward, int startAgeYears, int startAgeMonths, int startAgeDays,
                        List<DaYunPeriod> periods) {
        this.forward = forward;
        this.startAgeYears = startAgeYears;
        this.startAgeMonths = startAgeMonths;
        this.startAgeDays = startAgeDays;
        this.periods = periods;
    }

    /** true 為順排，false 為逆排。 */
    public boolean isForward() {
        return forward;
    }

    public int getStartAgeYears() {
        return startAgeYears;
    }

    public int getStartAgeMonths() {
        return startAgeMonths;
    }

    public int getStartAgeDays() {
        return startAgeDays;
    }

    public List<DaYunPeriod> getPeriods() {
        return periods;
    }
}
