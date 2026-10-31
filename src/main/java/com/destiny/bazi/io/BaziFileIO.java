package com.destiny.bazi.io;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.Gender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 排盤結果的儲存與載入（純文字 Properties 格式，UTF-8 編碼）。
 *
 * <p>檔案儲存出生年月日時分（載入時據此重新排盤），以及儲存時的程式版本與
 * 當時算出的四柱作為可讀紀錄；載入時僅採用出生年月日時分，確保結果與目前的排盤邏輯一致。</p>
 */
public final class BaziFileIO {

    private BaziFileIO() {
    }

    public static void save(File file, BirthInput input, BaziResult result) throws IOException {
        Properties props = new Properties();
        props.setProperty("appVersion", AppInfo.VERSION);
        props.setProperty("year", String.valueOf(input.year()));
        props.setProperty("month", String.valueOf(input.month()));
        props.setProperty("day", String.valueOf(input.day()));
        props.setProperty("hour", String.valueOf(input.hour()));
        props.setProperty("minute", String.valueOf(input.minute()));
        props.setProperty("gender", input.gender().name());
        props.setProperty("yearPillar", result.getYearPillar().getGanZhi());
        props.setProperty("monthPillar", result.getMonthPillar().getGanZhi());
        props.setProperty("dayPillar", result.getDayPillar().getGanZhi());
        props.setProperty("hourPillar", result.getHourPillar().getGanZhi());
        props.setProperty("effectiveYear", String.valueOf(result.getEffectiveYear()));
        props.setProperty("zodiac", result.getZodiac());

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            props.store(writer, "子平八字排盤結果");
        }
    }

    public static BirthInput load(File file) throws IOException {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        try {
            int year = Integer.parseInt(props.getProperty("year"));
            int month = Integer.parseInt(props.getProperty("month"));
            int day = Integer.parseInt(props.getProperty("day"));
            int hour = Integer.parseInt(props.getProperty("hour"));
            int minute = Integer.parseInt(props.getProperty("minute", "0"));
            Gender gender;
            try {
                gender = Gender.valueOf(props.getProperty("gender", Gender.MALE.name()));
            } catch (IllegalArgumentException e) {
                gender = Gender.MALE;
            }
            return new BirthInput(year, month, day, hour, minute, gender);
        } catch (NumberFormatException e) {
            throw new IOException("檔案格式不正確或內容缺漏", e);
        }
    }
}
