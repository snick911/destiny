package com.destiny.bazi.ziwei;

import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.LunarCalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 紫微斗數排盤核心，安星規則參考 lifeup 並依《紫微斗數全書》通行口訣修正。
 * 地支索引一律為 子=0、丑=1、寅=2 … 亥=11；「順」為地支遞增方向。
 *
 * <ul>
 *   <li>以農曆年月日時排盤：年以正月初一為界；23 時屬「晚子時」，以次日排盤
 *       （與八字的日柱換日一致）；閏月十五日以前屬本月、十六日起屬下個月。</li>
 *   <li>命宮：寅宮起正月順數至生月，再起子時逆數至生時；身宮：同樣起至生月後
 *       順數至生時。十二宮自命宮起逆排。宮干依年干以五虎遁起寅宮。</li>
 *   <li>五行局：命宮干支的納音五行（水二、木三、金四、土五、火六局）。</li>
 *   <li>紫微：生日除以局數，商數自寅宮起數；不能整除時借數補足，借數為偶則
 *       順進、為奇則逆退。天府與紫微以寅申為軸對稱。</li>
 *   <li>大限：自命宮起，第一限起於局數之歲，陽男陰女順行、陰男陽女逆行，每宮十年。</li>
 *   <li>流年：流年命宮在該年地支；流月以斗君（流年命宮逆數生月、再順數生時）為正月。</li>
 * </ul>
 *
 * <p>相較 lifeup 修正之處：紫微在寅／申時天府與紫微同宮（lifeup 誤安於對宮）；
 * 庚年天魁天鉞在丑未（lifeup 漏排）；巳年火星鈴星照巳酉丑起排（lifeup 漏排）；
 * 天哭天虛依生年支（lifeup 誤用生時）；
 * 封誥自寅宮起子時順數至生時（與台輔夾文曲）。</p>
 */
public final class ZiWeiCalculator {

    /** 閏月出生者，日數大於此值以下個月排盤。 */
    public static final int LEAP_MONTH_SPLIT_DAY = 15;

    private static final int YIN = 2;   // 寅
    private static final int MAO = 3;   // 卯
    private static final int CHEN = 4;  // 辰
    private static final int WU = 6;    // 午
    private static final int YOU = 9;   // 酉
    private static final int XU = 10;   // 戌
    private static final int HAI = 11;  // 亥

    // 祿存（依年干）：甲寅 乙卯 丙戊巳 丁己午 庚申 辛酉 壬亥 癸子
    private static final int[] LU_CUN = {2, 3, 5, 6, 5, 6, 8, 9, 11, 0};
    // 天魁、天鉞（依年干）：甲戊庚牛羊、乙己鼠猴鄉、丙丁豬雞位、壬癸兔蛇藏、六辛逢馬虎
    private static final int[] TIAN_KUI = {1, 0, 11, 11, 1, 0, 1, 6, 3, 3};
    private static final int[] TIAN_YUE = {7, 8, 9, 9, 7, 8, 7, 2, 5, 5};
    // 天官、天福（依年干）
    private static final int[] TIAN_GUAN = {7, 4, 5, 2, 3, 9, 11, 9, 10, 6};
    private static final int[] TIAN_FU_BLESSING = {9, 8, 0, 11, 3, 2, 6, 5, 6, 5};

    // 以下依年支除以 4 的餘數分組：0 申子辰、1 巳酉丑、2 寅午戌、3 亥卯未
    private static final int[] TIAN_MA = {2, 11, 8, 5};       // 寅、亥、申、巳
    private static final int[] HUO_XING_START = {2, 3, 1, 9}; // 寅、卯、丑、酉
    private static final int[] LING_XING_START = {10, 10, 3, 10}; // 戌、戌、卯、戌
    private static final int[] XIAN_CHI = {9, 6, 3, 0};       // 酉、午、卯、子
    private static final int[] HUA_GAI = {4, 1, 10, 7};       // 辰、丑、戌、未
    private static final int[] SMALL_LIMIT_START = {10, 7, 4, 1}; // 戌、未、辰、丑

    // 孤辰、寡宿（依年支所在季：寅卯辰、巳午未、申酉戌、亥子丑）
    private static final int[] GU_CHEN = {5, 8, 11, 2};
    private static final int[] GUA_SU = {1, 4, 7, 10};

