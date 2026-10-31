package com.destiny.bazi.tests;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.core.DaYunPeriod;
import com.destiny.bazi.core.DaYunResult;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.HiddenStems;
import com.destiny.bazi.core.LiuNianYear;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 大運（順逆、起運、交運日期與各期年份）以及五行質量、十神統計的測試。 */
final class DaYunTests {

    private static final BaziCalculator CALC = new BaziCalculator();

    private DaYunTests() {
    }

    static void run() {
        // 陽男陰女順排、陰男陽女逆排（1984 甲子年為陽、1985 乙丑年為陰）
        Check.isTrue("陽男順排", dayun(1984, 6, 1, 12, 0, Gender.MALE).isForward(), "應為順排");
        Check.isTrue("陽女逆排", !dayun(1984, 6, 1, 12, 0, Gender.FEMALE).isForward(), "應為逆排");
        Check.isTrue("陰男逆排", !dayun(1985, 6, 1, 12, 0, Gender.MALE).isForward(), "應為逆排");
        Check.isTrue("陰女順排", dayun(1985, 6, 1, 12, 0, Gender.FEMALE).isForward(), "應為順排");

        // 回報的錯誤案例：起運的月數跨年，交運應在 1959 年而非 1958 年
        DaYunResult d = dayun(1950, 9, 4, 4, 31, Gender.FEMALE);
        Check.equal("1950 案例起運", "8歲10個月28天",
                d.getStartAgeYears() + "歲" + d.getStartAgeMonths() + "個月" + d.getStartAgeDays() + "天");
        Check.equal("1950 案例交運日期", LocalDate.of(1959, 8, 1), d.getStartDate());
        StringBuilder periods = new StringBuilder();
        for (DaYunPeriod p : d.getPeriods()) {
            periods.append(p.getPillar()).append(p.getStartYear()).append('~').append(p.getEndYear())
                    .append('(').append(p.getStartAge()).append('~').append(p.getEndAge()).append(") ");
        }
        Check.equal("1950 案例各期大運",
                "癸未1959~1968(9~18) 壬午1969~1978(19~28) 辛巳1979~1988(29~38) 庚辰1989~1998(39~48) "
                        + "己卯1999~2008(49~58) 戊寅2009~2018(59~68) 丁丑2019~2028(69~78) 丙子2029~2038(79~88)",
                periods.toString().trim());

        // 出生於立春前 27 分鐘的女命（癸卯年陰女順排）：2 天後交運
        DaYunResult near = dayun(2024, 2, 4, 16, 0, Gender.FEMALE);
        Check.isTrue("立春前出生順排", near.isForward(), "應為順排");
        Check.equal("立春前出生起運", "0歲0個月2天",
                near.getStartAgeYears() + "歲" + near.getStartAgeMonths() + "個月" + near.getStartAgeDays() + "天");
        Check.equal("立春前出生交運日期", LocalDate.of(2024, 2, 6), near.getStartDate());
        Check.equal("立春前出生首運年份", 2024, near.getPeriods().get(0).getStartYear());

        // 隨機出生時間的不變性質
        Random random = new Random(20261031L);
        for (int i = 0; i < 5000; i++) {
            LocalDateTime birth = LocalDateTime.of(1900, 1, 1, 0, 0)
                    .plusMinutes((long) (random.nextDouble() * 200 * 525960));
            Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
            DaYunResult r = dayun(birth.getYear(), birth.getMonthValue(), birth.getDayOfMonth(),
                    birth.getHour(), birth.getMinute(), gender);
            String tag = birth + " " + gender;

            Check.isTrue(tag + " 起運範圍", r.getStartAgeYears() >= 0 && r.getStartAgeYears() <= 10
                    && r.getStartAgeMonths() >= 0 && r.getStartAgeMonths() < 12
                    && r.getStartAgeDays() >= 0 && r.getStartAgeDays() < 30,
                    r.getStartAgeYears() + "/" + r.getStartAgeMonths() + "/" + r.getStartAgeDays());
            LocalDate expectedStart = birth.toLocalDate().plusYears(r.getStartAgeYears())
                    .plusMonths(r.getStartAgeMonths()).plusDays(r.getStartAgeDays());
            Check.equal(tag + " 交運日期", expectedStart, r.getStartDate());

            List<DaYunPeriod> list = r.getPeriods();
            Check.equal(tag + " 期數", BaziCalculator.DAYUN_PERIOD_COUNT, list.size());
            Check.equal(tag + " 首運年份＝交運年份", r.getStartDate().getYear(), list.get(0).getStartYear());
            for (int k = 0; k < list.size(); k++) {
                DaYunPeriod p = list.get(k);
                Check.equal(tag + " 第" + (k + 1) + "運長度", p.getStartYear() + 9, p.getEndYear());
                Check.equal(tag + " 第" + (k + 1) + "運歲數", p.getStartYear() - birth.getYear(), p.getStartAge());
                if (k > 0) {
                    Check.equal(tag + " 第" + (k + 1) + "運銜接", list.get(k - 1).getEndYear() + 1, p.getStartYear());
                }
            }
        }
    }

