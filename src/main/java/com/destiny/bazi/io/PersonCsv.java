package com.destiny.bazi.io;

import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.LunarCalendar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 人員資料 CSV 匯入／匯出。
 *
 * <p>格式：第一列為表頭，欄位依表頭名稱對應（順序不拘）：</p>
 * <pre>
 * 姓名,曆法,年,月,日,閏月,時,分,性別
 * 王小明,國曆,1984,7,27,,9,30,男
 * 李小美,農曆,1985,3,7,否,2,0,女
 * </pre>
 * <ul>
 *   <li>必要欄位：姓名、年、月、日、時、性別。</li>
 *   <li>曆法：國曆（陽曆、西曆）或農曆（陰曆）；省略時為國曆。農曆會換算為國曆儲存。</li>
 *   <li>閏月：農曆閏月填「是」，其餘可留空或填「否」。</li>
 *   <li>時：0~23；分：0~59，可省略（視為 0）。性別：男／女（或 M／F）。</li>
 *   <li>沒有表頭時，依上方順序（姓名,曆法,年,月,日,閏月,時,分,性別）讀取。</li>
 *   <li>空白列與 # 開頭的列略過；欄位可用雙引號包住（內含逗號時）。</li>
 * </ul>
 *
 * <p>讀檔時自動判斷編碼：有 BOM 或內容為合法 UTF-8 即以 UTF-8 讀取，否則視為 Big5
 * （Windows 繁體中文版 Excel「CSV（逗號分隔）」的預設編碼）。匯出為 UTF-8 加 BOM，
 * Excel 可直接開啟。</p>
 */
public final class PersonCsv {

    public static final String[] COLUMNS = {"姓名", "曆法", "年", "月", "日", "閏月", "時", "分", "性別"};

    private static final int NAME = 0;
    private static final int CALENDAR = 1;
    private static final int YEAR = 2;
    private static final int MONTH = 3;
    private static final int DAY = 4;
    private static final int LEAP = 5;
    private static final int HOUR = 6;
    private static final int MINUTE = 7;
    private static final int GENDER = 8;

    private static final Map<String, Integer> HEADER_ALIASES = new HashMap<>();

    static {
        alias(NAME, "姓名", "名字", "name");
        alias(CALENDAR, "曆法", "历法", "日曆", "日历", "calendar");
        alias(YEAR, "年", "year");
        alias(MONTH, "月", "month");
        alias(DAY, "日", "day");
        alias(LEAP, "閏月", "闰月", "閏", "leap");
        alias(HOUR, "時", "时", "hour");
        alias(MINUTE, "分", "minute");
        alias(GENDER, "性別", "性别", "sex", "gender");
    }

    private static void alias(int column, String... names) {
        for (String n : names) {
            HEADER_ALIASES.put(n.toLowerCase(Locale.ROOT), column);
        }
    }

    /** 成功讀出的一筆人員（lineNumber 為 CSV 中的列號，從 1 起算）。 */
    public record Row(int lineNumber, BirthInput person) {
    }

    /** 無法讀取的一列與原因（lineNumber 為 0 表示整個檔案的問題）。 */
    public record Problem(int lineNumber, String message) {
        @Override
        public String toString() {
            return lineNumber > 0 ? "第 " + lineNumber + " 列：" + message : message;
        }
    }

    /** 讀取結果：成功的人員、問題列，以及判斷出的檔案編碼。 */
    public record Result(List<Row> rows, List<Problem> problems, Charset charset) {
        public List<BirthInput> people() {
            return rows.stream().map(Row::person).toList();
        }
    }

    private PersonCsv() {
    }

    // ------------------------------------------------------------------ 匯入

