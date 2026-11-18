package com.destiny.bazi.tests;

import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.Gender;
import com.destiny.bazi.core.LunarCalendar;
import com.destiny.bazi.ziwei.Decade;
import com.destiny.bazi.ziwei.FiveElementBureau;
import com.destiny.bazi.ziwei.LuckOverlay;
import com.destiny.bazi.ziwei.SiHua;
import com.destiny.bazi.ziwei.Star;
import com.destiny.bazi.ziwei.ZiWeiCalculator;
import com.destiny.bazi.ziwei.ZiWeiChart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 紫微斗數測試：逐宮核對範例命盤、紫微星諸局表、lifeup 錯誤的修正、閏月與晚子時、
 * 大限流年流月；並以依口訣字串另行撰寫的獨立實作，交叉比對隨機命盤的所有星曜位置。
 */
final class ZiWeiTests {

    private static final ZiWeiCalculator CALC = new ZiWeiCalculator();

    private ZiWeiTests() {
    }

    static void run() {
        sampleChart();
        ziWeiTables();
        lifeupFixes();
        leapMonthAndLateZi();
        luck();
    }

    private static int br(String branch) {
        return "子丑寅卯辰巳午未申酉戌亥".indexOf(branch);
    }

    /** 1984-07-27 0 時男命（農曆甲子年六月廿九子時）：每一宮的干支、宮名、大限與星曜。 */
    private static void sampleChart() {
        ZiWeiChart c = CALC.calculate(1984, 7, 27, 0, 0, Gender.MALE);
        Check.equal("範例農曆", "甲子年六月廿九", c.getLunarDate().toChineseString());
        Check.equal("範例五行局", FiveElementBureau.EARTH, c.getBureau());
        Check.equal("範例陰陽男女", "陽男", c.getYinYangGender());
        Check.equal("範例命主", Star.WU_QU, c.getLifeMaster());
        Check.equal("範例身主", Star.HUO_XING, c.getBodyMaster());
        Check.equal("範例身宮", br("未"), c.getBodyBranch());
        String[] expected = {
                "丙子 交友宮 55~64 貪狼 解神",
                "丁丑 遷移宮 65~74 天同 巨門 天魁 陀羅",
                "丙寅 疾厄宮 75~84 武曲 天相 火星 祿存 天馬 天刑 封誥 孤辰",
                "丁卯 財帛宮 85~94 太陽 天梁 擎羊 紅鸞",
                "戊辰 子女宮 95~104 七殺 文曲 陰煞 華蓋",
                "己巳 夫妻宮 105~114 天機 右弼 破碎",
                "庚午 兄弟宮 115~124 紫微 天姚 台輔 天哭 天虛",
                "辛未 命宮 5~14 天鉞 天才 天壽 天官",
                "壬申 父母宮 15~24 破軍 天巫",
                "癸酉 福德宮 25~34 左輔 天喜 咸池 天福",
                "甲戌 田宅宮 35~44 廉貞 天府 文昌 鈴星 寡宿",
                "乙亥 事業宮 45~54 太陰 地空 地劫"
        };
        for (int b = 0; b < 12; b++) {
            Decade d = c.decadeAt(b);
            StringBuilder sb = new StringBuilder(c.palaceGanZhi(b) + " " + c.palaceName(b) + " "
                    + d.startAge() + "~" + d.endAge());
            for (Star s : c.starsAt(b)) {
                sb.append(' ').append(s.label());
            }
            Check.equal("範例 " + GanZhi.BRANCHES[b] + "宮", expected[b], sb.toString());
        }
        Map<Star, SiHua> sihua = c.getNatalSiHua();
        Check.equal("甲年廉貞化祿", SiHua.LU, sihua.get(Star.LIAN_ZHEN));
        Check.equal("甲年破軍化權", SiHua.QUAN, sihua.get(Star.PO_JUN));
        Check.equal("甲年武曲化科", SiHua.KE, sihua.get(Star.WU_QU));
        Check.equal("甲年太陽化忌", SiHua.JI, sihua.get(Star.TAI_YANG));
        Check.equal("範例大限起訖年", "1988~1997", c.getDecades().get(0).startYear() + "~" + c.getDecades().get(0).endYear());
    }

