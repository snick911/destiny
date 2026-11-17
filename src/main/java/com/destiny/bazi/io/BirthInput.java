package com.destiny.bazi.io;

import com.destiny.bazi.core.Gender;

/**
 * 排盤所需的出生年月日時分與性別輸入（性別用於判定大運順排／逆排），以及選填的姓名。
 *
 * <p>姓名會去除前後空白，並把 Tab、換行等控制字元換成空白，確保可安全寫入人員資料檔；
 * 未填寫時為空字串。</p>
 */
public record BirthInput(int year, int month, int day, int hour, int minute, Gender gender, String name) {

    public BirthInput {
        name = normalizeName(name);
    }

    /** 不含姓名的輸入（姓名為空字串）。 */
    public BirthInput(int year, int month, int day, int hour, int minute, Gender gender) {
        this(year, month, day, hour, minute, gender, "");
    }

    public boolean hasName() {
        return !name.isEmpty();
    }

    /** 回傳同樣出生資料、但改為指定姓名的輸入。 */
    public BirthInput withName(String newName) {
        return new BirthInput(year, month, day, hour, minute, gender, newName);
    }

    /** 是否與另一筆輸入的出生年月日時分、性別相同（不比較姓名）。 */
    public boolean sameBirth(BirthInput other) {
        return year == other.year && month == other.month && day == other.day
                && hour == other.hour && minute == other.minute && gender == other.gender;
    }

    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        raw.codePoints().forEach(cp -> sb.appendCodePoint(Character.isISOControl(cp) ? ' ' : cp));
        return sb.toString().trim();
    }
}
