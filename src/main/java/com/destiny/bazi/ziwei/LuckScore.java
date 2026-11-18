package com.destiny.bazi.ziwei;

import com.destiny.bazi.core.GanZhi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * lifeup 的評分模式（{@code LifeNumberService}），公式逐行照原程式移植：
 *
 * <ul>
 *   <li><b>好運指數</b>：以該層命宮為主，看三方四正（命、遷移、事業、財帛）的化祿權科
 *       （三奇加會）與化忌、福德與夫妻宮的四化、魁鉞、左輔右弼、夾宮，扣除火鈴、
 *       羊陀（行運羊陀與本層羊陀相沖加倍）等；超過 100 記為 101、低於 0 記為 -1。
 *       大限好運指數直接計算；流年＝大限 × 60% ＋ 流年本身 × 40%；
 *       流月＝流月本身 × 70% ＋ 流年 × 30%（皆為整數除法）。</li>
 *   <li><b>財運指數</b>：看命宮三方四正的化祿、財帛宮的祿存與四化、左右、魁鉞、空劫，
 *       以及田宅宮的祿忌、煞星與吉星數。</li>
 * </ul>
 *
 * <p>lifeup 比對星曜時以「星名＋星曜類型」判定，而陰煞在 lifeup 中沒有設定類型，
 * 比對永遠不成立，因此原本評分中與陰煞有關的扣分從未生效；此處照原樣保留
 * （{@link Kind#UNTYPED}），使分數與 lifeup 一致。</p>
 */
public final class LuckScore {

    /** lifeup 星曜類型：比對時名稱與類型都要相同；UNTYPED（陰煞等）永遠比對不到。 */
    enum Kind { MAIN, LUCKY, SHOCK, SPECIAL, CHANCE, UNTYPED }

    private record Entry(String name, Kind kind, Layer status) {
    }

    /** 一宮在某張盤（本命或疊上行運）中的所有星曜。 */
    private static final class Palace {
        private final List<Entry> entries = new ArrayList<>();

        boolean has(String name, Kind kind) {
            return count(name, kind) > 0;
        }

        boolean has(String name, Kind kind, Layer status) {
            if (kind == Kind.UNTYPED) {
                return false;
            }
            for (Entry e : entries) {
                if (e.name().equals(name) && e.kind() == kind && e.status() == status) {
                    return true;
                }
            }
            return false;
        }

        int count(String name, Kind kind) {
            if (kind == Kind.UNTYPED) {
                return 0;
            }
            int n = 0;
            for (Entry e : entries) {
                if (e.name().equals(name) && e.kind() == kind) {
                    n++;
                }
            }
            return n;
        }

        /** lifeup 的吉星清單（左輔右弼文昌文曲天魁天鉞，含行運魁鉞昌曲）。 */
        int countLucky() {
            return (int) entries.stream().filter(e -> e.kind() == Kind.LUCKY).count();
        }

        /** lifeup 的煞星清單（擎羊陀羅火星鈴星地空地劫，含行運羊陀）。 */
        int countShock() {
            return (int) entries.stream().filter(e -> e.kind() == Kind.SHOCK).count();
        }
    }

    /** 一張用於評分的盤：十二宮（依地支）與該層命宮。 */
    private static final class Board {
        private final Palace[] palaces = new Palace[12];
        private final int lifeBranch;

        Board(int lifeBranch) {
            this.lifeBranch = lifeBranch;
            for (int i = 0; i < 12; i++) {
                palaces[i] = new Palace();
            }
        }

        /** 宮名索引（0 命宮、4 財帛、9 田宅…）所在的宮。 */
        Palace palace(int palaceIndex) {
            return palaces[GanZhi.mod(lifeBranch - palaceIndex, 12)];
        }

        int branchOf(int palaceIndex) {
            return GanZhi.mod(lifeBranch - palaceIndex, 12);
        }

        /** 相對地支位移的宮（lifeup 的 next 為地支遞增）。 */
        Palace offset(int branch, int steps) {
            return palaces[GanZhi.mod(branch + steps, 12)];
        }
    }

    private static final String LU = "化祿";
    private static final String QUAN = "化權";
    private static final String KE = "化科";
    private static final String JI = "化忌";
    private static final String KUI = "天魁";
    private static final String YUE = "天鉞";
    private static final String ZUO = "左輔";
    private static final String YOU = "右弼";
    private static final String YANG = "擎羊";
    private static final String TUO = "陀羅";
    private static final String HUO = "火星";
    private static final String LING = "鈴星";
    private static final String KONG = "地空";
    private static final String JIE = "地劫";
    private static final String LU_CUN = "祿存";
    private static final String YIN_SHA = "陰煞";

    private LuckScore() {
    }

    // ------------------------------------------------------------------ 公開評分

    /** 大限好運指數（lifeup：calWalkTenYearLucky，盤為大限命宮＋大限四化與祿羊陀馬魁鉞）。 */
    public static int decadeLuck(ZiWeiChart chart, LuckOverlay decade) {
        return walkLuck(board(chart, decade.getLifeBranch(), decade),
                Layer.DECADE, Layer.DECADE, Layer.DECADE, Layer.NATAL);
    }

    /** 流年本身的好運指數（lifeup：calWalkYearLucky，盤為流年命宮＋大限、流年星曜）。 */
    public static int yearLuckBase(ZiWeiChart chart, LuckOverlay decade, LuckOverlay year) {
        return walkLuck(board(chart, year.getLifeBranch(), decade, year),
                Layer.YEAR, Layer.YEAR, Layer.DECADE, Layer.YEAR);
    }

    /** 流年好運指數＝大限好運指數 × 60 / 100 ＋ 流年本身 × 40 / 100。 */
    public static int yearLuck(ZiWeiChart chart, LuckOverlay decade, LuckOverlay year) {
        return decadeLuck(chart, decade) * 60 / 100 + yearLuckBase(chart, decade, year) * 40 / 100;
    }

    /** 流月本身的好運指數（lifeup：calWalkMonthLucky，盤為流月命宮＋大限、流年、流月星曜）。 */
    public static int monthLuckBase(ZiWeiChart chart, LuckOverlay decade, LuckOverlay year, LuckOverlay month) {
        return walkLuck(board(chart, month.getLifeBranch(), decade, year, month),
                Layer.MONTH, Layer.MONTH, Layer.YEAR, Layer.MONTH);
    }

    /** 流月好運指數＝流月本身 × 70 / 100 ＋ 流年好運指數 × 30 / 100。 */
    public static int monthLuck(ZiWeiChart chart, LuckOverlay decade, LuckOverlay year, LuckOverlay month) {
        return monthLuckBase(chart, decade, year, month) * 70 / 100 + yearLuck(chart, decade, year) * 30 / 100;
    }

    /** 本命財運指數（lifeup：calMoneyLucky(本命盤)）。 */
    public static int natalWealth(ZiWeiChart chart) {
        return wealth(board(chart, chart.getLifeBranch()));
    }

    /** 大限財運指數（lifeup LifeNumberApp：以大限命宮重排宮位，不加大限星曜）。 */
    public static int decadeWealth(ZiWeiChart chart, LuckOverlay decade) {
        return wealth(board(chart, decade.getLifeBranch()));
    }

    /** 流年財運指數（lifeup：calMoneyLucky(流年盤，含大限與流年星曜)）。 */
    public static int yearWealth(ZiWeiChart chart, LuckOverlay decade, LuckOverlay year) {
        return wealth(board(chart, year.getLifeBranch(), decade, year));
    }

    /** lifeup 的好運指數評語（getLuckyDesc）。 */
    public static String describe(int grade) {
        if (grade >= 100) {
            return "天選之人，你將得到老田爺的眷顧";
        } else if (grade >= 80) {
            return "強運之人，一帆風順處處遇好事";
        } else if (grade >= 60) {
            return "你稱得上是幸運之人";
        } else if (grade >= 40) {
            return "好事壞事參半";
        } else if (grade >= 20) {
            return "運氣不佳，靜待天時";
        } else if (grade >= 0) {
            return "你完全沒有運氣，請靠自己努力";
        }
        return "你真的應該衰到爆了，願老天保佑你";
    }

    // ------------------------------------------------------------------ 建盤

    private static Kind kindOf(Star star) {
        return switch (star.category()) {
            case MAIN -> Kind.MAIN;
            case LUCKY -> Kind.LUCKY;
            case SHA -> Kind.SHOCK;
            case LU_MA -> Kind.SPECIAL;
            // 評分只查到雜曜中的陰煞，而 lifeup 的陰煞沒有類型
            case MINOR -> Kind.UNTYPED;
        };
    }

    private static Board board(ZiWeiChart chart, int lifeBranch, LuckOverlay... overlays) {
        Board b = new Board(lifeBranch);
        for (int branch = 0; branch < 12; branch++) {
            for (Star s : chart.starsAt(branch)) {
                b.palaces[branch].entries.add(new Entry(s.label(), kindOf(s), Layer.NATAL));
            }
        }
        addSiHua(b, chart, chart.getNatalSiHua(), Layer.NATAL, List.of());
        List<LuckOverlay> earlier = new ArrayList<>();
        for (LuckOverlay o : overlays) {
            // lifeup 先排該層四化、再排該層星曜，所以四化只看得到更早幾層的星
            addSiHua(b, chart, o.getSiHua(), o.getLayer(), earlier);
            for (int branch = 0; branch < 12; branch++) {
                for (Star s : o.starsAt(branch)) {
                    b.palaces[branch].entries.add(new Entry(s.label(), kindOf(s), o.getLayer()));
                }
            }
            earlier.add(o);
        }
        return b;
    }

    /**
     * 把四化加到化曜星所在的宮。lifeup 以星名找宮：自寅宮起依寅卯…亥子丑的順序，
     * 取第一個有該星的宮，因此流月四化落在文昌、文曲時，可能落到排在前面的流昌、
     * 流曲所在宮，而不是本命文昌、文曲所在宮；此處照原樣找宮，讓分數與 lifeup 相同。
     */
    private static void addSiHua(Board b, ZiWeiChart chart, Map<Star, SiHua> siHua, Layer layer,
                                 List<LuckOverlay> earlier) {
        for (Map.Entry<Star, SiHua> e : siHua.entrySet()) {
            Star star = e.getKey();
            int branch = chart.branchOf(star);
            for (LuckOverlay o : earlier) {
                for (int candidate = 0; candidate < 12; candidate++) {
                    if (o.starsAt(candidate).contains(star) && searchOrder(candidate) < searchOrder(branch)) {
                        branch = candidate;
                    }
                }
            }
            b.palaces[branch].entries.add(new Entry(e.getValue().toString(), Kind.CHANCE, layer));
        }
    }

    /** lifeup 宮位清單的順序：寅卯辰巳午未申酉戌亥子丑。 */
    private static int searchOrder(int branch) {
        return GanZhi.mod(branch - 2, 12);
    }

    // ------------------------------------------------------------------ 好運指數

    /**
     * lifeup 的 calWalkTenYearLucky／calWalkYearLucky／calWalkMonthLucky：三者公式相同，
     * 只差在比對哪一層的四化（chance）、魁鉞（kuiYue）、行運羊陀（yang）與被沖的羊陀（baseYang）。
     */
    private static int walkLuck(Board board, Layer chance, Layer kuiYue, Layer yang, Layer baseYang) {
        int gradeScore = 50;
        int lifeBranch = board.lifeBranch;
        Palace main = board.palace(0);
        Palace opposite = board.offset(lifeBranch, 6);
        Palace work = board.offset(lifeBranch, 4);
        Palace money = board.offset(lifeBranch, -4);
        Palace love = board.offset(lifeBranch, 10);
        Palace lucky = board.offset(lifeBranch, 2);
        Palace sideNext = board.offset(lifeBranch, 1);
        Palace sidePrev = board.offset(lifeBranch, -1);

        // 三奇加會
        int threeChangeCnt = 0;
        for (Palace p : new Palace[]{main, opposite, work, money}) {
            if (p.has(LU, Kind.CHANCE, chance) || p.has(QUAN, Kind.CHANCE, chance) || p.has(KE, Kind.CHANCE, chance)) {
                threeChangeCnt++;
            }
        }
        boolean jiInFour = main.has(JI, Kind.CHANCE, chance) || opposite.has(JI, Kind.CHANCE, chance)
                || work.has(JI, Kind.CHANCE, chance) || money.has(JI, Kind.CHANCE, chance);
        if (threeChangeCnt == 3) {
            gradeScore = jiInFour ? 65 : 80;
        } else if (threeChangeCnt == 2) {
            gradeScore = jiInFour ? 55 : 70;
        } else if (threeChangeCnt == 1) {
            gradeScore = jiInFour ? 45 : 60;
        }
        // 科忌同宮
        if (main.has(JI, Kind.CHANCE, chance) && main.has(KE, Kind.CHANCE, chance)) {
            gradeScore += 10;
        }
        // 忌沖命
        if (opposite.has(JI, Kind.CHANCE, chance)) {
            gradeScore -= 10;
            if (main.has(KE, Kind.CHANCE, chance)) {
                gradeScore += 8;
            }
        }
        // 忌沖福
        if (lucky.has(JI, Kind.CHANCE, chance)) {
            gradeScore -= 8;
            if (main.has(KE, Kind.CHANCE, chance)) {
                gradeScore += 4;
            }
        }
        // 祿會命
        if (opposite.has(LU, Kind.CHANCE, chance)) {
            gradeScore += 10;
        }
        if (lucky.has(LU, Kind.CHANCE, chance)) {
            gradeScore += 8;
        }
        if (lucky.has(KE, Kind.CHANCE, chance)) {
            gradeScore += 5;
        }
        if (lucky.has(QUAN, Kind.CHANCE, chance)) {
            gradeScore += 5;
        }
        if (love.has(LU, Kind.CHANCE, chance)) {
            gradeScore += 8;
        }
        if (love.has(QUAN, Kind.CHANCE, chance)) {
            gradeScore += 5;
        }
        if (love.has(KE, Kind.CHANCE, chance)) {
            gradeScore += 5;
        }

        int helpScore = 0;
        if (main.has(KUI, Kind.LUCKY, kuiYue)) {
            helpScore += 5;
        }
        if (main.has(YUE, Kind.LUCKY, kuiYue)) {
            helpScore += 5;
        }
        if (lucky.has(KUI, Kind.LUCKY, kuiYue)) {
            helpScore += 3;
        }
        if (lucky.has(YUE, Kind.LUCKY, kuiYue)) {
            helpScore += 3;
        }
        if (love.has(KUI, Kind.LUCKY, kuiYue)) {
            helpScore += 3;
        }
        if (love.has(YUE, Kind.LUCKY, kuiYue)) {
            helpScore += 3;
        }
        if (opposite.has(KUI, Kind.LUCKY, kuiYue) || work.has(KUI, Kind.LUCKY, kuiYue)
                || money.has(KUI, Kind.LUCKY, kuiYue)) {
            helpScore += 4;
        }
        if (opposite.has(YUE, Kind.LUCKY, kuiYue) || work.has(YUE, Kind.LUCKY, kuiYue)
                || money.has(YUE, Kind.LUCKY, kuiYue)) {
            helpScore += 4;
        }
        if ((main.has(KUI, Kind.LUCKY, kuiYue) && opposite.has(YUE, Kind.LUCKY, kuiYue))
                || (opposite.has(KUI, Kind.LUCKY, kuiYue) && main.has(YUE, Kind.LUCKY, kuiYue))) {
            helpScore *= 2;
        }

        boolean youInFour = main.has(YOU, Kind.LUCKY) || opposite.has(YOU, Kind.LUCKY)
                || work.has(YOU, Kind.LUCKY) || money.has(YOU, Kind.LUCKY);
        boolean zuoInFour = main.has(ZUO, Kind.LUCKY) || opposite.has(ZUO, Kind.LUCKY)
                || work.has(ZUO, Kind.LUCKY) || money.has(ZUO, Kind.LUCKY);
        if (youInFour && zuoInFour) {
            helpScore += 10;
        } else {
            if (youInFour) {
                helpScore += 3;
            }
            if (zuoInFour) {
                helpScore += 3;
            }
        }
        // 左輔右弼只有本命，lifeup 以本命層比對
        if (lucky.has(ZUO, Kind.LUCKY, Layer.NATAL)) {
            helpScore += 2;
        }
        if (lucky.has(YOU, Kind.LUCKY, Layer.NATAL)) {
            helpScore += 2;
        }
        if (love.has(ZUO, Kind.LUCKY, Layer.NATAL)) {
            helpScore += 2;
        }
        if (love.has(YOU, Kind.LUCKY, Layer.NATAL)) {
            helpScore += 2;
        }
        // 輔弼夾命、魁鉞夾命（夾宮比對不分層）
        if ((sideNext.has(ZUO, Kind.LUCKY) && sidePrev.has(YOU, Kind.LUCKY))
                || (sidePrev.has(ZUO, Kind.LUCKY) && sideNext.has(YOU, Kind.LUCKY))) {
            helpScore *= 2;
        }
        if ((sideNext.has(KUI, Kind.LUCKY) && sidePrev.has(YUE, Kind.LUCKY))
                || (sidePrev.has(KUI, Kind.LUCKY) && sideNext.has(YUE, Kind.LUCKY))) {
            helpScore += 10;
        }

        int unluckyScore = 0;
        // 陰煞：lifeup 中比對不到（見類別說明），照原樣保留
        if (main.has(YIN_SHA, Kind.UNTYPED)) {
            unluckyScore = 10;
        }
        if (opposite.has(YIN_SHA, Kind.UNTYPED) || work.has(YIN_SHA, Kind.UNTYPED)
                || money.has(YIN_SHA, Kind.UNTYPED)) {
            unluckyScore = 5;
        }
        if (main.has(HUO, Kind.SHOCK)) {
            unluckyScore += 8;
        }
        if (opposite.has(HUO, Kind.SHOCK) || work.has(HUO, Kind.SHOCK) || money.has(HUO, Kind.SHOCK)) {
            unluckyScore += 4;
        }
        if (main.has(LING, Kind.SHOCK)) {
            unluckyScore += 8;
        }
        if (opposite.has(LING, Kind.SHOCK) || work.has(LING, Kind.SHOCK) || money.has(LING, Kind.SHOCK)) {
            unluckyScore += 4;
        }
        // 行運羊沖本羊
        unluckyScore = clash(unluckyScore, main, opposite, YANG, yang, baseYang, 7);
        unluckyScore = clash(unluckyScore, opposite, main, YANG, yang, baseYang, 7);
        unluckyScore = clash(unluckyScore, money, lucky, YANG, yang, baseYang, 12);
        unluckyScore = clash(unluckyScore, lucky, money, YANG, yang, baseYang, 12);
        // 行運陀沖本陀
        unluckyScore = clash(unluckyScore, main, opposite, TUO, yang, baseYang, 12);
        unluckyScore = clash(unluckyScore, opposite, main, TUO, yang, baseYang, 12);
        unluckyScore = clash(unluckyScore, money, lucky, TUO, yang, baseYang, 15);
        unluckyScore = clash(unluckyScore, lucky, money, TUO, yang, baseYang, 15);

        gradeScore += helpScore;
        gradeScore -= unluckyScore;
        if (gradeScore > 100) {
            gradeScore = 101;
        }
        if (gradeScore < 0) {
            gradeScore = -1;
        }
        return gradeScore;
    }

    /** 行運羊（陀）在 at 宮：扣 penalty；若對沖宮 against 有被沖層的羊（陀），累計扣分加倍。 */
    private static int clash(int unluckyScore, Palace at, Palace against, String star, Layer yang, Layer baseYang,
                             int penalty) {
        if (at.has(star, Kind.SHOCK, yang)) {
            unluckyScore += penalty;
            if (against.has(star, Kind.SHOCK, baseYang)) {
                unluckyScore *= 2;
            }
        }
        return unluckyScore;
    }

    // ------------------------------------------------------------------ 財運指數

    /** lifeup 的 calMoneyLucky：四化、祿存、左右、魁鉞、空劫不分層比對。 */
    private static int wealth(Board board) {
        int grade = 0;
        int lifeBranch = board.lifeBranch;
        Palace main = board.palace(0);
        Palace opposite = board.offset(lifeBranch, 6);
        Palace[] lightUp = {board.offset(lifeBranch, 4), board.offset(lifeBranch, -4)};

        // 化祿
        int cnt1 = 0;
        if (main.has(LU, Kind.CHANCE)) {
            grade += 40;
            cnt1 += main.count(LU, Kind.CHANCE);
        }
        if (opposite.has(LU, Kind.CHANCE)) {
            grade += 20;
            cnt1 += opposite.count(LU, Kind.CHANCE);
        }
        for (Palace p : lightUp) {
            if (p.has(LU, Kind.CHANCE)) {
                grade += 10;
                cnt1 += p.count(LU, Kind.CHANCE);
            }
        }
        if (cnt1 >= 3) {
            grade = grade * (10 + cnt1) / 10;
        }
        // 陰煞（lifeup 中比對不到）
        if (main.has(YIN_SHA, Kind.UNTYPED)) {
            grade -= 20;
        }
        if (grade <= 0) {
            int count1 = main.countLucky() + opposite.countLucky() + lightUp[0].countLucky() + lightUp[1].countLucky();
            grade += 2 * count1;
        }

        // 財帛宮
        int moneyBranch = board.branchOf(4);
        Palace m = board.palaces[moneyBranch];
        opposite = board.offset(moneyBranch, 6);
        lightUp = new Palace[]{board.offset(moneyBranch, 4), board.offset(moneyBranch, -4)};
        Palace[] twoSide = {board.offset(moneyBranch, 1), board.offset(moneyBranch, -1)};
        // 祿存
        if (m.has(LU_CUN, Kind.SPECIAL)) {
            if (m.has(LU, Kind.CHANCE)) {
                grade += 50;
            }
            if (opposite.has(LU, Kind.CHANCE)) {
                grade += 100;
            }
            if (opposite.has(JI, Kind.CHANCE)) {
                grade -= 75;
                if (opposite.has(KE, Kind.CHANCE)) {
                    grade += 35;
                }
            }
            for (Palace p : lightUp) {
                if (p.has(JI, Kind.CHANCE)) {
                    grade -= 20;
                    if (p.has(KE, Kind.CHANCE)) {
                        grade += 10;
                    }
                }
                if (p.has(LU, Kind.CHANCE)) {
                    grade += 20;
                }
            }
        }
        // 陰煞（lifeup 中比對不到）
        if (m.has(YIN_SHA, Kind.UNTYPED)) {
            grade -= 30;
        }
        // 左右
        if (m.has(ZUO, Kind.LUCKY) && m.has(YOU, Kind.LUCKY)) {
            grade = grade * 12 / 10;
        }
        if ((lightUp[0].has(ZUO, Kind.LUCKY) && lightUp[1].has(YOU, Kind.LUCKY))
                || (lightUp[0].has(YOU, Kind.LUCKY) && lightUp[1].has(ZUO, Kind.LUCKY))) {
            grade = grade * 15 / 10;
        }
        // 魁鉞
        if ((m.has(KUI, Kind.LUCKY) && opposite.has(YUE, Kind.LUCKY))
                || (opposite.has(KUI, Kind.LUCKY) && m.has(YUE, Kind.LUCKY))) {
            grade += 20;
        }
        if ((lightUp[0].has(KUI, Kind.LUCKY) && lightUp[1].has(YUE, Kind.LUCKY))
                || (lightUp[1].has(KUI, Kind.LUCKY) && lightUp[0].has(YUE, Kind.LUCKY))) {
            grade += 40;
        }
        // 空劫
        if (pairAcross(lightUp[0], lightUp[1], JIE, KONG)) {
            grade -= 60;
        }
        if (pairAcross(m, opposite, JIE, KONG)) {
            grade -= 50;
        }
        if (pairAcross(twoSide[0], twoSide[1], JIE, KONG)) {
            grade -= 100;
        }
        int cnt2 = m.count(KUI, Kind.LUCKY) + m.count(YUE, Kind.LUCKY)
                + opposite.count(KUI, Kind.LUCKY) + opposite.count(YUE, Kind.LUCKY)
                + lightUp[0].count(YUE, Kind.LUCKY) + lightUp[1].count(YUE, Kind.LUCKY);
        if (cnt2 >= 4) {
            grade += 20;
        }

        // 田宅宮
        int saveGrade = 0;
        int saveBranch = board.branchOf(9);
        Palace save = board.palaces[saveBranch];
        opposite = board.offset(saveBranch, 6);
        lightUp = new Palace[]{board.offset(saveBranch, 4), board.offset(saveBranch, -4)};
        twoSide = new Palace[]{board.offset(saveBranch, 1), board.offset(saveBranch, -1)};
        if (opposite.has(JI, Kind.CHANCE)) {
            int k = opposite.count(JI, Kind.CHANCE);
            saveGrade -= 200 * k;
            if (save.has(LU_CUN, Kind.SPECIAL)) {
                saveGrade -= 200 * k;
            }
        }
        if (opposite.has(LU, Kind.CHANCE)) {
            int k = opposite.count(LU, Kind.CHANCE);
            saveGrade += 200 * k;
            if (save.has(LU_CUN, Kind.SPECIAL)) {
                saveGrade += 200 * k;
            }
        }
        // 以下兩段 lifeup 取的是對宮的化忌／化祿數，照原樣保留
        if (save.has(JI, Kind.CHANCE)) {
            int k = opposite.count(JI, Kind.CHANCE);
            saveGrade -= 20 * k - 50;
            if (opposite.has(KE, Kind.CHANCE)) {
                grade += 40;
            }
        }
        if (save.has(LU, Kind.CHANCE)) {
            int k = opposite.count(LU, Kind.CHANCE);
            saveGrade += 20 * k + 50;
        }
        int cnt3 = save.countShock() + opposite.countShock() + lightUp[0].countShock() + lightUp[1].countShock();
        saveGrade -= cnt3 * 15;
        int cnt4 = save.countLucky() + opposite.countLucky() + lightUp[0].countLucky() + lightUp[1].countLucky();
        saveGrade += cnt4 * 5;
        // 空劫
        if (pairAcross(lightUp[0], lightUp[1], JIE, KONG)) {
            saveGrade -= 80;
        }
        if (pairAcross(save, opposite, JIE, KONG)) {
            saveGrade -= 90;
        }
        if (pairAcross(twoSide[0], twoSide[1], JIE, KONG)) {
            saveGrade -= 150;
        }
        return grade + saveGrade;
    }

    /** 兩宮分別有 a、b 兩星（任一順序）。 */
    private static boolean pairAcross(Palace p1, Palace p2, String a, String b) {
        return (p1.has(a, Kind.SHOCK) && p2.has(b, Kind.SHOCK)) || (p2.has(a, Kind.SHOCK) && p1.has(b, Kind.SHOCK));
    }
}