    static void massAndCounts() {
        Random random = new Random(7L);
        for (int i = 0; i < 2000; i++) {
            LocalDateTime birth = LocalDateTime.of(1900, 1, 1, 0, 0)
                    .plusMinutes((long) (random.nextDouble() * 200 * 525960));
            BaziResult result = CALC.calculate(birth.getYear(), birth.getMonthValue(), birth.getDayOfMonth(),
                    birth.getHour(), birth.getMinute());
            DaYunResult d = CALC.calculateDaYun(result, birth.getYear(), birth.getMonthValue(),
                    birth.getDayOfMonth(), birth.getHour(), birth.getMinute(), Gender.MALE);
            DaYunPeriod period = d.getPeriods().get(random.nextInt(d.getPeriods().size()));
            int year = period.getStartYear() + random.nextInt(10);
            LiuNianYear liuNian = CALC.calculateLiuNian(year, result.getDayPillar().getStemIndex());

            double base = sum(CALC.calculateFiveElementMass(result).asMap());
            double luck = sum(CALC.calculateFiveElementMassWithLuck(result, period.getPillar(), liuNian.getPillar()).asMap());
            Check.isTrue(birth + " 五行質量總和", Math.abs(base - 13) < 1e-9, "總和為 " + base);
            Check.isTrue(birth + " 含運五行質量總和", Math.abs(luck - 17) < 1e-9, "總和為 " + luck);

            int hidden = HiddenStems.of(result.getYearPillar().getBranchIndex()).length
                    + HiddenStems.of(result.getMonthPillar().getBranchIndex()).length
                    + HiddenStems.of(result.getDayPillar().getBranchIndex()).length
                    + HiddenStems.of(result.getHourPillar().getBranchIndex()).length;
            int luckHidden = HiddenStems.of(period.getPillar().getBranchIndex()).length
                    + HiddenStems.of(liuNian.getPillar().getBranchIndex()).length;
            Check.equal(birth + " 十神次數", 3 + hidden, count(CALC.calculateTenGodCounts(result)));
            Check.equal(birth + " 含運十神次數", 5 + hidden + luckHidden, count(CALC.calculateTenGodCountsWithLuck(
                    result, period.getTenGods(), liuNian.getTenGods())));
        }
    }

    private static DaYunResult dayun(int y, int m, int d, int h, int mi, Gender gender) {
        return CALC.calculateDaYun(CALC.calculate(y, m, d, h, mi), y, m, d, h, mi, gender);
    }

    private static double sum(Map<String, Double> values) {
        return values.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static int count(Map<String, Integer> values) {
        return values.values().stream().mapToInt(Integer::intValue).sum();
    }
}