    // 命主（依命宮地支）、身主（依年支）
    private static final Star[] LIFE_MASTER = {
            Star.TAN_LANG, Star.JU_MEN, Star.LU_CUN, Star.WEN_QU, Star.LIAN_ZHEN, Star.WU_QU,
            Star.PO_JUN, Star.WU_QU, Star.LIAN_ZHEN, Star.WEN_QU, Star.LU_CUN, Star.JU_MEN
    };
    private static final Star[] BODY_MASTER = {
            Star.HUO_XING, Star.TIAN_XIANG, Star.TIAN_LIANG, Star.TIAN_TONG, Star.WEN_CHANG, Star.TIAN_JI,
            Star.HUO_XING, Star.TIAN_XIANG, Star.TIAN_LIANG, Star.TIAN_TONG, Star.WEN_CHANG, Star.TIAN_JI
    };

    /**
     * 以西曆出生年月日時分（北京時間）與性別排本命盤。
     */
    public ZiWeiChart calculate(int year, int month, int day, int hour, int minute, Gender gender) {
        LocalDateTime birth = LocalDateTime.of(year, month, day, hour, minute);
        boolean lateZiShift = hour == 23;
        LocalDate chartDate = lateZiShift ? birth.toLocalDate().plusDays(1) : birth.toLocalDate();
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(chartDate);
        int hourBranch = ((hour + 1) / 2) % 12;

        int chartMonth = lunar.month();
        if (lunar.leap() && lunar.day() > LEAP_MONTH_SPLIT_DAY) {
            chartMonth = chartMonth % 12 + 1;
        }
        int lunarDay = lunar.day();
        int yearStem = lunar.yearStemIndex();
        int yearBranch = lunar.yearBranchIndex();

        // ---- 命宮、身宮與十二宮宮干 ----
        int lifeBranch = GanZhi.mod(YIN + (chartMonth - 1) - hourBranch, 12);
        int bodyBranch = GanZhi.mod(YIN + (chartMonth - 1) + hourBranch, 12);
        int yinStem = GanZhi.mod((yearStem % 5) * 2 + 2, 10);
        int[] palaceStems = new int[12];
        for (int b = 0; b < 12; b++) {
            palaceStems[b] = GanZhi.mod(yinStem + GanZhi.mod(b - YIN, 12), 10);
        }
        FiveElementBureau bureau = FiveElementBureau.of(palaceStems[lifeBranch], lifeBranch);

        Map<Star, Integer> pos = new EnumMap<>(Star.class);

        // ---- 十四主星 ----
        int ziWei = ziWeiBranch(bureau.number(), lunarDay);
        pos.put(Star.ZI_WEI, ziWei);
        pos.put(Star.TIAN_JI, GanZhi.mod(ziWei - 1, 12));
        pos.put(Star.TAI_YANG, GanZhi.mod(ziWei - 3, 12));
        pos.put(Star.WU_QU, GanZhi.mod(ziWei - 4, 12));
        pos.put(Star.TIAN_TONG, GanZhi.mod(ziWei - 5, 12));
        pos.put(Star.LIAN_ZHEN, GanZhi.mod(ziWei - 8, 12));
        int tianFu = GanZhi.mod(CHEN - ziWei, 12);
        pos.put(Star.TIAN_FU, tianFu);
        pos.put(Star.TAI_YIN, GanZhi.mod(tianFu + 1, 12));
        pos.put(Star.TAN_LANG, GanZhi.mod(tianFu + 2, 12));
        pos.put(Star.JU_MEN, GanZhi.mod(tianFu + 3, 12));
        pos.put(Star.TIAN_XIANG, GanZhi.mod(tianFu + 4, 12));
        pos.put(Star.TIAN_LIANG, GanZhi.mod(tianFu + 5, 12));
        pos.put(Star.QI_SHA, GanZhi.mod(tianFu + 6, 12));
        pos.put(Star.PO_JUN, GanZhi.mod(tianFu + 10, 12));

        // ---- 月系、時系吉星 ----
        pos.put(Star.ZUO_FU, GanZhi.mod(CHEN + (chartMonth - 1), 12));
        pos.put(Star.YOU_BI, GanZhi.mod(XU - (chartMonth - 1), 12));
        pos.put(Star.WEN_CHANG, GanZhi.mod(XU - hourBranch, 12));
        pos.put(Star.WEN_QU, GanZhi.mod(CHEN + hourBranch, 12));

        // ---- 年干系：祿存、擎羊、陀羅、天魁、天鉞；年支系：天馬 ----
        pos.putAll(stemStars(yearStem, yearBranch));

        // ---- 火星、鈴星（依年支起，順數至生時）；地空、地劫（亥宮起子時） ----
        int group = yearBranch % 4;
        pos.put(Star.HUO_XING, GanZhi.mod(HUO_XING_START[group] + hourBranch, 12));
        pos.put(Star.LING_XING, GanZhi.mod(LING_XING_START[group] + hourBranch, 12));
        pos.put(Star.DI_JIE, GanZhi.mod(HAI + hourBranch, 12));
        pos.put(Star.DI_KONG, GanZhi.mod(HAI - hourBranch, 12));

        // ---- 雜曜 ----
        pos.put(Star.TIAN_XING, GanZhi.mod(YOU + (chartMonth - 1), 12));
        pos.put(Star.TIAN_YAO, GanZhi.mod(1 + (chartMonth - 1), 12));
        pos.put(Star.YIN_SHA, GanZhi.mod(YIN - 2 * ((chartMonth - 1) % 6), 12));
        int hongLuan = GanZhi.mod(MAO - yearBranch, 12);
        pos.put(Star.HONG_LUAN, hongLuan);
        pos.put(Star.TIAN_XI, GanZhi.mod(hongLuan + 6, 12));
        pos.put(Star.XIAN_CHI, XIAN_CHI[group]);
        pos.put(Star.TIAN_WU, new int[]{5, 8, 2, 11}[(chartMonth - 1) % 4]);
        pos.put(Star.HUA_GAI, HUA_GAI[group]);
        pos.put(Star.JIE_SHEN, GanZhi.mod(8 + 2 * ((chartMonth - 1) / 2), 12));
        pos.put(Star.TAI_FU, GanZhi.mod(WU + hourBranch, 12));
        pos.put(Star.FENG_GAO, GanZhi.mod(YIN + hourBranch, 12));
        pos.put(Star.TIAN_CAI, GanZhi.mod(lifeBranch + yearBranch, 12));
        pos.put(Star.TIAN_SHOU, GanZhi.mod(bodyBranch + yearBranch, 12));
        pos.put(Star.TIAN_GUAN, TIAN_GUAN[yearStem]);
        pos.put(Star.TIAN_FU_BLESSING, TIAN_FU_BLESSING[yearStem]);
        int season = GanZhi.mod(yearBranch - YIN, 12) / 3;
        pos.put(Star.GU_CHEN, GU_CHEN[season]);
        pos.put(Star.GUA_SU, GUA_SU[season]);
        pos.put(Star.PO_SUI, new int[]{5, 1, 9}[yearBranch % 3]);
        pos.put(Star.TIAN_KU, GanZhi.mod(WU - yearBranch, 12));
        pos.put(Star.TIAN_XU, GanZhi.mod(WU + yearBranch, 12));

        List<List<Star>> stars = emptyPalaces();
        for (Star star : Star.values()) {
            stars.get(pos.get(star)).add(star);
        }

        // ---- 生年四化 ----
        Map<Star, SiHua> natalSiHua = siHuaFor(yearStem);

        // ---- 大限 ----
        boolean yang = yearStem % 2 == 0;
        boolean forward = yang == (gender == Gender.MALE);
        List<Decade> decades = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int b = GanZhi.mod(lifeBranch + (forward ? i : -i), 12);
            int startAge = bureau.number() + 10 * i;
            decades.add(new Decade(i, b, palaceStems[b], startAge, startAge + 9,
                    lunar.year() + startAge - 1, lunar.year() + startAge + 8));
        }

