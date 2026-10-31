package com.destiny.bazi.core;

/**
 * 地支藏干表：每個地支所藏的天干，本氣（該支主氣）固定在陣列第一位；
 * 其餘藏干僅依重要性排序供顯示用，並不代表月令當令的日序（月令日序見
 * {@link BaziCalculator} 對辰戌丑未四墓庫月的餘氣／中氣／本氣判定）。
 */
public final class HiddenStems {

    // 天干索引：0甲 1乙 2丙 3丁 4戊 5己 6庚 7辛 8壬 9癸
    private static final int[][] TABLE = {
            {9},        // 子：癸
            {5, 9, 7},  // 丑：己(本氣) 癸(餘氣) 辛(中氣，金庫)
            {0, 2, 4},  // 寅：甲 丙 戊
            {1},        // 卯：乙
            {4, 1, 9},  // 辰：戊(本氣) 乙(餘氣) 癸(中氣，水庫)
            {2, 6, 4},  // 巳：丙 庚 戊
            {3, 5},     // 午：丁 己
            {5, 3, 1},  // 未：己(本氣) 丁(餘氣) 乙(中氣，木庫)
            {6, 8, 4},  // 申：庚 壬 戊
            {7},        // 酉：辛
            {4, 7, 3},  // 戌：戊(本氣) 辛(餘氣) 丁(中氣，火庫)
            {8, 0},     // 亥：壬 甲
    };

    private HiddenStems() {
    }

    /** 回傳指定地支（索引 0~11）所藏天干的索引陣列，本氣在前。 */
    public static int[] of(int branchIndex) {
        return TABLE[GanZhi.mod(branchIndex, 12)];
    }
}
