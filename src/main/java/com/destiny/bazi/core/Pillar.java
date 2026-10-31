package com.destiny.bazi.core;

/**
 * 代表八字中的一柱（年柱、月柱、日柱或時柱），包含天干與地支索引。
 */
public final class Pillar {

    private final int stemIndex;
    private final int branchIndex;

    public Pillar(int stemIndex, int branchIndex) {
        this.stemIndex = GanZhi.mod(stemIndex, 10);
        this.branchIndex = GanZhi.mod(branchIndex, 12);
    }

    public String getStem() {
        return GanZhi.STEMS[stemIndex];
    }

    public String getBranch() {
        return GanZhi.BRANCHES[branchIndex];
    }

    public String getStemElement() {
        return GanZhi.STEM_ELEMENTS[stemIndex];
    }

    public String getBranchElement() {
        return GanZhi.BRANCH_ELEMENTS[branchIndex];
    }

    public String getGanZhi() {
        return getStem() + getBranch();
    }

    public int getStemIndex() {
        return stemIndex;
    }

    public int getBranchIndex() {
        return branchIndex;
    }

    @Override
    public String toString() {
        return getGanZhi();
    }
}