    /** 紫微星諸局表（初一至三十）。 */
    private static void ziWeiTables() {
        Map<FiveElementBureau, String> table = new EnumMap<>(FiveElementBureau.class);
        table.put(FiveElementBureau.WATER, "丑寅寅卯卯辰辰巳巳午午未未申申酉酉戌戌亥亥子子丑丑寅寅卯卯辰");
        table.put(FiveElementBureau.WOOD, "辰丑寅巳寅卯午卯辰未辰巳申巳午酉午未戌未申亥申酉子酉戌丑戌亥");
        table.put(FiveElementBureau.METAL, "亥辰丑寅子巳寅卯丑午卯辰寅未辰巳卯申巳午辰酉午未巳戌未申午亥");
        table.put(FiveElementBureau.EARTH, "午亥辰丑寅未子巳寅卯申丑午卯辰酉寅未辰巳戌卯申巳午亥辰酉午未");
        table.put(FiveElementBureau.FIRE, "酉午亥辰丑寅戌未子巳寅卯亥申丑午卯辰子酉寅未辰巳丑戌卯申巳午");
        // 以命宮干支找出各局的範例，逐日檢查紫微所在宮
        for (Map.Entry<FiveElementBureau, String> e : table.entrySet()) {
            for (int day = 1; day <= 30; day++) {
                int expected = br(e.getValue().substring(day - 1, day));
                Check.equal(e.getKey().label() + "初" + day + "紫微", expected, ziWeiFor(e.getKey(), day));
            }
        }
    }

