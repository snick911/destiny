package com.destiny.bazi.tests;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.HiddenStems;
import com.destiny.bazi.core.LiuYueMonth;
import com.destiny.bazi.core.SolarTerm;
import com.destiny.bazi.core.TenGod;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 曆法相關測試：節氣時刻、四柱、傳統口訣表、十神與藏干表、流年流月。
 * 預期值一律直接寫出（官方公布時刻、公開八字、口訣），不由程式邏輯推導。
 */
final class CalendarTests {

    private static final BaziCalculator CALC = new BaziCalculator();

    private CalendarTests() {
    }

    // {西元年, 節氣索引, 官方公布時刻（北京時間，取至分鐘）}：香港天文台等官方公布值。
    private static final Object[][] OFFICIAL_TERMS = {
            {2024, 2, "2024-02-04T16:27"}, {2024, 4, "2024-03-05T10:23"}, {2024, 0, "2024-01-06T04:49"},
            {2024, 5, "2024-03-20T11:06"}, {2024, 22, "2024-12-06T23:17"}, {2024, 23, "2024-12-21T17:21"},
            {2024, 11, "2024-06-21T04:51"}, {2024, 17, "2024-09-22T20:44"}, {2023, 2, "2023-02-04T10:42"},
            {2025, 2, "2025-02-03T22:10"}, {2000, 2, "2000-02-04T20:40"}, {1990, 2, "1990-02-04T10:14"},
            {2000, 5, "2000-03-20T15:35"}, {2000, 11, "2000-06-21T09:48"}, {2000, 23, "2000-12-21T21:37"},
            {1990, 5, "1990-03-21T05:19"}, {2010, 23, "2010-12-22T07:38"}, {2020, 11, "2020-06-21T05:43"},
            {2020, 23, "2020-12-21T18:02"}, {2021, 2, "2021-02-03T22:59"}, {2022, 2, "2022-02-04T04:51"},
            {2019, 2, "2019-02-04T11:14"}, {2018, 2, "2018-02-04T05:28"}, {2017, 2, "2017-02-03T23:34"},
            {2016, 2, "2016-02-04T17:46"}, {2008, 2, "2008-02-04T19:00"},
    };

    static void solarTerms() {
        for (Object[] row : OFFICIAL_TERMS) {
            int year = (int) row[0];
            int term = (int) row[1];
            LocalDateTime official = LocalDateTime.parse((String) row[2]);
            LocalDateTime computed = SolarTerm.getTime(year, term);
            double diffMinutes = Duration.between(official, computed).toSeconds() / 60.0;
            Check.isTrue(year + " " + SolarTerm.NAMES[term], Math.abs(diffMinutes) <= 1.0,
                    "與官方時刻 " + official + " 相差 " + diffMinutes + " 分鐘");
        }

        // 1900~2100 每個節氣依序出現、間隔合理，且落在當年。
        LocalDateTime previous = null;
        for (int year = 1900; year <= 2100; year++) {
            for (int term = 0; term < 24; term++) {
                LocalDateTime t = SolarTerm.getTime(year, term);
                Check.equal(year + " " + SolarTerm.NAMES[term] + " 年份", year, t.getYear());
                if (previous != null) {
                    double gapDays = Duration.between(previous, t).toMinutes() / 1440.0;
                    Check.isTrue(year + " " + SolarTerm.NAMES[term] + " 間隔", gapDays > 14.5 && gapDays < 16,
                            "與前一節氣相隔 " + gapDays + " 天");
                }
                previous = t;
            }
        }

        // ΔT 在歷史年份不可發散：1600~2200 年立春都應在 2 月 3~5 日。
        for (int year = 1600; year <= 2200; year++) {
            LocalDateTime lichun = SolarTerm.getTime(year, SolarTerm.LICHUN);
            Check.isTrue(year + " 立春日期", lichun.getMonthValue() == 2
                    && lichun.getDayOfMonth() >= 3 && lichun.getDayOfMonth() <= 5, "立春為 " + lichun);
        }
    }