        return new ZiWeiChart(birth, gender, lunar, lateZiShift, chartMonth, hourBranch, forward,
                lifeBranch, bodyBranch, bureau, palaceStems, stars, pos, natalSiHua, decades,
                LIFE_MASTER[lifeBranch], BODY_MASTER[yearBranch], SMALL_LIMIT_START[group]);
    }

    /**
     * 紫微星所在地支：生日 day 加上借數 x 使其被局數整除，商為 q；
     * 自寅宮起數 q 宮，借數為偶數則再順進 x 宮、奇數則逆退 x 宮。
     */
    static int ziWeiBranch(int bureauNumber, int day) {
        int borrow = (bureauNumber - day % bureauNumber) % bureauNumber;
        int quotient = (day + borrow) / bureauNumber;
        int steps = (quotient - 1) + (borrow % 2 == 0 ? borrow : -borrow);
        return GanZhi.mod(YIN + steps, 12);
    }

    /** 依天干排祿存、擎羊、陀羅、天魁、天鉞，依地支排天馬（本命、大限、流年、流月共用）。 */
    private static Map<Star, Integer> stemStars(int stem, int branch) {
        Map<Star, Integer> pos = new EnumMap<>(Star.class);
        int lu = LU_CUN[stem];
        pos.put(Star.LU_CUN, lu);
        pos.put(Star.QING_YANG, GanZhi.mod(lu + 1, 12));
        pos.put(Star.TUO_LUO, GanZhi.mod(lu - 1, 12));
        pos.put(Star.TIAN_KUI, TIAN_KUI[stem]);
        pos.put(Star.TIAN_YUE, TIAN_YUE[stem]);
        pos.put(Star.TIAN_MA, TIAN_MA[branch % 4]);
        return pos;
    }

    private static Map<Star, SiHua> siHuaFor(int stem) {
        Map<Star, SiHua> map = new EnumMap<>(Star.class);
        for (SiHua sh : SiHua.values()) {
            map.put(sh.starFor(stem), sh);
        }
        return map;
    }

    private static List<List<Star>> emptyPalaces() {
        List<List<Star>> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            list.add(new ArrayList<>());
        }
        return list;
    }

    private static List<List<Star>> overlayStars(Map<Star, Integer> pos) {
        List<List<Star>> stars = emptyPalaces();
        for (Map.Entry<Star, Integer> e : pos.entrySet()) {
            stars.get(e.getValue()).add(e.getKey());
        }
        return stars;
    }

    // ------------------------------------------------------------------ 行運

    /** 第 index 步大限（0 起算）：大限命宮與宮名、宮干四化，以及大祿、大羊、大陀、大馬、大魁、大鉞。 */
    public LuckOverlay decade(ZiWeiChart chart, int index) {
        Decade d = chart.getDecades().get(index);
        String title = String.format("大限 %s（%d~%d歲，%d~%d年）",
                d.ganZhi(), d.startAge(), d.endAge(), d.startYear(), d.endYear());
        return new LuckOverlay(Layer.DECADE, title, d.branch(), d.stem(), d.branch(),
                siHuaFor(d.stem()), overlayStars(stemStars(d.stem(), d.branch())), null, -1);
    }

    /**
     * 流年（lunarYear 為農曆年，與西元年同號）：流年命宮在該年地支，依年干四化，
     * 並排流祿、流羊、流陀、流馬、流魁、流鉞、流昌、流曲、流鸞、流喜；以斗君定各宮流月，
     * 並標示小限。
     */
    public LuckOverlay year(ZiWeiChart chart, int lunarYear) {
        int stem = GanZhi.mod(lunarYear - 4, 10);
        int branch = GanZhi.mod(lunarYear - 4, 12);

        Map<Star, Integer> pos = stemStars(stem, branch);
        int chang = GanZhi.mod(LU_CUN[stem] + 3, 12);
        pos.put(Star.WEN_CHANG, chang);
        pos.put(Star.WEN_QU, GanZhi.mod(2 - chang, 12));
        int hongLuan = GanZhi.mod(MAO - branch, 12);
        pos.put(Star.HONG_LUAN, hongLuan);
        pos.put(Star.TIAN_XI, GanZhi.mod(hongLuan + 6, 12));

        int[] monthAt = new int[12];
        int douJun = douJunBranch(chart, branch);
        for (int m = 1; m <= 12; m++) {
            monthAt[GanZhi.mod(douJun + m - 1, 12)] = m;
        }

        int age = chart.ageInYear(lunarYear);
        String title = String.format("流年 %d年 %s%s（虛歲 %d）", lunarYear,
                GanZhi.STEMS[stem], GanZhi.BRANCHES[branch], age);
        return new LuckOverlay(Layer.YEAR, title, branch, stem, branch, siHuaFor(stem), overlayStars(pos),
                monthAt, age >= 1 ? chart.smallLimitBranch(age) : -1);
    }

    /**
     * 流月（month 為農曆月 1~12）：流月命宮在斗君起正月順數至該月的宮位；
     * 月干依流年年干以五虎遁推算，依月干四化並排月祿、月羊、月陀、月馬、月魁、月鉞。
     */
    public LuckOverlay month(ZiWeiChart chart, int lunarYear, int month) {
        int yearStem = GanZhi.mod(lunarYear - 4, 10);
        int yearBranch = GanZhi.mod(lunarYear - 4, 12);
        int stem = GanZhi.mod((yearStem % 5) * 2 + 2 + (month - 1), 10);
        int branch = GanZhi.mod(YIN + (month - 1), 12);
        int lifeBranch = GanZhi.mod(douJunBranch(chart, yearBranch) + month - 1, 12);
        String title = String.format("流月 %d年%s %s%s", lunarYear, LunarCalendar.MONTH_NAMES[month - 1],
                GanZhi.STEMS[stem], GanZhi.BRANCHES[branch]);
        return new LuckOverlay(Layer.MONTH, title, lifeBranch, stem, branch, siHuaFor(stem),
                overlayStars(stemStars(stem, branch)), null, -1);
    }

    /** 斗君（流年正月所在宮）：自流年地支宮起正月逆數至生月，再起子時順數至生時。 */
    public static int douJunBranch(ZiWeiChart chart, int yearBranch) {
        return GanZhi.mod(yearBranch - (chart.getChartMonth() - 1) + chart.getHourBranch(), 12);
    }
}
