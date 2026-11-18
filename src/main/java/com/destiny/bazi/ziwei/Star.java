package com.destiny.bazi.ziwei;

/**
 * 紫微斗數星曜：十四主星、六吉星、六煞星、祿存天馬與雜曜。
 * 列舉順序即同一宮內的顯示順序（主星在前）。
 */
public enum Star {

    // 紫微星系（自紫微逆行）
    ZI_WEI("紫微", Category.MAIN),
    TIAN_JI("天機", Category.MAIN),
    TAI_YANG("太陽", Category.MAIN),
    WU_QU("武曲", Category.MAIN),
    TIAN_TONG("天同", Category.MAIN),
    LIAN_ZHEN("廉貞", Category.MAIN),
    // 天府星系（自天府順行）
    TIAN_FU("天府", Category.MAIN),
    TAI_YIN("太陰", Category.MAIN),
    TAN_LANG("貪狼", Category.MAIN),
    JU_MEN("巨門", Category.MAIN),
    TIAN_XIANG("天相", Category.MAIN),
    TIAN_LIANG("天梁", Category.MAIN),
    QI_SHA("七殺", Category.MAIN),
    PO_JUN("破軍", Category.MAIN),

    // 六吉星
    ZUO_FU("左輔", Category.LUCKY),
    YOU_BI("右弼", Category.LUCKY),
    WEN_CHANG("文昌", Category.LUCKY),
    WEN_QU("文曲", Category.LUCKY),
    TIAN_KUI("天魁", Category.LUCKY),
    TIAN_YUE("天鉞", Category.LUCKY),

    // 六煞星
    QING_YANG("擎羊", Category.SHA),
    TUO_LUO("陀羅", Category.SHA),
    HUO_XING("火星", Category.SHA),
    LING_XING("鈴星", Category.SHA),
    DI_KONG("地空", Category.SHA),
    DI_JIE("地劫", Category.SHA),

    // 祿存、天馬
    LU_CUN("祿存", Category.LU_MA),
    TIAN_MA("天馬", Category.LU_MA),

    // 雜曜
    TIAN_XING("天刑", Category.MINOR),
    TIAN_YAO("天姚", Category.MINOR),
    YIN_SHA("陰煞", Category.MINOR),
    HONG_LUAN("紅鸞", Category.MINOR),
    TIAN_XI("天喜", Category.MINOR),
    XIAN_CHI("咸池", Category.MINOR),
    TIAN_WU("天巫", Category.MINOR),
    HUA_GAI("華蓋", Category.MINOR),
    JIE_SHEN("解神", Category.MINOR),
    TAI_FU("台輔", Category.MINOR),
    FENG_GAO("封誥", Category.MINOR),
    TIAN_CAI("天才", Category.MINOR),
    TIAN_SHOU("天壽", Category.MINOR),
    TIAN_GUAN("天官", Category.MINOR),
    TIAN_FU_BLESSING("天福", Category.MINOR),
    GU_CHEN("孤辰", Category.MINOR),
    GUA_SU("寡宿", Category.MINOR),
    PO_SUI("破碎", Category.MINOR),
    TIAN_KU("天哭", Category.MINOR),
    TIAN_XU("天虛", Category.MINOR);

    /** 星曜類別，決定排盤時的顯示顏色與先後。 */
    public enum Category {
        MAIN("主星"),
        LUCKY("六吉星"),
        SHA("六煞星"),
        LU_MA("祿存天馬"),
        MINOR("雜曜");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String label;
    private final Category category;

    Star(String label, Category category) {
        this.label = label;
        this.category = category;
    }

    public String label() {
        return label;
    }

    public Category category() {
        return category;
    }

    /** 行運星曜的簡稱（祿存→祿、擎羊→羊…），配合大／流／月前綴顯示，例如「流祿」。 */
    public String shortLabel() {
        return switch (this) {
            case LU_CUN -> "祿";
            case QING_YANG -> "羊";
            case TUO_LUO -> "陀";
            case TIAN_MA -> "馬";
            case TIAN_KUI -> "魁";
            case TIAN_YUE -> "鉞";
            case WEN_CHANG -> "昌";
            case WEN_QU -> "曲";
            case HONG_LUAN -> "鸞";
            case TIAN_XI -> "喜";
            default -> label;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
