package com.destiny.bazi.ui;

import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.LunarCalendar;
import com.destiny.bazi.ziwei.Decade;
import com.destiny.bazi.ziwei.Layer;
import com.destiny.bazi.ziwei.LuckOverlay;
import com.destiny.bazi.ziwei.LuckScore;
import com.destiny.bazi.ziwei.SiHua;
import com.destiny.bazi.ziwei.Star;
import com.destiny.bazi.ziwei.ZiWeiChart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 紫微斗數命盤：傳統四方格排列（巳午未申在上、寅丑子亥在下），中央兩格合併顯示
 * 命主資料與好運指數。每宮以直書由右至左列出星曜，星名下方標示四化（本命為實心色塊，
 * 大限／流年／流月為該層顏色的外框），宮位底部標示宮干支、宮名、大限歲數，
 * 以及疊上的大限／流年／流月宮名、行運星曜與斗君流月。點選宮位可切換到該宮的大限。
 *
 * <p>沿用 lifeup 的命盤樣式：每宮 200×200、黑底亮字（主星白、吉星黃、煞星紅、
 * 祿馬青、四化與行運以鮮明色標示，只有雜曜較暗），大限、流年、流月命宮分別以
 * 暗紅褐、暗綠、暗紅底色標示。</p>
 */
public final class ZiWeiChartPanel extends JComponent {

    /** 每宮 200×200，與 lifeup 的命盤相同大小。 */
    static final int BASE_CELL_WIDTH = 200;
    static final int BASE_CELL_HEIGHT = 200;
    private static final int MAX_CELL_WIDTH = 260;
    private static final int MIN_CELL_WIDTH = 100;

    // 地支（子=0）→ 方格欄、列
    private static final int[][] GRID = {
            {2, 3}, {1, 3}, {0, 3}, {0, 2}, {0, 1}, {0, 0},
            {1, 0}, {2, 0}, {3, 0}, {3, 1}, {3, 2}, {3, 3}
    };

    // ---- 黑底亮字配色（文字皆為亮色，只有雜曜與次要說明較暗） ----
    public static final Color CHART_BG = Color.BLACK;
    private static final Color HOVER_BG = new Color(0x22, 0x22, 0x22);
    private static final Color GRID_LINE = new Color(0xC9, 0xA2, 0x27);

    // 鮮明純色（與 lifeup 的 Java 預設色同一路線）：在黑底上都有足夠對比
    public static final Color LAYER_DECADE = new Color(0xFF, 0xA5, 0x00);
    public static final Color LAYER_YEAR = new Color(0x00, 0xFF, 0x99);
    public static final Color LAYER_MONTH = new Color(0xFF, 0x8F, 0xD0);
    // lifeup 的大限／流年／流月命宮底色
    private static final Color FILL_DECADE = new Color(105, 66, 65);
    private static final Color FILL_YEAR = new Color(40, 66, 42);
    private static final Color FILL_MONTH = new Color(110, 20, 42);

    public static final Color SIHUA_LU = new Color(0x00, 0xFF, 0x00);
    public static final Color SIHUA_QUAN = new Color(0xFF, 0x40, 0xFF);
    public static final Color SIHUA_KE = new Color(0x33, 0x99, 0xFF);
    public static final Color SIHUA_JI = new Color(0xFF, 0x30, 0x30);

    public static final Color STAR_MAIN = Color.WHITE;
    public static final Color STAR_LUCKY = new Color(0xFF, 0xFF, 0x00);
    public static final Color STAR_SHA = new Color(0xFF, 0x30, 0x30);
    public static final Color STAR_LU_MA = new Color(0x00, 0xFF, 0xFF);
    public static final Color STAR_MINOR = new Color(0xB0, 0xB0, 0xB0);

    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_SOFT = Color.WHITE;
    private static final Color TEXT_LABEL = new Color(0xD0, 0xD0, 0xD0);
    private static final Color LIFE_PALACE = new Color(0xFF, 0xD7, 0x00);

