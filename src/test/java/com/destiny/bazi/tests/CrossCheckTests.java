package com.destiny.bazi.tests;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.DaYunPeriod;
import com.destiny.bazi.core.DaYunResult;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.HiddenStemGod;
import com.destiny.bazi.core.PillarTenGods;
import com.destiny.bazi.core.SolarTerm;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 以另一套獨立寫法重算整張命盤並與程式比對：干支以錨點日期（1984 甲子年、
 * 1949-10-01 甲子日）推算，月干、時干、十神、藏干一律以傳統口訣字串查表，
 * 不呼叫程式內任何排盤公式（節氣時刻除外，已另以官方時刻驗證）。
 */
final class CrossCheckTests {

    private static final String STEMS = "甲乙丙丁戊己庚辛壬癸";
    private static final String BRANCHES = "子丑寅卯辰巳午未申酉戌亥";
    private static final String WU_HU = "丙戊庚壬甲丙戊庚壬甲";   // 年干 → 寅月月干
    private static final String WU_SHU = "甲丙戊庚壬甲丙戊庚壬";  // 日干 → 子時時干
    private static final Map<String, Character> JIE_BRANCH = Map.ofEntries(
            Map.entry("立春", '寅'), Map.entry("驚蟄", '卯'), Map.entry("清明", '辰'), Map.entry("立夏", '巳'),
            Map.entry("芒種", '午'), Map.entry("小暑", '未'), Map.entry("立秋", '申'), Map.entry("白露", '酉'),
            Map.entry("寒露", '戌'), Map.entry("立冬", '亥'), Map.entry("大雪", '子'), Map.entry("小寒", '丑'));
    private static final int[] HOUR_STARTS = {23, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21}; // 子~亥
    private static final Map<Character, String> CANG = Map.ofEntries(
            Map.entry('子', "癸"), Map.entry('丑', "己癸辛"), Map.entry('寅', "甲丙戊"), Map.entry('卯', "乙"),
            Map.entry('辰', "戊乙癸"), Map.entry('巳', "丙庚戊"), Map.entry('午', "丁己"), Map.entry('未', "己丁乙"),
            Map.entry('申', "庚壬戊"), Map.entry('酉', "辛"), Map.entry('戌', "戊辛丁"), Map.entry('亥', "壬甲"));
    private static final String[] TEN_GODS = {
            "比肩劫財食神傷官偏財正財七殺正官偏印正印", "劫財比肩傷官食神正財偏財正官七殺正印偏印",
            "偏印正印比肩劫財食神傷官偏財正財七殺正官", "正印偏印劫財比肩傷官食神正財偏財正官七殺",
            "七殺正官偏印正印比肩劫財食神傷官偏財正財", "正官七殺正印偏印劫財比肩傷官食神正財偏財",
            "偏財正財七殺正官偏印正印比肩劫財食神傷官", "正財偏財正官七殺正印偏印劫財比肩傷官食神",
            "食神傷官偏財正財七殺正官偏印正印比肩劫財", "傷官食神正財偏財正官七殺正印偏印劫財比肩"};

    private static final BaziCalculator CALC = new BaziCalculator();

    private CrossCheckTests() {
    }

    static void run() {
        Random random = new Random(42L);
        for (int i = 0; i < 10000; i++) {
            LocalDateTime birth = LocalDateTime.of(1900, 1, 1, 0, 0)
                    .plusMinutes((long) (random.nextDouble() * 200 * 525960));
            Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
            compare(birth, gender);
        }
    }