    /** 讀取 CSV 檔（自動判斷 UTF-8／Big5）。 */
    public static Result read(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        Charset charset = StandardCharsets.UTF_8;
        String text;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        } else {
            try {
                text = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException e) {
                charset = Charset.forName("MS950");
                text = new String(bytes, charset);
            }
        }
        Result parsed = parse(text);
        return new Result(parsed.rows(), parsed.problems(), charset);
    }

    /** 解析 CSV 文字。同一檔案中重複的姓名只取第一筆，其餘列為問題。 */
    public static Result parse(String text) {
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        List<Row> rows = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        int[] columns = null;
        Map<String, Integer> seenNames = new LinkedHashMap<>();

        for (Record record : records(text)) {
            List<String> fields = record.fields();
            if (isBlank(fields) || fields.get(0).trim().startsWith("#")) {
                continue;
            }
            if (columns == null) {
                int[] header = headerColumns(fields);
                if (header != null) {
                    String missing = missingRequired(header);
                    if (missing != null) {
                        problems.add(new Problem(record.line(), "表頭缺少必要欄位：" + missing));
                        return new Result(rows, problems, StandardCharsets.UTF_8);
                    }
                    columns = header;
                    continue;
                }
                columns = new int[COLUMNS.length];
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = i;
                }
            }
            try {
                BirthInput person = toPerson(fields, columns);
                String key = person.name().toLowerCase(Locale.ROOT);
                Integer first = seenNames.putIfAbsent(key, record.line());
                if (first != null) {
                    problems.add(new Problem(record.line(), "姓名「" + person.name() + "」與第 " + first + " 列重複，略過"));
                } else {
                    rows.add(new Row(record.line(), person));
                }
            } catch (IllegalArgumentException | DateTimeException e) {
                problems.add(new Problem(record.line(), e.getMessage()));
            }
        }
        return new Result(rows, problems, StandardCharsets.UTF_8);
    }

    /** 表頭列（至少含「姓名」）回傳各標準欄位在此檔的欄號（-1 表示沒有該欄）；不是表頭時回傳 null。 */
    private static int[] headerColumns(List<String> fields) {
        int[] columns = new int[COLUMNS.length];
        java.util.Arrays.fill(columns, -1);
        boolean any = false;
        for (int i = 0; i < fields.size(); i++) {
            Integer col = HEADER_ALIASES.get(fields.get(i).trim().toLowerCase(Locale.ROOT));
            if (col != null && columns[col] < 0) {
                columns[col] = i;
                any = true;
            }
        }
        return any && columns[NAME] >= 0 ? columns : null;
    }

    private static String missingRequired(int[] columns) {
        List<String> missing = new ArrayList<>();
        for (int col : new int[]{NAME, YEAR, MONTH, DAY, HOUR, GENDER}) {
            if (columns[col] < 0) {
                missing.add(COLUMNS[col]);
            }
        }
        return missing.isEmpty() ? null : String.join("、", missing);
    }

    private static BirthInput toPerson(List<String> fields, int[] columns) {
        String name = BirthInput.normalizeName(field(fields, columns[NAME]));
        if (name.isEmpty()) {
            throw new IllegalArgumentException("缺少姓名");
        }
        boolean lunar = parseCalendar(field(fields, columns[CALENDAR]));
        boolean leap = parseYesNo(field(fields, columns[LEAP]), "閏月");
        int year = parseInt(field(fields, columns[YEAR]), "年");
        int month = parseInt(field(fields, columns[MONTH]), "月");
        int day = parseInt(field(fields, columns[DAY]), "日");
        int hour = parseInt(field(fields, columns[HOUR]), "時");
        String minuteText = field(fields, columns[MINUTE]);
        int minute = minuteText.isEmpty() ? 0 : parseInt(minuteText, "分");
        Gender gender = parseGender(field(fields, columns[GENDER]));

        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("時應為 0~23，實際為 " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("分應為 0~59，實際為 " + minute);
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月應為 1~12，實際為 " + month);
        }
        LocalDate date;
        if (lunar) {
            if (year < 1900 || year > 2100) {
                throw new IllegalArgumentException("農曆年應在 1900~2100 之間，實際為 " + year);
            }
            date = LunarCalendar.toSolar(year, month, leap, day);
        } else {
            if (leap) {
                throw new IllegalArgumentException("國曆沒有閏月");
            }
            try {
                date = LocalDate.of(year, month, day);
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("國曆 " + year + "年" + month + "月沒有 " + day + " 日");
            }
        }
        if (date.getYear() < 1900 || date.getYear() > 2100) {
            throw new IllegalArgumentException("出生日期 " + date + " 超出 1900~2100 年");
        }
        return new BirthInput(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hour, minute, gender, name);
    }

    private static String field(List<String> fields, int column) {
        return column >= 0 && column < fields.size() ? fields.get(column).trim() : "";
    }

    private static int parseInt(String text, String label) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("缺少「" + label + "」");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("「" + label + "」不是數字：" + text);
        }
    }

    /** 回傳是否為農曆；空白視為國曆。 */
    private static boolean parseCalendar(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "", "國曆", "国历", "國", "陽曆", "阳历", "西曆", "西历", "公曆", "公历", "新曆", "solar", "sun", "s" -> false;
            case "農曆", "农历", "農", "陰曆", "阴历", "舊曆", "旧历", "lunar", "moon", "l" -> true;
            default -> throw new IllegalArgumentException("無法辨識的曆法：" + text + "（請填國曆或農曆）");
        };
    }

    private static boolean parseYesNo(String text, String label) {
        String t = text.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "", "否", "不是", "非", "n", "no", "0", "false" -> false;
            case "是", "閏", "闰", "y", "yes", "1", "true" -> true;
            default -> throw new IllegalArgumentException("「" + label + "」應填是或否：" + text);
        };
    }

    private static Gender parseGender(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "男", "m", "male", "乾", "男性" -> Gender.MALE;
            case "女", "f", "female", "坤", "女性" -> Gender.FEMALE;
            case "" -> throw new IllegalArgumentException("缺少「性別」");
            default -> throw new IllegalArgumentException("無法辨識的性別：" + text + "（請填男或女）");
        };
    }

    private static boolean isBlank(List<String> fields) {
        return fields.stream().allMatch(f -> f.trim().isEmpty());
    }

    // ------------------------------------------------------------------ CSV 斷列

    private record Record(int line, List<String> fields) {
    }

    /** 依 RFC 4180 拆解記錄：支援雙引號欄位（內含逗號、換行，"" 代表一個引號）。 */
    private static List<Record> records(String text) {
        List<Record> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int recordLine = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == '"' && field.toString().trim().isEmpty()) {
                field.setLength(0);
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                records.add(new Record(recordLine, fields));
                fields = new ArrayList<>();
                line++;
                recordLine = line;
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
            records.add(new Record(recordLine, fields));
        }
        return records;
    }

    // ------------------------------------------------------------------ 與現有資料比對

    /**
     * 匯入計畫：與人員資料比對後，分為新增、同名但出生資料不同（需決定是否覆蓋）、
     * 與現有資料完全相同（不必寫入）三類。
     */
    public record Plan(List<BirthInput> added, List<BirthInput> changed, List<BirthInput> unchanged) {
    }

    public static Plan plan(PersonStore store, Result result) throws IOException {
        Map<String, BirthInput> existing = new HashMap<>();
        for (BirthInput p : store.list()) {
            existing.put(p.name().toLowerCase(Locale.ROOT), p);
        }
        List<BirthInput> added = new ArrayList<>();
        List<BirthInput> changed = new ArrayList<>();
        List<BirthInput> unchanged = new ArrayList<>();
        for (BirthInput p : result.people()) {
            BirthInput old = existing.get(p.name().toLowerCase(Locale.ROOT));
            if (old == null) {
                added.add(p);
            } else if (old.sameBirth(p) && old.name().equals(p.name())) {
                unchanged.add(p);
            } else {
                changed.add(p);
            }
        }
        return new Plan(added, changed, unchanged);
    }

    // ------------------------------------------------------------------ 匯出

    /** 匯出為 CSV（UTF-8 加 BOM，日期一律為國曆），可再用 {@link #read} 匯入。 */
    public static void write(Path file, List<BirthInput> people) throws IOException {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(",", COLUMNS)).append("\r\n");
        for (BirthInput p : people) {
            sb.append(quote(p.name())).append(",國曆,")
                    .append(p.year()).append(',').append(p.month()).append(',').append(p.day()).append(",,")
                    .append(p.hour()).append(',').append(p.minute()).append(',')
                    .append(p.gender().getLabel()).append("\r\n");
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