    private ZiWeiChart chart;
    private String personName = "";
    private LuckOverlay decade;
    private LuckOverlay year;
    private LuckOverlay month;
    private double fontScale = 1.0;
    /** 上次版面配置時採用的實際縮放比例（見 {@link #effectiveScale()}）。 */
    private double layoutScale = 1.0;
    private int hoverBranch = -1;
    private IntConsumer palaceClickListener;
    /** 本次繪製採用的縮放比例。 */
    private double paintScale = 1.0;

    public ZiWeiChartPanel() {
        setOpaque(true);
        setBackground(CHART_BG);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(branchAt(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(-1);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int branch = branchAt(e.getPoint());
                if (branch >= 0 && palaceClickListener != null) {
                    palaceClickListener.accept(branch);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        ToolTipManager.sharedInstance().registerComponent(this);
        // 寬度不足時會自動縮小字級，高度隨之改變：寬度變動後重新要求版面配置。
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                double scale = effectiveScale();
                if (Math.abs(scale - layoutScale) > 1e-6) {
                    layoutScale = scale;
                    SwingUtilities.invokeLater(() -> {
                        revalidate();
                        repaint();
                    });
                }
            }
        });
    }

    public void setChart(ZiWeiChart chart) {
        this.chart = chart;
        repaint();
    }

    public void setPersonName(String name) {
        this.personName = name == null ? "" : name;
        repaint();
    }

    /** 設定要疊上的大限、流年、流月（不顯示的層傳入 null）。 */
    public void setOverlays(LuckOverlay decade, LuckOverlay year, LuckOverlay month) {
        this.decade = decade;
        this.year = year;
        this.month = month;
        repaint();
    }

    public void setFontScale(double scale) {
        this.fontScale = scale;
        revalidate();
        repaint();
    }