    static void pillars() {
        // 公開流傳的名人八字
        chart("毛澤東", 1893, 12, 26, 8, 0, "癸巳 甲子 丁酉 甲辰");
        chart("蔣介石", 1887, 10, 31, 12, 0, "丁亥 庚戌 己巳 庚午");
        chart("孫中山", 1866, 11, 12, 4, 0, "丙寅 己亥 辛卯 庚寅");
        chart("袁世凱", 1859, 9, 16, 14, 0, "己未 癸酉 丁巳 丁未");
        chart("李小龍", 1940, 11, 27, 8, 0, "庚辰 丁亥 甲戌 戊辰");
        chart("乾隆", 1711, 9, 25, 0, 30, "辛卯 丁酉 庚午 丙子");

        // 立春交界前後一分鐘（官方：2024-02-04 16:27、2023-02-04 10:42）
        chart("2024 立春前", 2024, 2, 4, 16, 26, "癸卯 乙丑 戊戌 庚申");
        chart("2024 立春後", 2024, 2, 4, 16, 28, "甲辰 丙寅 戊戌 庚申");
        chart("2023 立春前", 2023, 2, 4, 10, 41, "壬寅 癸丑 癸巳 丁巳");
        chart("2023 立春後", 2023, 2, 4, 10, 43, "癸卯 甲寅 癸巳 丁巳");

        // 日柱錨點：1949-10-01 為甲子日、2000-01-01 為戊午日
        Check.equal("1949-10-01 日柱", "甲子", CALC.calculate(1949, 10, 1, 12, 0).getDayPillar().getGanZhi());
        Check.equal("2000-01-01 日柱", "戊午", CALC.calculate(2000, 1, 1, 12, 0).getDayPillar().getGanZhi());
        // 年柱錨點：1984 為甲子年
        Check.equal("1984 年柱", "甲子", CALC.calculate(1984, 6, 1, 12, 0).getYearPillar().getGanZhi());

        // 23 時起日柱換至次日
        BaziResult late = CALC.calculate(2024, 4, 9, 23, 30);
        Check.equal("23 時日柱", "甲辰", late.getDayPillar().getGanZhi());
        Check.equal("23 時時柱", "甲子", late.getHourPillar().getGanZhi());
        Check.isTrue("23 時換日旗標", late.isEarlyZiShift(), "應標示為已換日");
        BaziResult early = CALC.calculate(2024, 4, 10, 0, 30);
        Check.equal("0 時日柱", "甲辰", early.getDayPillar().getGanZhi());
        Check.isTrue("0 時不換日", !early.isEarlyZiShift(), "0 時不應換日");

        // 時辰：23~0 子、1~2 丑、3~4 寅……21~22 亥
        String hourBranches = "子丑丑寅寅卯卯辰辰巳巳午午未未申申酉酉戌戌亥亥子";
        for (int hour = 0; hour < 24; hour++) {
            Check.equal(hour + " 時地支", String.valueOf(hourBranches.charAt(hour)),
                    CALC.calculate(2024, 6, 15, hour, 30).getHourPillar().getBranch());
        }

        // 五虎遁：甲己之年丙作首，乙庚之歲戊為頭，丙辛必定尋庚起，丁壬壬位順行流，戊癸甲寅
        String wuHu = "丙戊庚壬甲丙戊庚壬甲";
        for (int i = 0; i < 10; i++) {
            BaziResult r = CALC.calculate(1984 + i, 2, 20, 12, 0); // 寅月
            Check.equal("五虎遁 " + r.getYearPillar().getStem() + "年寅月",
                    wuHu.charAt(r.getYearPillar().getStemIndex()) + "寅", r.getMonthPillar().getGanZhi());
        }
        // 五鼠遁：甲己還加甲，乙庚丙作初，丙辛從戊起，丁壬庚子居，戊癸壬子
        String wuShu = "甲丙戊庚壬甲丙戊庚壬";
        for (int i = 0; i < 10; i++) {
            BaziResult r = CALC.calculate(2024, 1, 1 + i, 0, 30);
            Check.equal("五鼠遁 " + r.getDayPillar().getStem() + "日子時",
                    wuShu.charAt(r.getDayPillar().getStemIndex()) + "子", r.getHourPillar().getGanZhi());
        }
    }

    private static void chart(String name, int y, int m, int d, int h, int mi, String expected) {
        BaziResult r = CALC.calculate(y, m, d, h, mi);
        String actual = r.getYearPillar() + " " + r.getMonthPillar() + " " + r.getDayPillar() + " " + r.getHourPillar();
        Check.equal(name + " 八字", expected, actual);
    }

    static void tenGodsAndHiddenStems() {
        // 十神表：日干（列）見甲~癸（欄）
        String[] table = {
                "比肩 劫財 食神 傷官 偏財 正財 七殺 正官 偏印 正印",
                "劫財 比肩 傷官 食神 正財 偏財 正官 七殺 正印 偏印",
                "偏印 正印 比肩 劫財 食神 傷官 偏財 正財 七殺 正官",
                "正印 偏印 劫財 比肩 傷官 食神 正財 偏財 正官 七殺",
                "七殺 正官 偏印 正印 比肩 劫財 食神 傷官 偏財 正財",
                "正官 七殺 正印 偏印 劫財 比肩 傷官 食神 正財 偏財",
                "偏財 正財 七殺 正官 偏印 正印 比肩 劫財 食神 傷官",
                "正財 偏財 正官 七殺 正印 偏印 劫財 比肩 傷官 食神",
                "食神 傷官 偏財 正財 七殺 正官 偏印 正印 比肩 劫財",
                "傷官 食神 正財 偏財 正官 七殺 正印 偏印 劫財 比肩",
        };
        for (int day = 0; day < 10; day++) {
            String[] row = table[day].split(" ");
            for (int target = 0; target < 10; target++) {
                Check.equal(GanZhi.STEMS[day] + "日見" + GanZhi.STEMS[target], row[target], TenGod.of(day, target));
            }
        }

        // 地支藏干（本氣在前）
        String[] hidden = {"癸", "己癸辛", "甲丙戊", "乙", "戊乙癸", "丙庚戊", "丁己", "己丁乙", "庚壬戊", "辛", "戊辛丁", "壬甲"};
        for (int branch = 0; branch < 12; branch++) {
            StringBuilder actual = new StringBuilder();
            for (int stem : HiddenStems.of(branch)) {
                actual.append(GanZhi.STEMS[stem]);
            }
            Check.equal(GanZhi.BRANCHES[branch] + " 藏干", hidden[branch], actual.toString());
        }
    }

    static void liuNianLiuYue() {
        Check.equal("2024 流年", "甲辰", CALC.calculateLiuNian(2024, 0).getPillar().getGanZhi());
        Check.equal("2025 流年", "乙巳", CALC.calculateLiuNian(2025, 0).getPillar().getGanZhi());
        Check.equal("1984 流年", "甲子", CALC.calculateLiuNian(1984, 0).getPillar().getGanZhi());
        String[] months2024 = {"丙寅", "丁卯", "戊辰", "己巳", "庚午", "辛未", "壬申", "癸酉", "甲戌", "乙亥", "丙子", "丁丑"};
        for (int m = 0; m < 12; m++) {
            Check.equal("2024 " + LiuYueMonth.MONTH_NAMES[m], months2024[m],
                    CALC.calculateLiuYue(2024, m, 0).getPillar().getGanZhi());
        }
    }
}
