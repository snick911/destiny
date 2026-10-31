package com.destiny.bazi.core;

/**
 * 天干、地支、五行與生肖的基礎資料表。
 * 天干索引 0~9 對應 甲乙丙丁戊己庚辛壬癸；地支索引 0~11 對應 子丑寅卯辰巳午未申酉戌亥。
 */
public final class GanZhi {

    public static final String[] STEMS = {
            "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
    };

    public static final String[] BRANCHES = {
            "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    };

    // 甲乙屬木、丙丁屬火、戊己屬土、庚辛屬金、壬癸屬水
    public static final String[] STEM_ELEMENTS = {
            "木", "木", "火", "火", "土", "土", "金", "金", "水", "水"
    };

    // 子屬水、丑屬土、寅卯屬木、辰屬土、巳午屬火、未屬土、申酉屬金、戌屬土、亥屬水
    public static final String[] BRANCH_ELEMENTS = {
            "水", "土", "木", "木", "土", "火", "火", "土", "金", "金", "土", "水"
    };

    public static final String[] ZODIAC = {
            "鼠", "牛", "虎", "兔", "龍", "蛇", "馬", "羊", "猴", "雞", "狗", "豬"
    };

    private GanZhi() {
    }

    /** 將任意整數安全地轉換為 [0, n) 範圍內的餘數。 */
    public static int mod(long value, int n) {
        int r = (int) (value % n);
        return r < 0 ? r + n : r;
    }
}
