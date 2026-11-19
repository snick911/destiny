package com.destiny.bazi.tests;

import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.LunarCalendar;
import com.destiny.bazi.io.BirthInput;
import com.destiny.bazi.io.PersonCsv;
import com.destiny.bazi.io.PersonStore;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** 人員資料 CSV 匯入／匯出：欄位對應、國曆／農曆（含閏月）、編碼判斷、錯誤回報、來回一致與同名比對。 */
final class PersonCsvTests {

    private PersonCsvTests() {
    }

    static void run() throws IOException {
        parsing();
        problems();
        encodingsAndRoundTrip();
        planAndStore();
    }

    private static void parsing() {
        String csv = "姓名,曆法,年,月,日,閏月,時,分,性別\n"
                + "國曆甲,國曆,1984,7,27,,9,30,男\n"
                + "農曆乙,農曆,1984,6,29,否,0,0,女\n"
                + "閏月丙,農曆,2023,2,16,是,10,5,M\n"
                + "\"王,小明\",西曆,2000,1,1,,23,,F\n"
                + "\n"
                + "# 註解列\n";
        PersonCsv.Result r = PersonCsv.parse(csv);
        Check.equal("CSV 成功筆數", 4, r.rows().size());
        Check.equal("CSV 無問題列", List.of(), r.problems());
        Check.equal("國曆列", new BirthInput(1984, 7, 27, 9, 30, Gender.MALE, "國曆甲"), r.people().get(0));
        Check.equal("農曆列換算為國曆", new BirthInput(1984, 7, 27, 0, 0, Gender.FEMALE, "農曆乙"), r.people().get(1));
        LocalDate leap = LunarCalendar.toSolar(2023, 2, true, 16);
        Check.equal("農曆閏月列", new BirthInput(leap.getYear(), leap.getMonthValue(), leap.getDayOfMonth(), 10, 5,
                Gender.MALE, "閏月丙"), r.people().get(2));
        Check.equal("引號內含逗號的姓名", "王,小明", r.people().get(3).name());
        Check.equal("分可省略", 0, r.people().get(3).minute());
        Check.equal("列號（含表頭）", 5, r.rows().get(3).lineNumber());

        // 表頭順序不同、別名、省略曆法／閏月／分（預設國曆）
        PersonCsv.Result alias = PersonCsv.parse("性别,时,日,月,年,name\r\nf,13,5,3,1990,丁\r\n");
        Check.equal("表頭別名與順序", new BirthInput(1990, 3, 5, 13, 0, Gender.FEMALE, "丁"), alias.people().get(0));

        // 沒有表頭：依 姓名,曆法,年,月,日,閏月,時,分,性別 的順序
        PersonCsv.Result noHeader = PersonCsv.parse("戊,農曆,1985,2,30,,6,0,女");
        Check.equal("無表頭依固定順序", new BirthInput(1985, 4, 19, 6, 0, Gender.FEMALE, "戊"), noHeader.people().get(0));
    }

    private static void problems() {
        String csv = "姓名,曆法,年,月,日,閏月,時,分,性別\n"
                + ",農曆,1976,12,17,否,7,0,女\n"            // 2 缺姓名
                + "甲,國曆,2023,2,30,,1,0,男\n"              // 3 國曆無此日
                + "乙,農曆,2024,5,1,是,1,0,男\n"             // 4 該年沒有閏五月
                + "丙,農曆,2023,1,30,,1,0,男\n"              // 5 小月沒有三十
                + "丁,國曆,1990,1,1,,24,0,男\n"              // 6 時超出
                + "戊,國曆,1990,1,1,,1,0,X\n"                // 7 性別
                + "己,國曆,一九九〇,1,1,,1,0,男\n"           // 8 非數字
                + "庚,國曆,1990,1,1,,1,0,男\n"               // 9 正常
                + "庚,國曆,1991,1,1,,1,0,女\n"               // 10 重複姓名
                + "辛,陰陽曆,1990,1,1,,1,0,男\n"             // 11 曆法
                + "壬,國曆,1899,1,1,,1,0,男\n";              // 12 超出年份
        PersonCsv.Result r = PersonCsv.parse(csv);
        Check.equal("錯誤檔只匯入正常列", 1, r.rows().size());
        Check.equal("錯誤列數", 10, r.problems().size());
        String all = r.problems().toString();
        String[] expected = {"第 2 列：缺少姓名", "第 3 列：國曆 2023年2月沒有 30 日", "第 4 列：2024年沒有閏五月",
                "第 5 列：2023年正月只有 29 天", "第 6 列：時應為 0~23", "第 7 列：無法辨識的性別", "第 8 列：「年」不是數字",
                "第 10 列：姓名「庚」與第 9 列重複", "第 11 列：無法辨識的曆法", "第 12 列：出生日期 1899-01-01 超出"};
        for (String e : expected) {
            Check.isTrue("錯誤訊息：" + e, all.contains(e), all);
        }

        PersonCsv.Result badHeader = PersonCsv.parse("姓名,年,月,日\n甲,1990,1,1\n");
        Check.equal("表頭缺必要欄位", 0, badHeader.rows().size());
        Check.isTrue("表頭缺欄位訊息", badHeader.problems().toString().contains("表頭缺少必要欄位：時、性別"),
                badHeader.problems().toString());
    }