    /** 找一個五行局符合、農曆日為 day 的出生日期，回傳紫微所在地支。 */
    private static int ziWeiFor(FiveElementBureau bureau, int day) {
        for (int y = 1950; y < 2050; y++) {
            for (int m = 1; m <= 12; m++) {
                try {
                    LocalDate d = LunarCalendar.toSolar(y, m, false, day);
                    for (int h = 0; h < 23; h += 2) {
                        ZiWeiChart c = CALC.calculate(d.getYear(), d.getMonthValue(), d.getDayOfMonth(), h, 0, Gender.MALE);
                        if (c.getBureau() == bureau) {
                            return c.branchOf(Star.ZI_WEI);
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // 小月沒有三十，換下個月
                }
            }
        }
        throw new AssertionError("找不到 " + bureau + " 初" + day);
    }

    /** lifeup 原有錯誤的修正。 */
    private static void lifeupFixes() {
        int checkedYin = 0;
        int checkedShen = 0;
        Random random = new Random(7);
        while (checkedYin < 3 || checkedShen < 3) {
            ZiWeiChart c = randomChart(random);
            int zw = c.branchOf(Star.ZI_WEI);
            if (zw == br("寅") || zw == br("申")) {
                Check.equal("紫微在" + GanZhi.BRANCHES[zw] + "時天府同宮", zw, c.branchOf(Star.TIAN_FU));
                if (zw == br("寅")) {
                    checkedYin++;
                } else {
                    checkedShen++;
                }
            }
        }
        // 庚年（1990 庚午）天魁在丑、天鉞在未
        ZiWeiChart geng = CALC.calculate(1990, 6, 1, 12, 0, Gender.FEMALE);
        Check.equal("庚年天魁", br("丑"), geng.branchOf(Star.TIAN_KUI));
        Check.equal("庚年天鉞", br("未"), geng.branchOf(Star.TIAN_YUE));
        // 巳年（1989 己巳）子時：火星在卯、鈴星在戌
        ZiWeiChart si = CALC.calculate(1989, 6, 1, 0, 30, Gender.MALE);
        Check.equal("巳年子時火星", br("卯"), si.branchOf(Star.HUO_XING));
        Check.equal("巳年子時鈴星", br("戌"), si.branchOf(Star.LING_XING));
        // 天哭天虛依年支（巳年：哭在丑、虛在亥），與生時無關
        for (int h = 0; h < 23; h += 2) {
            ZiWeiChart c = CALC.calculate(1989, 6, 1, h, 0, Gender.MALE);
            Check.equal("巳年天哭（" + h + "時）", br("丑"), c.branchOf(Star.TIAN_KU));
            Check.equal("巳年天虛（" + h + "時）", br("亥"), c.branchOf(Star.TIAN_XU));
            // 台輔、封誥夾文曲：封誥自寅宮起子時順行
            int wenQu = c.branchOf(Star.WEN_QU);
            Check.equal("台輔在文曲前二位（" + h + "時）", GanZhi.mod(wenQu + 2, 12), c.branchOf(Star.TAI_FU));
            Check.equal("封誥在文曲後二位（" + h + "時）", GanZhi.mod(wenQu - 2, 12), c.branchOf(Star.FENG_GAO));
        }
    }

    private static void leapMonthAndLateZi() {
        // 2023 年閏二月：十五日以前以二月排盤，十六日起以三月排盤
        LocalDate leap15 = LunarCalendar.toSolar(2023, 2, true, 15);
        LocalDate leap16 = LunarCalendar.toSolar(2023, 2, true, 16);
        ZiWeiChart c15 = CALC.calculate(leap15.getYear(), leap15.getMonthValue(), leap15.getDayOfMonth(), 10, 0, Gender.MALE);
        ZiWeiChart c16 = CALC.calculate(leap16.getYear(), leap16.getMonthValue(), leap16.getDayOfMonth(), 10, 0, Gender.MALE);
        Check.equal("閏二月十五以二月排盤", 2, c15.getChartMonth());
        Check.isTrue("閏二月十五不算下月", !c15.isLeapMonthAsNext(), "");
        Check.equal("閏二月十六以三月排盤", 3, c16.getChartMonth());
        Check.isTrue("閏二月十六算下月", c16.isLeapMonthAsNext(), "");
        Check.equal("閏月十六左輔依三月", GanZhi.mod(br("辰") + 2, 12), c16.branchOf(Star.ZUO_FU));

        // 23 時為晚子時：以次日的農曆日排盤，時辰為子時
        ZiWeiChart late = CALC.calculate(1984, 7, 27, 23, 10, Gender.MALE);
        Check.isTrue("23 時晚子時換日", late.isLateZiShift(), "");
        Check.equal("晚子時農曆日", "甲子年七月初一", late.getLunarDate().toChineseString());
        Check.equal("晚子時時辰", 0, late.getHourBranch());
        ZiWeiChart early = CALC.calculate(1984, 7, 28, 0, 10, Gender.MALE);
        for (int b = 0; b < 12; b++) {
            Check.equal("晚子時與次日子時同盤 " + GanZhi.BRANCHES[b], early.starsAt(b), late.starsAt(b));
        }

        // 農曆年以正月初一為界：2024-02-09（除夕）屬癸卯年、2024-02-10 屬甲辰年
        Check.equal("除夕屬前一年", "癸卯", CALC.calculate(2024, 2, 9, 12, 0, Gender.MALE).getLunarDate().yearGanZhi());
        Check.equal("春節屬新年", "甲辰", CALC.calculate(2024, 2, 10, 12, 0, Gender.MALE).getLunarDate().yearGanZhi());
    }

    private static void luck() {
        ZiWeiChart c = CALC.calculate(1984, 7, 27, 0, 0, Gender.MALE);
        // 陰男逆行：1985 乙丑年生男
        ZiWeiChart yinMale = CALC.calculate(1985, 6, 1, 12, 0, Gender.MALE);
        Check.isTrue("陰男大限逆行", !yinMale.isForward(), "");
        Check.equal("逆行第二限", GanZhi.mod(yinMale.getLifeBranch() - 1, 12), yinMale.getDecades().get(1).branch());
        Check.isTrue("陽女大限逆行", !CALC.calculate(1984, 7, 27, 0, 0, Gender.FEMALE).isForward(), "");

        Check.equal("43 歲在第四限", 3, c.decadeIndexForAge(43));
        Check.equal("起限前屬第一限", 0, c.decadeIndexForAge(2));
        Check.equal("超過十二限屬最後一限", 11, c.decadeIndexForAge(200));

        LuckOverlay decade = CALC.decade(c, 3);
        Check.equal("大限命宮", br("戌"), decade.getLifeBranch());
        Check.equal("大限干支", "甲戌", decade.ganZhi());
        Check.equal("大限宮名", "大命", decade.palaceLabel(br("戌")));
        Check.equal("大限財帛", "大財", decade.palaceLabel(br("午")));
        Check.isTrue("大限天馬（戌宮→申）", decade.starsAt(br("申")).contains(Star.TIAN_MA), decade.starsAt(br("申")).toString());

        LuckOverlay year = CALC.year(c, 2026);
        Check.equal("流年命宮", br("午"), year.getLifeBranch());
        Check.equal("流年干支", "丙午", year.ganZhi());
        Check.equal("丙年廉貞化忌", SiHua.JI, year.getSiHua().get(Star.LIAN_ZHEN));
        Check.isTrue("流昌在申", year.starsAt(br("申")).contains(Star.WEN_CHANG), "");
        Check.isTrue("流曲在午", year.starsAt(br("午")).contains(Star.WEN_QU), "");
        Check.isTrue("流鸞在酉", year.starsAt(br("酉")).contains(Star.HONG_LUAN), "");
        // 斗君：午宮起正月逆數至六月（丑），再順數至子時（丑）
        Check.equal("斗君", br("丑"), ZiWeiCalculator.douJunBranch(c, br("午")));
        Check.equal("流年正月在丑", 1, year.monthAt(br("丑")));
        Check.equal("流年十二月在子", 12, year.monthAt(br("子")));
        // 小限：子年生起戌宮，男順行，43 歲在辰
        Check.equal("小限", br("辰"), year.getSmallLimitBranch());
        Check.equal("一歲小限", br("戌"), c.smallLimitBranch(1));

        for (int m = 1; m <= 12; m++) {
            LuckOverlay month = CALC.month(c, 2026, m);
            Check.equal("流月" + m + "命宮與斗君流月一致", m, year.monthAt(month.getLifeBranch()));
            Check.equal("流月" + m + "地支", GanZhi.mod(1 + m, 12), month.getBranch());
        }
        Check.equal("丙年正月庚寅", "庚寅", CALC.month(c, 2026, 1).ganZhi());
        Check.equal("丙年十月己亥", "己亥", CALC.month(c, 2026, 10).ganZhi());
    }

    // ------------------------------------------------------------------ 獨立實作交叉比對

    private static ZiWeiChart randomChart(Random random) {
        LocalDate start = LocalDate.of(1901, 3, 1);
        LocalDate d = start.plusDays(random.nextInt(365 * 198));
        return CALC.calculate(d.getYear(), d.getMonthValue(), d.getDayOfMonth(), random.nextInt(24),
                random.nextInt(60), random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
    }

    /** 以口訣字串另行撰寫的安星法，與 {@link ZiWeiCalculator} 比對隨機命盤。 */
    static void crossCheck() {
        Random random = new Random(20261117);
        for (int n = 0; n < 20000; n++) {
            ZiWeiChart c = randomChart(random);
            Reference ref = new Reference(c.getLunarDate().yearStemIndex(), c.getLunarDate().yearBranchIndex(),
                    c.getChartMonth(), c.getLunarDate().day(), c.getHourBranch(), c.getGender());
            String tag = c.getBirth() + " " + c.getGender();
            Check.equal(tag + " 命宮", ref.life, c.getLifeBranch());
            Check.equal(tag + " 身宮", ref.body, c.getBodyBranch());
            Check.equal(tag + " 五行局", ref.bureau, c.getBureau().number());
            for (Star s : Star.values()) {
                Integer expected = ref.stars.get(s.label());
                if (expected == null) {
                    Check.isTrue(tag + " 參考實作缺 " + s, false, s.label());
                } else if (expected != c.branchOf(s)) {
                    Check.equal(tag + " " + s.label(), GanZhi.BRANCHES[expected], GanZhi.BRANCHES[c.branchOf(s)]);
                }
            }
            Map<Star, SiHua> sihua = c.getNatalSiHua();
            for (SiHua sh : SiHua.values()) {
                Star s = sihua.entrySet().stream().filter(e -> e.getValue() == sh).findFirst().orElseThrow().getKey();
                Check.equal(tag + " 化" + sh.label(), ref.siHua[sh.ordinal()], s.label());
            }
            Check.equal(tag + " 第一限宮", ref.decadeBranches.get(0), c.getDecades().get(0).branch());
            Check.equal(tag + " 第五限宮", ref.decadeBranches.get(4), c.getDecades().get(4).branch());
            Check.equal(tag + " 第一限起歲", ref.bureau, c.getDecades().get(0).startAge());
            int year = c.getLunarDate().year() + random.nextInt(90);
            Check.equal(tag + " " + year + " 斗君", ref.douJun(GanZhi.mod(year - 4, 12)),
                    ZiWeiCalculator.douJunBranch(c, GanZhi.mod(year - 4, 12)));
        }
    }

    /** 參考實作：每顆星依口訣字串查表或逐宮數算，不共用 ZiWeiCalculator 的程式。 */
    private static final class Reference {
        private static final String ORDER = "子丑寅卯辰巳午未申酉戌亥";
        private static final String STEMS = "甲乙丙丁戊己庚辛壬癸";
        // 納音五行（六十甲子，每兩干支一組）
        private static final String NAYIN = "金火木土金火水土金木水土火木水金火木土金火水土金木水土火木水";

        final int life;
        final int body;
        final int bureau;
        final Map<String, Integer> stars = new java.util.HashMap<>();
        final String[] siHua;
        final List<Integer> decadeBranches = new ArrayList<>();
        private final int month;
        private final int hour;

        Reference(int stem, int branch, int month, int day, int hour, Gender gender) {
            this.month = month;
            this.hour = hour;
            // 寅宮起正月，順數到生月；再起子時，逆數為命宮、順數為身宮
            int monthPalace = walk(ORDER.indexOf('寅'), month - 1);
            life = walk(monthPalace, -hour);
            body = walk(monthPalace, hour);
            // 五虎遁：甲己之年丙作首，乙庚之歲戊為頭，丙辛必定尋庚起，丁壬壬位順行流，戊癸何方覓，甲寅之上好追求
            int yinStem = STEMS.indexOf("丙戊庚壬甲".charAt(stem % 5));
            int lifeStem = (yinStem + walkDistance(ORDER.indexOf('寅'), life)) % 10;
            int jiazi = jiaziIndex(lifeStem, life);
            char element = NAYIN.charAt(jiazi / 2);
            bureau = switch (element) {
                case '水' -> 2;
                case '木' -> 3;
                case '金' -> 4;
                case '土' -> 5;
                default -> 6;
            };

            // 紫微：借數使生日能被局數整除
            int borrow = 0;
            while ((day + borrow) % bureau != 0) {
                borrow++;
            }
            int zw = walk(ORDER.indexOf('寅'), (day + borrow) / bureau - 1);
            zw = walk(zw, borrow % 2 == 0 ? borrow : -borrow);
            put("紫微", zw);
            String ziSeries = "紫微,天機,,太陽,武曲,天同,,,廉貞";
            String[] zs = ziSeries.split(",", -1);
            for (int i = 1; i < zs.length; i++) {
                if (!zs[i].isEmpty()) {
                    put(zs[i], walk(zw, -i));
                }
            }
            String tianFuOf = "辰卯寅丑子亥戌酉申未午巳"; // 紫微在子、丑…亥時天府的位置
            int tf = ORDER.indexOf(tianFuOf.charAt(zw));
            String[] fs = "天府,太陰,貪狼,巨門,天相,天梁,七殺,,,,破軍".split(",", -1);
            for (int i = 0; i < fs.length; i++) {
                if (!fs[i].isEmpty()) {
                    put(fs[i], walk(tf, i));
                }
            }

            put("左輔", walk(ORDER.indexOf('辰'), month - 1));
            put("右弼", walk(ORDER.indexOf('戌'), -(month - 1)));
            put("文昌", walk(ORDER.indexOf('戌'), -hour));
            put("文曲", walk(ORDER.indexOf('辰'), hour));
            char luChar = "寅卯巳午巳午申酉亥子".charAt(stem);
            int lu = ORDER.indexOf(luChar);
            put("祿存", lu);
            put("擎羊", walk(lu, 1));
            put("陀羅", walk(lu, -1));
            String[] kuiYue = {"丑未", "子申", "亥酉", "亥酉", "丑未", "子申", "丑未", "午寅", "卯巳", "卯巳"};
            put("天魁", ORDER.indexOf(kuiYue[stem].charAt(0)));
            put("天鉞", ORDER.indexOf(kuiYue[stem].charAt(1)));
            char yb = ORDER.charAt(branch);
            put("天馬", ORDER.indexOf(lookup(yb, "申子辰寅", "寅午戌申", "巳酉丑亥", "亥卯未巳")));
            int huoStart = ORDER.indexOf(lookup(yb, "寅午戌丑", "申子辰寅", "巳酉丑卯", "亥卯未酉"));
            int lingStart = ORDER.indexOf(lookup(yb, "寅午戌卯", "申子辰戌", "巳酉丑戌", "亥卯未戌"));
            put("火星", walk(huoStart, hour));
            put("鈴星", walk(lingStart, hour));
            put("地劫", walk(ORDER.indexOf('亥'), hour));
            put("地空", walk(ORDER.indexOf('亥'), -hour));

            put("天刑", walk(ORDER.indexOf('酉'), month - 1));
            put("天姚", walk(ORDER.indexOf('丑'), month - 1));
            put("陰煞", ORDER.indexOf("寅子戌申午辰寅子戌申午辰".charAt(month - 1)));
            int hongLuan = walk(ORDER.indexOf('卯'), -branch);
            put("紅鸞", hongLuan);
            put("天喜", walk(hongLuan, 6));
            put("咸池", ORDER.indexOf(lookup(yb, "申子辰酉", "寅午戌卯", "巳酉丑午", "亥卯未子")));
            put("天巫", ORDER.indexOf("巳申寅亥巳申寅亥巳申寅亥".charAt(month - 1)));
            put("華蓋", ORDER.indexOf(lookup(yb, "申子辰辰", "寅午戌戌", "巳酉丑丑", "亥卯未未")));
            put("解神", ORDER.indexOf("申申戌戌子子寅寅辰辰午午".charAt(month - 1)));
            put("台輔", walk(ORDER.indexOf('午'), hour));
            put("封誥", walk(ORDER.indexOf('寅'), hour));
            put("天才", walk(life, branch));
            put("天壽", walk(body, branch));
            put("天官", ORDER.indexOf("未辰巳寅卯酉亥酉戌午".charAt(stem)));
            put("天福", ORDER.indexOf("酉申子亥卯寅午巳午巳".charAt(stem)));
            put("孤辰", ORDER.indexOf(lookup(yb, "寅卯辰巳", "巳午未申", "申酉戌亥", "亥子丑寅")));
            put("寡宿", ORDER.indexOf(lookup(yb, "寅卯辰丑", "巳午未辰", "申酉戌未", "亥子丑戌")));
            put("破碎", ORDER.indexOf(lookup(yb, "子午卯酉巳", "寅申巳亥酉", "辰戌丑未丑")));
            put("天哭", walk(ORDER.indexOf('午'), -branch));
            put("天虛", walk(ORDER.indexOf('午'), branch));

            String[] sihuaRhymes = {
                    "廉貞破軍武曲太陽", "天機天梁紫微太陰", "天同天機文昌廉貞", "太陰天同天機巨門", "貪狼太陰右弼天機",
                    "武曲貪狼天梁文曲", "太陽武曲太陰天同", "巨門太陽文曲文昌", "天梁紫微左輔武曲", "破軍巨門太陰貪狼"
            };
            String r = sihuaRhymes[stem];
            siHua = new String[]{r.substring(0, 2), r.substring(2, 4), r.substring(4, 6), r.substring(6, 8)};

            boolean yangMale = stem % 2 == 0 && gender == Gender.MALE;
            boolean yinFemale = stem % 2 == 1 && gender == Gender.FEMALE;
            int step = yangMale || yinFemale ? 1 : -1;
            for (int i = 0; i < 12; i++) {
                decadeBranches.add(walk(life, step * i));
            }
        }

        /** 流年地支宮起正月逆數至生月，再起子時順數至生時。 */
        int douJun(int yearBranch) {
            return walk(walk(yearBranch, -(month - 1)), hour);
        }

        private void put(String name, int branch) {
            stars.put(name, branch);
        }

        /** 在口訣「三合（或四正）地支＋結果」字串中，找到含年支的一組並回傳結果字。 */
        private static char lookup(char yearBranch, String... groups) {
            for (String g : groups) {
                if (g.substring(0, g.length() - 1).indexOf(yearBranch) >= 0) {
                    return g.charAt(g.length() - 1);
                }
            }
            throw new IllegalArgumentException("口訣中找不到 " + yearBranch);
        }

        /** 自 from 宮起，順（正）或逆（負）走 steps 宮。 */
        private static int walk(int from, int steps) {
            int p = from;
            for (int i = 0; i < Math.abs(steps); i++) {
                p = steps > 0 ? (p + 1) % 12 : (p + 11) % 12;
            }
            return p;
        }

        private static int walkDistance(int from, int to) {
            int n = 0;
            int p = from;
            while (p != to) {
                p = (p + 1) % 12;
                n++;
            }
            return n;
        }

        /** 干支在六十甲子中的序號。 */
        private static int jiaziIndex(int stem, int branch) {
            for (int i = 0; i < 60; i++) {
                if (i % 10 == stem && i % 12 == branch) {
                    return i;
                }
            }
            throw new IllegalArgumentException("干支陰陽不合：" + stem + "," + branch);
        }
    }
}
