package com.destiny.bazi.ziwei;

/**
 * 盤的層次：本命盤，以及疊在其上的大限、流年、流月。
 * 前綴用於標示行運宮位與星曜，例如「大命」「流財」「月祿」。
 */
public enum Layer {
    NATAL("本命", "本"),
    DECADE("大限", "大"),
    YEAR("流年", "流"),
    MONTH("流月", "月");

    private final String label;
    private final String prefix;

    Layer(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    public String label() {
        return label;
    }

    public String prefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return label;
    }
}
