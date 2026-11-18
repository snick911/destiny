package com.destiny.bazi.ziwei;

import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.LunarCalendar;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 紫微斗數本命盤：十二宮（以地支索引 子=0 … 亥=11 存放）的宮干、宮名與星曜，
 * 生年四化、五行局、命身宮、命主身主與十二步大限。
 */
public final class ZiWeiChart {

    /** 十二宮名稱，索引 0 為命宮，自命宮起逆時針（地支遞減）依序排列。 */
    public static final String[] PALACE_NAMES = {
            "命宮", "兄弟宮", "夫妻宮", "子女宮", "財帛宮", "疾厄宮",
            "遷移宮", "交友宮", "事業宮", "田宅宮", "福德宮", "父母宮"
    };

    /** 十二宮簡稱，用於大限／流年／流月宮位標示（例如「大命」「流財」）。 */
    public static final String[] PALACE_SHORT_NAMES = {
            "命", "兄", "夫", "子", "財", "疾", "遷", "友", "事", "田", "福", "父"
    };

    private final LocalDateTime birth;
    private final Gender gender;
    private final LunarCalendar.LunarDate lunarDate;
    private final boolean lateZiShift;
    private final int chartMonth;
    private final int hourBranch;
    private final boolean forward;
    private final int lifeBranch;
    private final int bodyBranch;
    private final FiveElementBureau bureau;
    private final int[] palaceStems;
    private final List<List<Star>> stars;
    private final Map<Star, Integer> starBranches;
    private final Map<Star, SiHua> natalSiHua;
    private final List<Decade> decades;
    private final Star lifeMaster;
    private final Star bodyMaster;
    private final int smallLimitStartBranch;

    ZiWeiChart(LocalDateTime birth, Gender gender, LunarCalendar.LunarDate lunarDate, boolean lateZiShift,
               int chartMonth, int hourBranch, boolean forward, int lifeBranch, int bodyBranch,
               FiveElementBureau bureau, int[] palaceStems, List<List<Star>> stars, Map<Star, Integer> starBranches,
               Map<Star, SiHua> natalSiHua, List<Decade> decades, Star lifeMaster, Star bodyMaster,
               int smallLimitStartBranch) {
        this.birth = birth;
        this.gender = gender;
        this.lunarDate = lunarDate;
        this.lateZiShift = lateZiShift;
        this.chartMonth = chartMonth;
        this.hourBranch = hourBranch;
        this.forward = forward;
        this.lifeBranch = lifeBranch;
        this.bodyBranch = bodyBranch;
        this.bureau = bureau;
        this.palaceStems = palaceStems.clone();
        this.stars = stars.stream().map(List::copyOf).toList();
        this.starBranches = Collections.unmodifiableMap(new EnumMap<>(starBranches));
        this.natalSiHua = Collections.unmodifiableMap(new EnumMap<>(natalSiHua));
        this.decades = List.copyOf(decades);
        this.lifeMaster = lifeMaster;
        this.bodyMaster = bodyMaster;
        this.smallLimitStartBranch = smallLimitStartBranch;
    }

    /** 輸入的出生時刻（西曆，北京時間）。 */
    public LocalDateTime getBirth() {
        return birth;
    }

    public Gender getGender() {
        return gender;
    }

    /** 出生的農曆日期（23 時出生者已順延至次日）。 */
    public LunarCalendar.LunarDate getLunarDate() {
        return lunarDate;
    }

    /** 是否因 23 時屬「晚子時」而以次日的農曆日期排盤。 */
    public boolean isLateZiShift() {
        return lateZiShift;
    }

    /** 排盤所用的月份（1~12）：閏月十五日以前屬本月，十六日起屬下個月。 */
    public int getChartMonth() {
        return chartMonth;
    }

    /** 閏月十六日以後出生、以下個月排盤。 */
    public boolean isLeapMonthAsNext() {
        return lunarDate.leap() && chartMonth != lunarDate.month();
    }

    /** 生時地支索引（子=0）。 */
    public int getHourBranch() {
        return hourBranch;
    }

    public int getYearStem() {
        return lunarDate.yearStemIndex();
    }

    public int getYearBranch() {
        return lunarDate.yearBranchIndex();
    }

    public boolean isYangYear() {
        return getYearStem() % 2 == 0;
    }

    /** 例如「陽男」「陰女」。 */
    public String getYinYangGender() {
        return (isYangYear() ? "陽" : "陰") + gender.getLabel();
    }

    /** 大限是否順行（陽男陰女順行、陰男陽女逆行）。 */
    public boolean isForward() {
        return forward;
    }

    public int getLifeBranch() {
        return lifeBranch;
    }

    public int getBodyBranch() {
        return bodyBranch;
    }

    public FiveElementBureau getBureau() {
        return bureau;
    }

    /** 地支 branch 所在宮的宮干索引（甲=0）。 */
    public int palaceStem(int branch) {
        return palaceStems[branch];
    }

    public String palaceGanZhi(int branch) {
        return GanZhi.STEMS[palaceStems[branch]] + GanZhi.BRANCHES[branch];
    }

    /** 地支 branch 所在宮的宮名索引（0=命宮 … 11=父母宮）。 */
    public int palaceIndex(int branch) {
        return GanZhi.mod(lifeBranch - branch, 12);
    }

    public String palaceName(int branch) {
        return PALACE_NAMES[palaceIndex(branch)];
    }

    /** 宮名索引（0=命宮）所在的地支。 */
    public int branchOfPalace(int palaceIndex) {
        return GanZhi.mod(lifeBranch - palaceIndex, 12);
    }

    /** 地支 branch 所在宮的本命星曜（主星在前）。 */
    public List<Star> starsAt(int branch) {
        return stars.get(branch);
    }

    /** 星曜所在的地支索引。 */
    public int branchOf(Star star) {
        return starBranches.get(star);
    }

    /** 生年四化（依出生年干）。 */
    public Map<Star, SiHua> getNatalSiHua() {
        return natalSiHua;
    }

    /** 十二步大限，依年齡先後排列。 */
    public List<Decade> getDecades() {
        return decades;
    }

    public Star getLifeMaster() {
        return lifeMaster;
    }

    public Star getBodyMaster() {
        return bodyMaster;
    }

    /** 某農曆年的虛歲（出生當年為 1 歲）。 */
    public int ageInYear(int lunarYear) {
        return lunarYear - lunarDate.year() + 1;
    }

    /** 某虛歲所在大限的索引；未起限前為第一限，超過十二限後為最後一限。 */
    public int decadeIndexForAge(int age) {
        for (Decade d : decades) {
            if (d.containsAge(age)) {
                return d.index();
            }
        }
        return age < decades.get(0).startAge() ? 0 : decades.size() - 1;
    }

    /** 地支 branch 所在宮的大限。 */
    public Decade decadeAt(int branch) {
        for (Decade d : decades) {
            if (d.branch() == branch) {
                return d;
            }
        }
        throw new IllegalArgumentException("沒有地支 " + branch + " 的大限");
    }

    /** 某虛歲的小限宮位：依生年支起一歲，男命順行、女命逆行。 */
    public int smallLimitBranch(int age) {
        int step = gender == Gender.MALE ? 1 : -1;
        return GanZhi.mod(smallLimitStartBranch + step * (age - 1), 12);
    }
}
