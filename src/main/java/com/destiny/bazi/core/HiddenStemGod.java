package com.destiny.bazi.core;

/**
 * 地支中一個藏干及其對應的十神。
 */
public final class HiddenStemGod {

    private final String stem;
    private final String tenGod;
    private final boolean primary;

    public HiddenStemGod(String stem, String tenGod, boolean primary) {
        this.stem = stem;
        this.tenGod = tenGod;
        this.primary = primary;
    }

    public String getStem() {
        return stem;
    }

    public String getTenGod() {
        return tenGod;
    }

    /** 是否為本氣（該地支所藏的主要天干）。 */
    public boolean isPrimary() {
        return primary;
    }
}
