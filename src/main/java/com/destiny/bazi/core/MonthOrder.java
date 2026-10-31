package com.destiny.bazi.core;

/**
 * 月令：出生時刻在月柱地支中實際「當令」的天干與五行。
 *
 * <p>辰戌丑未（四墓庫月）依入節天數分三階段：前 5 天為上個節氣殘留的
 * 「餘氣」當令，中間 9 天為該支所藏「墓庫」之氣（中氣）當令，後 16 天
 * 才是純粹的本氣（戊／己土）當令。其餘地支不分階段，直接以該支本氣
 * （主氣）為月令。</p>
 */
public final class MonthOrder {

    private final String stem;
    private final String element;
    private final String phase;
    private final long dayOfMonth;

    public MonthOrder(String stem, String element, String phase, long dayOfMonth) {
        this.stem = stem;
        this.element = element;
        this.phase = phase;
        this.dayOfMonth = dayOfMonth;
    }

    /** 當令天干，例如辰月本氣當令時為「戊」。 */
    public String getStem() {
        return stem;
    }

    /** 當令五行：木火土金水。 */
    public String getElement() {
        return element;
    }

    /** 「餘氣」／「中氣」／「本氣」（辰戌丑未）或「主氣」（其餘地支）。 */
    public String getPhase() {
        return phase;
    }

    /** 入節第幾天（自本月節氣交接當天起算為第 1 天）。 */
    public long getDayOfMonth() {
        return dayOfMonth;
    }
}
