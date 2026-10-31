package com.destiny.bazi.tests;

import com.destiny.bazi.AppInfo;
import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.io.BaziFileIO;
import com.destiny.bazi.io.BirthInput;

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
}
