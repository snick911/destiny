package com.destiny.bazi.core;

/**
 * 性別，用於判定大運的順排／逆排方向。
 */
public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
