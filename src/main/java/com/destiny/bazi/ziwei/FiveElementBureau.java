package com.destiny.bazi.ziwei;

/**
 * 五行局：依命宮干支的納音五行決定，局數即起大限的歲數，也用於安紫微星。
 */
public enum FiveElementBureau {
    WATER("水二局", "水", 2),
    WOOD("木三局", "木", 3),
    METAL("金四局", "金", 4),
    EARTH("土五局", "土", 5),
    FIRE("火六局", "火", 6);

    // [干組 甲乙/丙丁/戊己/庚辛/壬癸][支組 子丑午未/寅卯申酉/辰巳戌亥]
    private static final FiveElementBureau[][] TABLE = {
            {METAL, WATER, FIRE},
            {WATER, FIRE, EARTH},
            {FIRE, EARTH, WOOD},
            {EARTH, WOOD, METAL},
            {WOOD, METAL, WATER}
    };

    private final String label;
    private final String element;
    private final int number;

    FiveElementBureau(String label, String element, int number) {
        this.label = label;
        this.element = element;
        this.number = number;
    }

    /** 命宮干支（天干甲=0、地支子=0）的納音五行局。 */
    public static FiveElementBureau of(int stemIndex, int branchIndex) {
        return TABLE[stemIndex / 2][(branchIndex / 2) % 3];
    }

    public String label() {
        return label;
    }

    public String element() {
        return element;
    }

    public int number() {
        return number;
    }

    @Override
    public String toString() {
        return label;
    }
}
