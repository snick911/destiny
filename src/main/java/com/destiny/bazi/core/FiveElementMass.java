package com.destiny.bazi.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 五行質量：以旺相休囚死加權後，四柱天干與地支藏干合計的五行分量。
 */
public final class FiveElementMass {

    private final Map<String, Double> mass;

    public FiveElementMass(Map<String, Double> mass) {
        this.mass = new LinkedHashMap<>(mass);
    }

    public double get(String element) {
        return mass.getOrDefault(element, 0.0);
    }

    /** 依 木火土金水 順序回傳。 */
    public Map<String, Double> asMap() {
        return mass;
    }
}
