package com.destiny.bazi.ziwei;

import com.destiny.bazi.core.GanZhi;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 疊在本命盤上的一層行運（大限、流年或流月）：該層命宮位置與十二宮名、
 * 依該層天干產生的四化，以及該層的祿存、擎羊、陀羅、天馬、天魁、天鉞
 * （流年另有流昌、流曲、紅鸞、天喜）。流年層另記錄斗君推得的各宮流月與小限宮位。
 */
public final class LuckOverlay {

    private final Layer layer;
    private final String title;
    private final int lifeBranch;
    private final int stem;
    private final int branch;
    private final Map<Star, SiHua> siHua;
    private final List<List<Star>> stars;
    private final int[] monthAtBranch;
    private final int smallLimitBranch;

    LuckOverlay(Layer layer, String title, int lifeBranch, int stem, int branch, Map<Star, SiHua> siHua,
                List<List<Star>> stars, int[] monthAtBranch, int smallLimitBranch) {
        this.layer = layer;
        this.title = title;
        this.lifeBranch = lifeBranch;
        this.stem = stem;
        this.branch = branch;
        this.siHua = Collections.unmodifiableMap(new EnumMap<>(siHua));
        this.stars = stars.stream().map(List::copyOf).toList();
        this.monthAtBranch = monthAtBranch == null ? null : monthAtBranch.clone();
        this.smallLimitBranch = smallLimitBranch;
    }

    public Layer getLayer() {
        return layer;
    }

    /** 例如「大限 丙寅（6~15歲，1989~1998年）」「流年 2026年 丙午」。 */
    public String getTitle() {
        return title;
    }

    /** 該層命宮所在的地支。 */
    public int getLifeBranch() {
        return lifeBranch;
    }

    /** 該層干支：大限為大限命宮的宮干支，流年、流月為該年／月的干支。 */
    public String ganZhi() {
        return GanZhi.STEMS[stem] + GanZhi.BRANCHES[branch];
    }

    public int getStem() {
        return stem;
    }

    public int getBranch() {
        return branch;
    }

    /** 地支 branch 所在宮在這一層的宮名索引（0=命宮）。 */
    public int palaceIndex(int branchIndex) {
        return GanZhi.mod(lifeBranch - branchIndex, 12);
    }

    /** 例如「大命」「流財」「月父」。 */
    public String palaceLabel(int branchIndex) {
        return layer.prefix() + ZiWeiChart.PALACE_SHORT_NAMES[palaceIndex(branchIndex)];
    }

    /** 依該層天干產生的四化。 */
    public Map<Star, SiHua> getSiHua() {
        return siHua;
    }

    /** 該層在地支 branch 的行運星曜（顯示時加上前綴，例如「流祿」）。 */
    public List<Star> starsAt(int branchIndex) {
        return stars.get(branchIndex);
    }

    /** 流年層：地支 branch 所在宮為流年第幾月的流月命宮（依斗君推算）；其他層為 0。 */
    public int monthAt(int branchIndex) {
        return monthAtBranch == null ? 0 : monthAtBranch[branchIndex];
    }

    /** 流年層：當年小限所在的地支；其他層為 -1。 */
    public int getSmallLimitBranch() {
        return smallLimitBranch;
    }
}
