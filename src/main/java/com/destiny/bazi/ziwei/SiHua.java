package com.destiny.bazi.ziwei;

/**
 * 四化：化祿、化權、化科、化忌。依天干決定化曜的星：
 *
 * <pre>
 * 甲：廉貞祿 破軍權 武曲科 太陽忌    乙：天機祿 天梁權 紫微科 太陰忌
 * 丙：天同祿 天機權 文昌科 廉貞忌    丁：太陰祿 天同權 天機科 巨門忌
 * 戊：貪狼祿 太陰權 右弼科 天機忌    己：武曲祿 貪狼權 天梁科 文曲忌
 * 庚：太陽祿 武曲權 太陰科 天同忌    辛：巨門祿 太陽權 文曲科 文昌忌
 * 壬：天梁祿 紫微權 左輔科 武曲忌    癸：破軍祿 巨門權 太陰科 貪狼忌
 * </pre>
 *
 * <p>與 lifeup 相同（庚干採「日武陰同」、戊干科右弼、壬干科左輔）。</p>
 */
public enum SiHua {
    LU("祿"),
    QUAN("權"),
    KE("科"),
    JI("忌");

    private static final Star[][] BY_STEM = {
            {Star.LIAN_ZHEN, Star.PO_JUN, Star.WU_QU, Star.TAI_YANG},     // 甲
            {Star.TIAN_JI, Star.TIAN_LIANG, Star.ZI_WEI, Star.TAI_YIN},    // 乙
            {Star.TIAN_TONG, Star.TIAN_JI, Star.WEN_CHANG, Star.LIAN_ZHEN}, // 丙
            {Star.TAI_YIN, Star.TIAN_TONG, Star.TIAN_JI, Star.JU_MEN},     // 丁
            {Star.TAN_LANG, Star.TAI_YIN, Star.YOU_BI, Star.TIAN_JI},      // 戊
            {Star.WU_QU, Star.TAN_LANG, Star.TIAN_LIANG, Star.WEN_QU},     // 己
            {Star.TAI_YANG, Star.WU_QU, Star.TAI_YIN, Star.TIAN_TONG},     // 庚
            {Star.JU_MEN, Star.TAI_YANG, Star.WEN_QU, Star.WEN_CHANG},     // 辛
            {Star.TIAN_LIANG, Star.ZI_WEI, Star.ZUO_FU, Star.WU_QU},       // 壬
            {Star.PO_JUN, Star.JU_MEN, Star.TAI_YIN, Star.TAN_LANG}        // 癸
    };

    private final String label;

    SiHua(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 天干（甲=0）化出此四化的星。 */
    public Star starFor(int stemIndex) {
        return BY_STEM[stemIndex][ordinal()];
    }

    @Override
    public String toString() {
        return "化" + label;
    }
}
