package com.destiny.bazi.core;

/**
 * 十神推算：依「日主」（日干）與目標天干的五行生克關係、陰陽同異，
 * 判定比肩、劫財、食神、傷官、偏財、正財、七殺、正官、偏印、正印。
 */
public final class TenGod {

    public static final String RI_ZHU = "日主";

    /** 十神固定順序：比肩、劫財、食神、傷官、偏財、正財、七殺、正官、偏印、正印。 */
    public static final String[] ALL = {
            "比肩", "劫財", "食神", "傷官", "偏財", "正財", "七殺", "正官", "偏印", "正印"
    };

    // 依「五行生克差距」(0~4) 分組，每組再依陰陽同異分為兩神。
    // 差距 0：同我 -> 比肩/劫財；1：我生 -> 食神/傷官；2：我克 -> 偏財/正財；
    // 差距 3：克我 -> 七殺/正官；4：生我 -> 偏印/正印。
    private static final String[][] TABLE = {
            {"比肩", "劫財"},
            {"食神", "傷官"},
            {"偏財", "正財"},
            {"七殺", "正官"},
            {"偏印", "正印"},
    };

    private TenGod() {
    }

    /**
     * 計算目標天干相對於日主天干的十神。若目標即為日主本身，回傳 {@link #RI_ZHU}。
     *
     * @param dayStemIndex    日干索引（0~9）
     * @param targetStemIndex 欲判定的天干索引（0~9）
     */
    public static String of(int dayStemIndex, int targetStemIndex) {
        int dayGroup = dayStemIndex / 2;   // 五行分組：0木 1火 2土 3金 4水
        int dayYang = dayStemIndex % 2;    // 0 陽、1 陰
        int targetGroup = targetStemIndex / 2;
        int targetYang = targetStemIndex % 2;

        int diff = GanZhi.mod(targetGroup - dayGroup, 5);
        int polarity = (dayYang == targetYang) ? 0 : 1; // 0 同性、1 異性
        return TABLE[diff][polarity];
    }
}