    private static void compare(LocalDateTime b, Gender gender) {
        String tag = b + " " + gender;

        // ---- 獨立推算四柱 ----
        LocalDateTime lichun = SolarTerm.getTime(b.getYear(), SolarTerm.LICHUN);
        int baziYear = b.isBefore(lichun) ? b.getYear() - 1 : b.getYear();
        String yearGz = jiaZi(baziYear - 1984);

        LocalDateTime prevJie = null;
        String prevName = null;
        LocalDateTime nextJie = null;
        for (int y = b.getYear() - 1; y <= b.getYear() + 1; y++) {
            for (int t = 0; t < 24; t += 2) {
                LocalDateTime time = SolarTerm.getTime(y, t);
                if (!time.isAfter(b) && (prevJie == null || time.isAfter(prevJie))) {
                    prevJie = time;
                    prevName = SolarTerm.NAMES[t];
                }
                if (time.isAfter(b) && (nextJie == null || time.isBefore(nextJie))) {
                    nextJie = time;
                }
            }
        }
        char monthBranch = JIE_BRANCH.get(prevName);
        int steps = Math.floorMod(BRANCHES.indexOf(monthBranch) - BRANCHES.indexOf('寅'), 12);
        char monthStem = STEMS.charAt((STEMS.indexOf(WU_HU.charAt(STEMS.indexOf(yearGz.charAt(0)))) + steps) % 10);
        String monthGz = "" + monthStem + monthBranch;

        LocalDate dayDate = b.getHour() == 23 ? b.toLocalDate().plusDays(1) : b.toLocalDate();
        String dayGz = jiaZi(ChronoUnit.DAYS.between(LocalDate.of(1949, 10, 1), dayDate));

        int hourIdx = 0;
        for (int k = 0; k < 12; k++) {
            if (Math.floorMod(b.getHour() - HOUR_STARTS[k], 24) < 2) {
                hourIdx = k;
            }
        }
        char hourStem = STEMS.charAt((STEMS.indexOf(WU_SHU.charAt(STEMS.indexOf(dayGz.charAt(0)))) + hourIdx) % 10);
        String hourGz = "" + hourStem + BRANCHES.charAt(hourIdx);

        BaziResult r = CALC.calculate(b.getYear(), b.getMonthValue(), b.getDayOfMonth(), b.getHour(), b.getMinute());
        Check.equal(tag + " 四柱", yearGz + monthGz + dayGz + hourGz,
                r.getYearPillar().getGanZhi() + r.getMonthPillar().getGanZhi()
                        + r.getDayPillar().getGanZhi() + r.getHourPillar().getGanZhi());

        // ---- 十神與藏干 ----
        char dayStem = dayGz.charAt(0);
        String[] gzs = {yearGz, monthGz, dayGz, hourGz};
        PillarTenGods[] gods = {r.getYearTenGods(), r.getMonthTenGods(), r.getDayTenGods(), r.getHourTenGods()};
        for (int p = 0; p < 4; p++) {
            Check.equal(tag + " 天干十神" + p, p == 2 ? "日主" : tenGod(dayStem, gzs[p].charAt(0)), gods[p].getStemTenGod());
            StringBuilder expected = new StringBuilder();
            for (char c : CANG.get(gzs[p].charAt(1)).toCharArray()) {
                expected.append(c).append(tenGod(dayStem, c));
            }
            StringBuilder actual = new StringBuilder();
            for (HiddenStemGod h : gods[p].getBranchHiddenGods()) {
                actual.append(h.getStem()).append(h.getTenGod());
            }
            Check.equal(tag + " 藏干十神" + p, expected.toString(), actual.toString());
        }

        // ---- 大運：方向、起運、交運日期、各期干支與年份 ----
        boolean yang = STEMS.indexOf(yearGz.charAt(0)) % 2 == 0;
        boolean forward = yang == (gender == Gender.MALE);
        LocalDateTime target = forward ? nextJie : prevJie;
        long minutes = Math.abs(Duration.between(b, target).getSeconds()) / 60;
        double years = minutes / 1440.0 / 3.0;
        int y = (int) years;
        double monthsExact = (years - y) * 12;
        int m = (int) monthsExact;
        int d = (int) Math.floor((monthsExact - m) * 30 + 0.5);
        if (d >= 30) {
            d -= 30;
            m++;
        }
        if (m >= 12) {
            m -= 12;
            y++;
        }
        LocalDate startDate = b.toLocalDate().plusYears(y).plusMonths(m).plusDays(d);

        DaYunResult dy = CALC.calculateDaYun(r, b.getYear(), b.getMonthValue(), b.getDayOfMonth(),
                b.getHour(), b.getMinute(), gender);
        Check.equal(tag + " 大運方向", forward, dy.isForward());
        Check.equal(tag + " 起運", y + "/" + m + "/" + d,
                dy.getStartAgeYears() + "/" + dy.getStartAgeMonths() + "/" + dy.getStartAgeDays());
        Check.equal(tag + " 交運日期", startDate, dy.getStartDate());

        int monthIdx = jiaZiIndex(monthGz);
        List<DaYunPeriod> periods = dy.getPeriods();
        for (int k = 0; k < periods.size(); k++) {
            DaYunPeriod p = periods.get(k);
            String expected = jiaZi(monthIdx + (forward ? k + 1 : -(k + 1))) + " " + (startDate.getYear() + 10 * k);
            Check.equal(tag + " 第" + (k + 1) + "運", expected, p.getPillar().getGanZhi() + " " + p.getStartYear());
        }
    }

    private static String jiaZi(long index) {
        int i = (int) Math.floorMod(index, 60L);
        return "" + STEMS.charAt(i % 10) + BRANCHES.charAt(i % 12);
    }

    private static int jiaZiIndex(String gz) {
        for (int i = 0; i < 60; i++) {
            if (jiaZi(i).equals(gz)) {
                return i;
            }
        }
        throw new IllegalArgumentException(gz);
    }

    private static String tenGod(char dayStem, char target) {
        int col = STEMS.indexOf(target);
        return TEN_GODS[STEMS.indexOf(dayStem)].substring(col * 2, col * 2 + 2);
    }
}
