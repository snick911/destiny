package com.destiny.bazi.io;

import com.destiny.bazi.core.Gender;

/**
 * 排盤所需的出生年月日時分與性別輸入（性別用於判定大運順排／逆排）。
 */
public record BirthInput(int year, int month, int day, int hour, int minute, Gender gender) {
}
