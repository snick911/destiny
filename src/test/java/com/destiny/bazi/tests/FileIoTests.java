package com.destiny.bazi.tests;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.io.BaziFileIO;
import com.destiny.bazi.io.BirthInput;
import com.destiny.bazi.io.PersonStore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** .bazi 存檔與載入：來回一致、記錄版本、相容舊檔、拒絕格式錯誤的檔案。 */
final class FileIoTests {

    private FileIoTests() {
    }

    static void run() throws IOException {
        File dir = Files.createTempDirectory("bazi-test").toFile();
        try {
            // 與程式預設檔名相同使用中文；系統檔名編碼無法表示中文時（如未設定語系的 Linux）改用英文檔名。
            String name = "八字_19900515.bazi";
            if (!Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")).newEncoder().canEncode(name)) {
                name = "bazi_19900515.bazi";
            }
            File file = new File(dir, name);
            BirthInput input = new BirthInput(1990, 5, 15, 8, 30, Gender.FEMALE);
            BaziFileIO.save(file, input, new BaziCalculator().calculate(1990, 5, 15, 8, 30));
            Check.equal("存檔後載入", input, BaziFileIO.load(file));
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Check.isTrue("存檔記錄版本", content.contains("appVersion=" + AppInfo.VERSION), "找不到 appVersion");
            Check.isTrue("存檔記錄四柱", content.contains("dayPillar="), "找不到 dayPillar");

            // 含姓名的存檔
            File named = new File(dir, "named.bazi");
            BirthInput withName = input.withName("王小明");
            BaziFileIO.save(named, withName, new BaziCalculator().calculate(1990, 5, 15, 8, 30));
            Check.equal("含姓名存檔後載入", withName, BaziFileIO.load(named));

            // 舊版檔案：沒有 gender 與 appVersion，性別預設為男
            File legacy = new File(dir, "legacy.bazi");
            Files.writeString(legacy.toPath(), "year=1984\nmonth=2\nday=5\nhour=10\nminute=0\n", StandardCharsets.UTF_8);
            Check.equal("載入舊版檔案", new BirthInput(1984, 2, 5, 10, 0, Gender.MALE), BaziFileIO.load(legacy));

            File broken = new File(dir, "broken.bazi");
            Files.writeString(broken.toPath(), "year=abc\n", StandardCharsets.UTF_8);
            boolean rejected = false;
            try {
                BaziFileIO.load(broken);
            } catch (IOException expected) {
                rejected = true;
            }
            Check.isTrue("拒絕格式錯誤的檔案", rejected, "應拋出 IOException");
        } finally {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }
    }

    /** 人員資料庫：新增、同名取代、搜尋、刪除、姓名清理、略過格式錯誤的行。 */
    static void personStore() throws IOException {
        java.nio.file.Path dir = Files.createTempDirectory("bazi-people");
        java.nio.file.Path file = dir.resolve("sub").resolve("people.tsv");
        try {
            PersonStore store = new PersonStore(file);
            Check.equal("檔案不存在時為空清單", 0, store.list().size());

            BirthInput a = new BirthInput(1990, 5, 15, 8, 30, Gender.FEMALE, "王小明");
            BirthInput b = new BirthInput(1984, 2, 29, 23, 5, Gender.MALE, "Tom Lee");
            Check.equal("新增第一筆", false, store.put(a));
            Check.equal("新增第二筆", false, store.put(b));
            Check.equal("兩筆資料", java.util.List.of(a, b), store.list());
            Check.equal("依姓名尋找", b, store.find("  tom lee "));
            Check.equal("找不到時為 null", null, store.find("不存在"));

            BirthInput a2 = new BirthInput(1991, 6, 16, 9, 0, Gender.FEMALE, "王小明");
            Check.equal("同名取代", true, store.put(a2));
            Check.equal("取代後順序不變", java.util.List.of(a2, b), store.list());

            BirthInput tricky = new BirthInput(2000, 1, 1, 0, 0, Gender.MALE, " 陳\t大\n文 ");
            Check.equal("姓名去除控制字元", "陳 大 文", tricky.name());
            store.put(tricky);
            Check.equal("含特殊字元姓名可讀回", tricky, store.find("陳 大 文"));

            Check.equal("刪除", true, store.remove("Tom Lee"));
            Check.equal("刪除不存在者", false, store.remove("Tom Lee"));
            Check.equal("刪除後剩兩筆", 2, store.list().size());

            boolean rejected = false;
            try {
                store.put(new BirthInput(2000, 1, 1, 0, 0, Gender.MALE));
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            Check.isTrue("拒絕無姓名的人員", rejected, "應拋出 IllegalArgumentException");

            // 手動損毀的行（欄位不足、數字錯誤、日期不存在、性別錯誤）應略過，其餘照常讀取
            Files.writeString(file, "# 註解\n張三\t1980\t1\t2\t3\t4\tMALE\n壞\t1980\n壞2\tabc\t1\t1\t1\t1\tMALE\n"
                    + "壞3\t1981\t2\t30\t1\t1\tMALE\n壞4\t1980\t1\t1\t1\t1\tX\n", StandardCharsets.UTF_8);
            Check.equal("略過格式錯誤的行", java.util.List.of(new BirthInput(1980, 1, 2, 3, 4, Gender.MALE, "張三")),
                    store.list());
        } finally {
            try (var paths = Files.walk(dir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
