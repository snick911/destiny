package com.destiny.bazi.io;

import com.destiny.bazi.core.Gender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 人員資料庫：以姓名為鍵，儲存多位人員的出生資料，存放於單一 UTF-8 純文字檔
 * （每行一筆，欄位以 Tab 分隔：姓名、年、月、日、時、分、性別）。
 *
 * <p>預設位置為使用者家目錄下的 {@code .destiny-bazi/people.tsv}；可用系統屬性
 * {@value #DATA_DIR_PROPERTY} 指定其他資料夾（測試即以此避免動到真實資料）。
 * 寫入時先寫暫存檔再取代原檔，避免寫到一半當機造成資料毀損。格式錯誤的行會略過，
 * 不影響其他資料讀取。</p>
 */
public final class PersonStore {

    public static final String DATA_DIR_PROPERTY = "destiny.dataDir";
    private static final String FILE_NAME = "people.tsv";
    private static final String HEADER = "# 子平八字排盤 人員資料（姓名\t年\t月\t日\t時\t分\t性別）";

    private final Path file;

    public PersonStore(Path file) {
        this.file = file;
    }

    /** 依系統屬性或家目錄決定的預設人員資料檔。 */
    public static PersonStore openDefault() {
        String dir = System.getProperty(DATA_DIR_PROPERTY);
        Path base = dir != null && !dir.isBlank()
                ? Path.of(dir)
                : Path.of(System.getProperty("user.home"), ".destiny-bazi");
        return new PersonStore(base.resolve(FILE_NAME));
    }

    public Path getFile() {
        return file;
    }

    /** 讀取全部人員（依檔案中的順序）；檔案不存在時回傳空清單。 */
    public List<BirthInput> list() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<BirthInput> people = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            BirthInput person = parse(line);
            if (person != null) {
                people.add(person);
            }
        }
        return people;
    }

    /** 依姓名（不分大小寫）尋找人員，找不到時回傳 null。 */
    public BirthInput find(String name) throws IOException {
        String key = BirthInput.normalizeName(name);
        for (BirthInput p : list()) {
            if (sameName(p.name(), key)) {
                return p;
            }
        }
        return null;
    }

    /**
     * 新增或更新一位人員（同名者就地取代，否則加在最後）。
     *
     * @return true 表示取代了既有的同名資料
     */
    public boolean put(BirthInput person) throws IOException {
        if (!person.hasName()) {
            throw new IllegalArgumentException("人員資料必須有姓名");
        }
        List<BirthInput> people = list();
        for (int i = 0; i < people.size(); i++) {
            if (sameName(people.get(i).name(), person.name())) {
                people.set(i, person);
                write(people);
                return true;
            }
        }
        people.add(person);
        write(people);
        return false;
    }

    /**
     * 一次新增或更新多位人員（同名者就地取代，其餘依序加在最後），只寫檔一次。
     *
     * @return {新增筆數, 取代筆數}
     */
    public int[] putAll(List<BirthInput> persons) throws IOException {
        List<BirthInput> people = list();
        int added = 0;
        int replaced = 0;
        for (BirthInput person : persons) {
            if (!person.hasName()) {
                throw new IllegalArgumentException("人員資料必須有姓名");
            }
            int index = -1;
            for (int i = 0; i < people.size(); i++) {
                if (sameName(people.get(i).name(), person.name())) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                people.set(index, person);
                replaced++;
            } else {
                people.add(person);
                added++;
            }
        }
        if (added + replaced > 0) {
            write(people);
        }
        return new int[]{added, replaced};
    }

    /** 刪除指定姓名的人員，回傳是否確實刪除。 */
    public boolean remove(String name) throws IOException {
        List<BirthInput> people = list();
        boolean removed = people.removeIf(p -> sameName(p.name(), BirthInput.normalizeName(name)));
        if (removed) {
            write(people);
        }
        return removed;
    }

    private static boolean sameName(String a, String b) {
        return a.toLowerCase(Locale.ROOT).equals(b.toLowerCase(Locale.ROOT));
    }

    private void write(List<BirthInput> people) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (BirthInput p : people) {
            lines.add(String.join("\t", p.name(),
                    String.valueOf(p.year()), String.valueOf(p.month()), String.valueOf(p.day()),
                    String.valueOf(p.hour()), String.valueOf(p.minute()), p.gender().name()));
        }
        Path tmp = Files.createTempFile(dir, "people", ".tmp");
        try {
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static BirthInput parse(String line) {
        if (line.isBlank() || line.startsWith("#")) {
            return null;
        }
        String[] f = line.split("\t", -1);
        if (f.length < 7) {
            return null;
        }
        try {
            BirthInput p = new BirthInput(
                    Integer.parseInt(f[1].trim()), Integer.parseInt(f[2].trim()), Integer.parseInt(f[3].trim()),
                    Integer.parseInt(f[4].trim()), Integer.parseInt(f[5].trim()),
                    Gender.valueOf(f[6].trim()), f[0]);
            if (!p.hasName() || p.year() < 1900 || p.year() > 2100 || p.month() < 1 || p.month() > 12
                    || p.day() < 1 || p.day() > YearMonth.of(p.year(), p.month()).lengthOfMonth()
                    || p.hour() < 0 || p.hour() > 23 || p.minute() < 0 || p.minute() > 59) {
                return null;
            }
            return p;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