    private static void encodingsAndRoundTrip() throws IOException {
        Path dir = Files.createTempDirectory("bazi-csv");
        try {
            String csv = "姓名,曆法,年,月,日,閏月,時,分,性別\r\n彭大明,農曆,1984,6,29,否,0,0,男\r\n";

            Path big5 = dir.resolve("big5.csv");
            Files.write(big5, csv.getBytes(Charset.forName("MS950")));
            PersonCsv.Result r = PersonCsv.read(big5);
            Check.isTrue("Big5 檔判斷", r.charset().name().contains("950"), r.charset().name());
            Check.equal("Big5 檔姓名", "彭大明", r.people().get(0).name());

            Path bom = dir.resolve("bom.csv");
            Files.write(bom, ("﻿" + csv).getBytes(StandardCharsets.UTF_8));
            Check.equal("UTF-8 BOM 檔", "彭大明", PersonCsv.read(bom).people().get(0).name());

            Path plain = dir.resolve("utf8.csv");
            Files.write(plain, csv.getBytes(StandardCharsets.UTF_8));
            Check.equal("UTF-8 檔", StandardCharsets.UTF_8, PersonCsv.read(plain).charset());

            List<BirthInput> people = List.of(
                    new BirthInput(1984, 7, 27, 0, 0, Gender.MALE, "彭大明"),
                    new BirthInput(1990, 2, 28, 23, 59, Gender.FEMALE, "Lee, \"Amy\""));
            Path out = dir.resolve("export.csv");
            PersonCsv.write(out, people);
            byte[] bytes = Files.readAllBytes(out);
            Check.isTrue("匯出含 UTF-8 BOM", (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB, "沒有 BOM");
            Check.equal("匯出再匯入一致", people, PersonCsv.read(out).people());
        } finally {
            try (var paths = Files.walk(dir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static void planAndStore() throws IOException {
        Path dir = Files.createTempDirectory("bazi-csv-store");
        try {
            PersonStore store = new PersonStore(dir.resolve("people.tsv"));
            store.put(new BirthInput(1980, 1, 1, 1, 0, Gender.MALE, "相同"));
            store.put(new BirthInput(1981, 1, 1, 1, 0, Gender.MALE, "不同"));
            PersonCsv.Result r = PersonCsv.parse("姓名,年,月,日,時,性別\n相同,1980,1,1,1,男\n不同,1999,9,9,9,女\n新的,2001,1,1,1,男\n");
            PersonCsv.Plan plan = PersonCsv.plan(store, r);
            Check.equal("計畫：新增", List.of("新的"), plan.added().stream().map(BirthInput::name).toList());
            Check.equal("計畫：同名不同資料", List.of("不同"), plan.changed().stream().map(BirthInput::name).toList());
            Check.equal("計畫：與現有相同", List.of("相同"), plan.unchanged().stream().map(BirthInput::name).toList());

            int[] counts = store.putAll(plan.added());
            Check.equal("只新增（保留原有）", "1/0", counts[0] + "/" + counts[1]);
            Check.equal("保留原有資料", 1981, store.find("不同").year());
            counts = store.putAll(plan.changed());
            Check.equal("覆蓋同名", "0/1", counts[0] + "/" + counts[1]);
            Check.equal("覆蓋後資料", 1999, store.find("不同").year());
            Check.equal("人員總數", 3, store.list().size());
            Check.equal("覆蓋不改變順序", "相同,不同,新的",
                    String.join(",", store.list().stream().map(BirthInput::name).toList()));
        } finally {
            try (var paths = Files.walk(dir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
