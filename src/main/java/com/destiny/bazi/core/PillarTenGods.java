package com.destiny.bazi.core;

import java.util.List;

/**
 * 單一柱的十神資訊：天干十神（日柱本身為「日主」）與地支藏干十神列表。
 */
public final class PillarTenGods {

    private final String stemTenGod;
    private final List<HiddenStemGod> branchHiddenGods;

    public PillarTenGods(String stemTenGod, List<HiddenStemGod> branchHiddenGods) {
        this.stemTenGod = stemTenGod;
        this.branchHiddenGods = branchHiddenGods;
    }

    public String getStemTenGod() {
        return stemTenGod;
    }

    public List<HiddenStemGod> getBranchHiddenGods() {
        return branchHiddenGods;
    }
}
