package com.destiny.bazi.ziwei;

import com.destiny.bazi.core.GanZhi;

/**
 * 一步大限（十年）：所在宮位的地支與宮干、起訖虛歲，以及對應的農曆年份。
 *
 * @param index     第幾步大限（0 起算，0 為命宮所在的第一限）
 * @param branch    大限命宮的地支索引（子=0）
 * @param stem      大限命宮的宮干索引（甲=0），決定大限四化
 * @param startAge  起始虛歲（第一限即五行局數）
 * @param endAge    結束虛歲
 * @param startYear 起始農曆年（出生年＋起始虛歲－1）
 * @param endYear   結束農曆年
 */
public record Decade(int index, int branch, int stem, int startAge, int endAge, int startYear, int endYear) {

    public String ganZhi() {
        return GanZhi.STEMS[stem] + GanZhi.BRANCHES[branch];
    }

    public boolean containsAge(int age) {
        return age >= startAge && age <= endAge;
    }
}