    /** 點選宮位時通知所點宮位的地支（子=0）。 */
    public void setPalaceClickListener(IntConsumer listener) {
        this.palaceClickListener = listener;
        setCursor(listener == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** 寬度依字體縮放；高度依實際寬度可容納的縮放比例（寬度不足時字級自動縮小）。 */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) Math.round(BASE_CELL_WIDTH * 4 * fontScale),
                (int) Math.round(BASE_CELL_HEIGHT * 4 * effectiveScale()));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MIN_CELL_WIDTH * 4, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension((int) Math.round(MAX_CELL_WIDTH * 4 * fontScale), getPreferredSize().height);
    }

    /**
     * 實際繪製用的縮放比例：通常等於字體縮放；視窗較窄、放不下該字級的宮格寬度時，
     * 依目前寬度等比例縮小，避免命盤被裁切或文字互相重疊。
     */
    double effectiveScale() {
        int w = getWidth();
        if (w <= 0) {
            return fontScale;
        }
        return Math.min(fontScale, w / (BASE_CELL_WIDTH * 4.0));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int branch = branchAt(e.getPoint());
        if (chart == null || branch < 0) {
            return null;
        }
        Decade d = chart.decadeAt(branch);
        return "<html>" + chart.palaceGanZhi(branch) + "　" + chart.palaceName(branch)
                + (branch == chart.getBodyBranch() ? "（身宮）" : "")
                + "<br>大限 " + d.startAge() + "~" + d.endAge() + " 歲（" + d.startYear() + "~" + d.endYear() + " 年）"
                + "<br>點選切換到此大限</html>";
    }

    private int scaled(double v) {
        return (int) Math.round(v * paintScale);
    }

    private float fontSize(double base) {
        return (float) (base * paintScale);
    }

    private void setHover(int branch) {
        if (branch != hoverBranch) {
            hoverBranch = branch;
            repaint();
        }
    }

    /** 方格座標所在的宮位地支；中央區域或盤外回傳 -1。 */
    int branchAt(Point p) {
        Rectangle[] cells = cellBounds();
        for (int b = 0; b < 12; b++) {
            if (cells[b].contains(p)) {
                return b;
            }
        }
        return -1;
    }

    private Rectangle[] cellBounds() {
        int w = getWidth();
        int h = getHeight();
        Rectangle[] cells = new Rectangle[12];
        for (int b = 0; b < 12; b++) {
            int col = GRID[b][0];
            int row = GRID[b][1];
            int x0 = w * col / 4;
            int x1 = w * (col + 1) / 4;
            int y0 = h * row / 4;
            int y1 = h * (row + 1) / 4;
            cells[b] = new Rectangle(x0, y0, x1 - x0, y1 - y0);
        }
        return cells;
    }

    // ------------------------------------------------------------------ 繪製

    @Override
    protected void paintComponent(Graphics g) {
        paintScale = effectiveScale();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (chart == null) {
            g2.dispose();
            return;
        }
        Rectangle[] cells = cellBounds();
        for (int b = 0; b < 12; b++) {
            paintPalace(g2, b, cells[b]);
        }
        Rectangle center = new Rectangle(cells[4].x + cells[4].width, cells[4].y,
                cells[9].x - (cells[4].x + cells[4].width), cells[3].y + cells[3].height - cells[4].y);
        paintCenter(g2, center);

        g2.setColor(GRID_LINE);
        g2.setStroke(new BasicStroke(1f));
        for (Rectangle r : cells) {
            g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        }
        g2.drawRect(center.x, center.y, center.width - 1, center.height - 1);
        g2.dispose();
    }

    private void paintPalace(Graphics2D g, int branch, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clip(r);

        // 底色：黑底；與 lifeup 相同，大限、流年、流月命宮依序（大限優先）塗上該層底色
        Color fill = branch == hoverBranch ? HOVER_BG : CHART_BG;
        if (decade != null && decade.getLifeBranch() == branch) {
            fill = FILL_DECADE;
        } else if (year != null && year.getLifeBranch() == branch) {
            fill = FILL_YEAR;
        } else if (month != null && month.getLifeBranch() == branch) {
            fill = FILL_MONTH;
        }
        g2.setColor(fill);
        g2.fillRect(r.x, r.y, r.width, r.height);
        // 各層命宮外框（大限在外、流年居中、流月在內）
        int ring = 0;
        for (LuckOverlay o : overlays()) {
            if (o.getLifeBranch() == branch) {
                g2.setColor(layerColor(o.getLayer()));
                g2.setStroke(new BasicStroke(scaled(2)));
                int inset = scaled(2 + ring * 3);
                g2.drawRect(r.x + inset, r.y + inset, r.width - 1 - inset * 2, r.height - 1 - inset * 2);
            }
            ring++;
        }

        int pad = scaled(7);
        Font infoFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(14));
        Font nameFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(16));
        FontMetrics nameFm = g2.getFontMetrics(nameFont);
        int left = r.x + pad;
        int right = r.x + r.width - pad;

        // ---- 最下列：本命宮名與身宮（右）、行運宮名（左；放不下時移到上一行） ----
        int baselineB = r.y + r.height - pad - nameFm.getDescent();
        String palaceName = chart.palaceName(branch);
        boolean isLife = branch == chart.getLifeBranch();
        int x = right;
        if (branch == chart.getBodyBranch()) {
            x = drawBadge(g2, "身", x, baselineB, nameFm, LIFE_PALACE, true);
            x -= scaled(4);
        }
        g2.setFont(nameFont);
        g2.setColor(isLife ? LIFE_PALACE : TEXT);
        g2.drawString(palaceName, x - nameFm.stringWidth(palaceName), baselineB);
        List<Token> layerLabels = new ArrayList<>();
        for (LuckOverlay o : overlays()) {
            boolean lifeOfLayer = o.getLifeBranch() == branch;
            layerLabels.add(new Token(o.palaceLabel(branch), layerColor(o.getLayer()),
                    lifeOfLayer ? infoFont.deriveFont(Font.BOLD) : infoFont));
        }
        int topB = paintRow(g2, layerLabels, right - x + nameFm.stringWidth(palaceName), nameFm.getAscent(),
                baselineB, left, right);

        // ---- 上一列：宮干支（右）、大限歲數與流月／小限（左） ----
        int baselineA = topB - scaled(3) - nameFm.getDescent();
        String gz = chart.palaceGanZhi(branch);
        Font gzFont = nameFont;
        g2.setFont(gzFont);
        g2.setColor(TEXT);
        int gzWidth = g2.getFontMetrics().stringWidth(gz);
        g2.drawString(gz, right - gzWidth, baselineA);
        Decade d = chart.decadeAt(branch);
        List<Token> infoTokens = new ArrayList<>();
        infoTokens.add(new Token(d.startAge() + "~" + d.endAge(), TEXT_SOFT, infoFont));
        if (year != null && year.monthAt(branch) > 0) {
            infoTokens.add(new Token(LunarCalendar.MONTH_NAMES[year.monthAt(branch) - 1], LAYER_YEAR, infoFont));
        }
        if (year != null && year.getSmallLimitBranch() == branch) {
            infoTokens.add(new Token("小限", LAYER_YEAR, infoFont));
        }
        int topA = paintRow(g2, infoTokens, gzWidth, g2.getFontMetrics(gzFont).getAscent(), baselineA, left, right);

        // ---- 行運星曜（大祿、流羊…）：在上一列之上，由下往上換行 ----
        Font luckFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(14));
        FontMetrics luckFm = g2.getFontMetrics(luckFont);
        List<Token> luckTokens = new ArrayList<>();
        for (LuckOverlay o : overlays()) {
            for (Star s : o.starsAt(branch)) {
                luckTokens.add(new Token(o.getLayer().prefix() + s.shortLabel(), layerColor(o.getLayer()), luckFont));
            }
        }
        int luckTop = luckTokens.isEmpty() ? topA - scaled(2)
                : paintRow(g2, luckTokens, 0, 0, topA - scaled(4) - luckFm.getDescent(), left, right);

        // ---- 本命星曜：直書，由右至左 ----
        paintStars(g2, branch, new Rectangle(left, r.y + pad, right - left, luckTop - scaled(2) - (r.y + pad)));
        g2.dispose();
    }

    /** 一段有顏色、字型的文字。 */
    private record Token(String text, Color color, Font font) {
    }

    /**
     * 由下往上畫一列：右側文字（寬 rightWidth，已由呼叫者畫在 baseline）保持不動，
     * 左側標記放得下時與右側同一行，否則移到上方（仍太長時再依寬度換行）。
     * rightWidth 為 0 表示這列只有左側標記。回傳所畫內容最上緣的 y。
     */
    private int paintRow(Graphics2D g2, List<Token> tokens, int rightWidth, int rightAscent, int baseline,
                         int left, int right) {
        int top = baseline - rightAscent;
        if (tokens.isEmpty()) {
            return top;
        }
        int gap = scaled(5);
        FontMetrics fm = g2.getFontMetrics(tokens.get(0).font());
        int lineHeight = fm.getHeight();
        List<List<Token>> lines = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int width = 0;
        for (Token t : tokens) {
            int w = g2.getFontMetrics(t.font()).stringWidth(t.text());
            if (!current.isEmpty() && left + width + w > right) {
                lines.add(current);
                current = new ArrayList<>();
                width = 0;
            }
            current.add(t);
            width += w + gap;
        }
        lines.add(current);

        boolean sameLine = rightWidth == 0
                || (lines.size() == 1 && left + width + gap <= right - rightWidth);
        int b = sameLine ? baseline + lineHeight : baseline;
        for (int i = lines.size() - 1; i >= 0; i--) {
            b -= lineHeight;
            int cx = left;
            for (Token t : lines.get(i)) {
                g2.setFont(t.font());
                g2.setColor(t.color());
                g2.drawString(t.text(), cx, b);
                cx += g2.getFontMetrics().stringWidth(t.text()) + gap;
            }
        }
        return Math.min(top, b - fm.getAscent());
    }

    private void paintStars(Graphics2D g2, int branch, Rectangle area) {
        Font mainFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(17));
        Font auxFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(16));
        Font minorFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(14));
        Font tagFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(14));
        FontMetrics tagFm = g2.getFontMetrics(tagFont);
        int tagSize = tagFm.getAscent() + scaled(3);
        int gap = scaled(3);

        int x = area.x + area.width;
        int bandTop = area.y;
        int bandHeight = 0;
        for (Star star : chart.starsAt(branch)) {
            Font font = switch (star.category()) {
                case MAIN -> mainFont;
                case MINOR -> minorFont;
                default -> auxFont;
            };
            FontMetrics fm = g2.getFontMetrics(font);
            List<SiHua> tags = new ArrayList<>();
            List<Layer> tagLayers = new ArrayList<>();
            SiHua natal = chart.getNatalSiHua().get(star);
            if (natal != null) {
                tags.add(natal);
                tagLayers.add(Layer.NATAL);
            }
            for (LuckOverlay o : overlays()) {
                SiHua sh = o.getSiHua().get(star);
                if (sh != null) {
                    tags.add(sh);
                    tagLayers.add(o.getLayer());
                }
            }
            String label = star.label();
            int charHeight = fm.getAscent() + scaled(1);
            int colWidth = Math.max(fm.charWidth('紫'), tagSize);
            int colHeight = label.length() * charHeight + tags.size() * (tagSize + scaled(2)) + scaled(2);
            if (x - colWidth < area.x && x < area.x + area.width) {
                bandTop += bandHeight + scaled(4);
                bandHeight = 0;
                x = area.x + area.width;
            }
            int colX = x - colWidth;
            int y = bandTop;
            g2.setFont(font);
            g2.setColor(starColor(star));
            for (int i = 0; i < label.length(); i++) {
                String ch = label.substring(i, i + 1);
                g2.drawString(ch, colX + (colWidth - fm.stringWidth(ch)) / 2, y + fm.getAscent());
                y += charHeight;
            }
            y += scaled(2);
            for (int i = 0; i < tags.size(); i++) {
                paintTag(g2, tags.get(i), tagLayers.get(i), colX + (colWidth - tagSize) / 2, y, tagSize, tagFont);
                y += tagSize + scaled(2);
            }
            bandHeight = Math.max(bandHeight, colHeight);
            x = colX - gap;
        }
    }

    /** 四化標記：本命為實心色塊；行運為該層顏色外框、字以四化顏色顯示。 */
    private void paintTag(Graphics2D g2, SiHua sh, Layer layer, int x, int y, int size, Font font) {
        Color c = siHuaColor(sh);
        int arc = scaled(4);
        if (layer == Layer.NATAL) {
            g2.setColor(c);
            g2.fillRoundRect(x, y, size, size, arc, arc);
            g2.setColor(Color.BLACK);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(x, y, size, size, arc, arc);
            g2.setColor(layerColor(layer));
            g2.setStroke(new BasicStroke(Math.max(1f, (float) (1.4 * paintScale))));
            g2.drawRoundRect(x, y, size - 1, size - 1, arc, arc);
            g2.setColor(c);
        }
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String text = sh.label();
        g2.drawString(text, x + (size - fm.stringWidth(text)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
    }

    /** 文字徽章（例如「身」），右緣對齊 rightX，回傳徽章左緣。 */
    private int drawBadge(Graphics2D g2, String text, int rightX, int baseline, FontMetrics fm, Color color,
                          boolean filled) {
        int padX = scaled(3);
        int w = fm.stringWidth(text) + padX * 2;
        int h = fm.getAscent() + scaled(2);
        int x = rightX - w;
        int y = baseline - fm.getAscent();
        int arc = scaled(5);
        g2.setColor(color);
        if (filled) {
            g2.fillRoundRect(x, y, w, h, arc, arc);
            g2.setColor(Color.BLACK);
        } else {
            g2.drawRoundRect(x, y, w, h, arc, arc);
        }
        g2.setFont(fm.getFont());
        g2.drawString(text, x + padX, baseline);
        return x;
    }

    private void paintCenter(Graphics2D g, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clip(r);
        g2.setColor(CHART_BG);
        g2.fillRect(r.x, r.y, r.width, r.height);

        int pad = scaled(14);
        int x = r.x + pad;
        int y = r.y + pad;
        int maxX = r.x + r.width - pad;
        Font titleFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(20));
        Font labelFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(15));
        Font valueFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(15));
        FontMetrics titleFm = g2.getFontMetrics(titleFont);
        FontMetrics lineFm = g2.getFontMetrics(valueFont);
        int lineHeight = lineFm.getHeight() + scaled(2);

        g2.setFont(titleFont);
        g2.setColor(LIFE_PALACE);
        String title = personName.isEmpty() ? "紫微斗數命盤" : personName + "　紫微斗數命盤";
        g2.drawString(fit(g2, title, maxX - x), x, y + titleFm.getAscent());
        y += titleFm.getHeight() + scaled(6);

        LunarCalendar.LunarDate lunar = chart.getLunarDate();
        String solar = String.format("%d年%d月%d日 %d時%02d分", chart.getBirth().getYear(),
                chart.getBirth().getMonthValue(), chart.getBirth().getDayOfMonth(),
                chart.getBirth().getHour(), chart.getBirth().getMinute());
        String lunarText = lunar.yearGanZhi() + "年 " + lunar.monthName() + lunar.dayName() + " "
                + GanZhi.BRANCHES[chart.getHourBranch()] + "時";
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"陽曆", solar});
        rows.add(new String[]{"農曆", lunarText});
        if (chart.isLateZiShift() || chart.isLeapMonthAsNext()) {
            List<String> notes = new ArrayList<>();
            if (chart.isLateZiShift()) {
                notes.add("23 時為晚子時，以次日排盤");
            }
            if (chart.isLeapMonthAsNext()) {
                notes.add("閏月十六日後，以" + LunarCalendar.MONTH_NAMES[chart.getChartMonth() - 1] + "排盤");
            }
            rows.add(new String[]{"附註", String.join("；", notes)});
        }
        rows.add(new String[]{"命局", chart.getYinYangGender() + "　" + chart.getBureau().label()
                + "　大限" + (chart.isForward() ? "順行" : "逆行")});
        String body = chart.getBodyBranch() == chart.getLifeBranch()
                ? "命身同宮"
                : chart.palaceGanZhi(chart.getBodyBranch()) + "（" + chart.palaceName(chart.getBodyBranch()) + "）";
        rows.add(new String[]{"命宮", chart.palaceGanZhi(chart.getLifeBranch()) + "　身宮：" + body});
        rows.add(new String[]{"命主", chart.getLifeMaster().label() + "　身主：" + chart.getBodyMaster().label()});
        StringBuilder sihua = new StringBuilder();
        for (SiHua sh : SiHua.values()) {
            sihua.append(sh.starFor(chart.getYearStem()).label()).append(sh.label()).append(' ');
        }
        rows.add(new String[]{"生年四化", sihua.toString().trim()});
        drawRows(g2, rows, x, y, maxX, lineHeight, labelFont, valueFont, TEXT);
        y += rows.size() * lineHeight + scaled(8);

        // 與 lifeup 中宮相同：各層行運的干支，以及該層的好運指數與評語
        for (String[] row : luckRows()) {
            Color color = layerColor(Layer.valueOf(row[2]));
            drawRows(g2, List.<String[]>of(new String[]{row[0], row[1]}), x, y, maxX, lineHeight,
                    labelFont, valueFont, color);
            y += lineHeight;
        }
        g2.dispose();
    }

    /** 中宮的行運列：{標籤, 內容, 層}；每層兩列（干支、好運指數與評語）。 */
    List<String[]> luckRows() {
        List<String[]> rows = new ArrayList<>();
        if (chart == null) {
            return rows;
        }
        for (LuckOverlay o : overlays()) {
            rows.add(new String[]{o.getLayer().label(), o.getTitle().substring(o.getLayer().label().length()).trim(),
                    o.getLayer().name()});
            int grade = luckGrade(o.getLayer());
            rows.add(new String[]{"好運指數", grade + "　" + LuckScore.describe(grade), o.getLayer().name()});
        }
        return rows;
    }

    /** 依 lifeup 公式計算某層的好運指數（流年、流月需要上一層）。 */
    private int luckGrade(Layer layer) {
        return switch (layer) {
            case DECADE -> LuckScore.decadeLuck(chart, decade);
            case YEAR -> LuckScore.yearLuck(chart, decade, year);
            case MONTH -> LuckScore.monthLuck(chart, decade, year, month);
            default -> throw new IllegalArgumentException(layer.toString());
        };
    }

    private void drawRows(Graphics2D g2, List<String[]> rows, int x, int y, int maxX, int lineHeight,
                          Font labelFont, Font valueFont, Color valueColor) {
        FontMetrics labelFm = g2.getFontMetrics(labelFont);
        int labelWidth = labelFm.stringWidth("生年四化") + scaled(10);
        for (String[] row : rows) {
            int baseline = y + labelFm.getAscent();
            g2.setFont(labelFont);
            g2.setColor(TEXT_LABEL);
            g2.drawString(row[0], x, baseline);
            g2.setFont(valueFont);
            g2.setColor(valueColor);
            g2.drawString(fit(g2, row[1], maxX - x - labelWidth), x + labelWidth, baseline);
            y += lineHeight;
        }
    }

    /** 文字超出寬度時截斷並加上「…」。 */
    private static String fit(Graphics2D g2, String text, int width) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String s = text;
        while (!s.isEmpty() && fm.stringWidth(s + "…") > width) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    /** 命盤下方的圖例（HTML，供 JLabel 顯示）：星曜類別、四化與行運層的顏色。 */
    public static String legendHtml() {
        StringBuilder sb = new StringBuilder("<html><div style='text-align:center'>");
        Object[][] items = {
                {"主星", STAR_MAIN}, {"六吉", STAR_LUCKY}, {"六煞", STAR_SHA}, {"祿存天馬", STAR_LU_MA},
                {"雜曜", STAR_MINOR}, null,
                {"化祿", SIHUA_LU}, {"化權", SIHUA_QUAN}, {"化科", SIHUA_KE}, {"化忌", SIHUA_JI},
                {"大限", LAYER_DECADE}, {"流年", LAYER_YEAR}, {"流月", LAYER_MONTH}
        };
        for (Object[] item : items) {
            if (item == null) {
                sb.append("<br>");
                continue;
            }
            Color c = (Color) item[1];
            sb.append(String.format("<span style='color:#%02x%02x%02x'>■</span>%s　　",
                    c.getRed(), c.getGreen(), c.getBlue(), item[0]));
        }
        return sb.append("</div></html>").toString();
    }

    private List<LuckOverlay> overlays() {
        List<LuckOverlay> list = new ArrayList<>(3);
        if (decade != null) {
            list.add(decade);
        }
        if (year != null) {
            list.add(year);
        }
        if (month != null) {
            list.add(month);
        }
        return list;
    }

    static Color layerColor(Layer layer) {
        return switch (layer) {
            case DECADE -> LAYER_DECADE;
            case YEAR -> LAYER_YEAR;
            case MONTH -> LAYER_MONTH;
            default -> LIFE_PALACE;
        };
    }

    private static Color siHuaColor(SiHua sh) {
        return switch (sh) {
            case LU -> SIHUA_LU;
            case QUAN -> SIHUA_QUAN;
            case KE -> SIHUA_KE;
            case JI -> SIHUA_JI;
        };
    }

    static Color starColor(Star star) {
        return switch (star.category()) {
            case MAIN -> STAR_MAIN;
            case LUCKY -> STAR_LUCKY;
            case SHA -> STAR_SHA;
            case LU_MA -> STAR_LU_MA;
            case MINOR -> STAR_MINOR;
        };
    }
}
