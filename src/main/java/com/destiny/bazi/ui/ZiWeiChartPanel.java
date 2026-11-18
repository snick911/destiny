package com.destiny.bazi.ui;

import com.destiny.bazi.core.GanZhi;
import com.destiny.bazi.core.LunarCalendar;
import com.destiny.bazi.ziwei.Decade;
import com.destiny.bazi.ziwei.Layer;
import com.destiny.bazi.ziwei.LuckOverlay;
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
 * 命主資料。每宮以直書由右至左列出星曜，星名下方標示四化（本命為實心色塊，
 * 大限／流年／流月為該層顏色的外框），宮位底部標示宮干支、宮名、大限歲數，
 * 以及疊上的大限／流年／流月宮名、行運星曜與斗君流月。點選宮位可切換到該宮的大限。
 */
public final class ZiWeiChartPanel extends JComponent {

    private static final int BASE_CELL_WIDTH = 172;
    private static final int BASE_CELL_HEIGHT = 200;
    private static final int MAX_CELL_WIDTH = 250;
    private static final int MIN_CELL_WIDTH = 100;

    // 地支（子=0）→ 方格欄、列
    private static final int[][] GRID = {
            {2, 3}, {1, 3}, {0, 3}, {0, 2}, {0, 1}, {0, 0},
            {1, 0}, {2, 0}, {3, 0}, {3, 1}, {3, 2}, {3, 3}
    };

    public static final Color LAYER_DECADE = new Color(0xE5, 0xA5, 0x5D);
    public static final Color LAYER_YEAR = new Color(0x5F, 0xB3, 0xA1);
    public static final Color LAYER_MONTH = new Color(0xD9, 0x82, 0xB5);

    private static final Color SIHUA_LU = new Color(0x8F, 0xBC, 0x6B);
    private static final Color SIHUA_QUAN = new Color(0xC7, 0x9B, 0xD9);
    private static final Color SIHUA_KE = new Color(0x6F, 0xA8, 0xDC);
    private static final Color SIHUA_JI = new Color(0xE0, 0x6C, 0x75);

    private static final Color STAR_MAIN = new Color(0xEC, 0xEF, 0xF4);
    private static final Color CELL_BG = DarkTheme.BG_PANEL;

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
        setBackground(DarkTheme.BG_CARD);
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

        g2.setColor(DarkTheme.BORDER);
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

        // 底色：滑鼠停留時轉亮；最內層行運命宮加上該層顏色的淡底
        g2.setColor(branch == hoverBranch ? DarkTheme.BG_CARD_HIGHLIGHT : CELL_BG);
        g2.fillRect(r.x, r.y, r.width, r.height);
        LuckOverlay deepest = month != null ? month : year != null ? year : decade;
        if (deepest != null && deepest.getLifeBranch() == branch) {
            Color c = layerColor(deepest.getLayer());
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 38));
            g2.fillRect(r.x, r.y, r.width, r.height);
        }
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
        Font infoFont = DarkTheme.BASE_FONT.deriveFont(fontSize(13));
        Font nameFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(15));
        FontMetrics nameFm = g2.getFontMetrics(nameFont);
        int left = r.x + pad;
        int right = r.x + r.width - pad;

        // ---- 最下列：本命宮名與身宮（右）、行運宮名（左；放不下時移到上一行） ----
        int baselineB = r.y + r.height - pad - nameFm.getDescent();
        String palaceName = chart.palaceName(branch);
        boolean isLife = branch == chart.getLifeBranch();
        int x = right;
        if (branch == chart.getBodyBranch()) {
            x = drawBadge(g2, "身", x, baselineB, nameFm, DarkTheme.ACCENT, true);
            x -= scaled(4);
        }
        g2.setFont(nameFont);
        g2.setColor(isLife ? DarkTheme.ACCENT : DarkTheme.FG_PRIMARY);
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
        Font gzFont = nameFont.deriveFont(Font.PLAIN);
        g2.setFont(gzFont);
        g2.setColor(DarkTheme.FG_SECONDARY);
        int gzWidth = g2.getFontMetrics().stringWidth(gz);
        g2.drawString(gz, right - gzWidth, baselineA);
        Decade d = chart.decadeAt(branch);
        List<Token> infoTokens = new ArrayList<>();
        infoTokens.add(new Token(d.startAge() + "~" + d.endAge(), DarkTheme.FG_SECONDARY, infoFont));
        if (year != null && year.monthAt(branch) > 0) {
            infoTokens.add(new Token(LunarCalendar.MONTH_NAMES[year.monthAt(branch) - 1], LAYER_YEAR, infoFont));
        }
        if (year != null && year.getSmallLimitBranch() == branch) {
            infoTokens.add(new Token("小限", LAYER_YEAR, infoFont));
        }
        int topA = paintRow(g2, infoTokens, gzWidth, g2.getFontMetrics(gzFont).getAscent(), baselineA, left, right);

        // ---- 行運星曜（大祿、流羊…）：在上一列之上，由下往上換行 ----
        Font luckFont = DarkTheme.BASE_FONT.deriveFont(fontSize(12.5));
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
        Font mainFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(16));
        Font auxFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(14.5f));
        Font minorFont = DarkTheme.BASE_FONT.deriveFont(fontSize(13));
        Font tagFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(11.5f));
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
            g2.setColor(DarkTheme.BG_APP);
        } else {
            g2.setColor(DarkTheme.BG_APP);
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
            g2.setColor(DarkTheme.BG_APP);
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
        g2.setColor(DarkTheme.BG_CARD);
        g2.fillRect(r.x, r.y, r.width, r.height);

        int pad = scaled(14);
        int x = r.x + pad;
        int y = r.y + pad;
        Font titleFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(19));
        Font labelFont = DarkTheme.BASE_FONT.deriveFont(fontSize(14));
        Font valueFont = DarkTheme.BASE_FONT.deriveFont(Font.BOLD, fontSize(14));
        FontMetrics titleFm = g2.getFontMetrics(titleFont);
        FontMetrics lineFm = g2.getFontMetrics(valueFont);
        int lineHeight = lineFm.getHeight() + scaled(2);

        g2.setFont(titleFont);
        g2.setColor(DarkTheme.ACCENT);
        String title = personName.isEmpty() ? "紫微斗數命盤" : personName + "　紫微斗數命盤";
        g2.drawString(title, x, y + titleFm.getAscent());
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
        drawRows(g2, rows, x, y, r.x + r.width - pad, lineHeight, labelFont, valueFont, DarkTheme.FG_PRIMARY);
        y += rows.size() * lineHeight + scaled(6);

        List<String[]> luckRows = new ArrayList<>();
        List<Color> luckColors = new ArrayList<>();
        for (LuckOverlay o : overlays()) {
            StringBuilder sb = new StringBuilder(o.getTitle().substring(o.getLayer().label().length()).trim());
            luckRows.add(new String[]{o.getLayer().label(), sb.toString()});
            luckColors.add(layerColor(o.getLayer()));
            StringBuilder sh = new StringBuilder();
            for (SiHua s : SiHua.values()) {
                sh.append(s.starFor(o.getStem()).label()).append(s.label()).append(' ');
            }
            luckRows.add(new String[]{"", sh.toString().trim()});
            luckColors.add(layerColor(o.getLayer()));
        }
        for (int i = 0; i < luckRows.size(); i++) {
            drawRows(g2, List.<String[]>of(luckRows.get(i)), x, y, r.x + r.width - pad, lineHeight,
                    labelFont, valueFont, luckColors.get(i));
            y += lineHeight;
        }

        // 圖例佔兩行，放不下（例如視窗極窄）時省略，避免與上方文字重疊
        FontMetrics legendFm = g2.getFontMetrics(DarkTheme.BASE_FONT.deriveFont(fontSize(12)));
        int legendTop = r.y + r.height - pad - legendFm.getHeight() * 2 - scaled(2);
        if (y <= legendTop) {
            paintLegend(g2, x, r.y + r.height - pad, r.x + r.width - pad);
        }
        g2.dispose();
    }

    private void drawRows(Graphics2D g2, List<String[]> rows, int x, int y, int maxX, int lineHeight,
                          Font labelFont, Font valueFont, Color valueColor) {
        FontMetrics labelFm = g2.getFontMetrics(labelFont);
        int labelWidth = labelFm.stringWidth("生年四化") + scaled(10);
        for (String[] row : rows) {
            int baseline = y + labelFm.getAscent();
            g2.setFont(labelFont);
            g2.setColor(DarkTheme.FG_SECONDARY);
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

    /** 圖例：星曜類別、四化與行運層的顏色（由下往上兩列）。 */
    private void paintLegend(Graphics2D g2, int x, int bottom, int maxX) {
        Font font = DarkTheme.BASE_FONT.deriveFont(fontSize(12));
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int sw = scaled(10);
        int gap = scaled(10);

        String[] catLabels = {"主星", "六吉", "六煞", "祿馬", "雜曜"};
        Color[] catColors = {STAR_MAIN, DarkTheme.ELEMENT_WOOD, DarkTheme.ELEMENT_FIRE, DarkTheme.ELEMENT_EARTH,
                DarkTheme.FG_SECONDARY};
        String[] layerLabels = {"祿", "權", "科", "忌", "大限", "流年", "流月"};
        Color[] layerColors = {SIHUA_LU, SIHUA_QUAN, SIHUA_KE, SIHUA_JI, LAYER_DECADE, LAYER_YEAR, LAYER_MONTH};

        int baseline2 = bottom - fm.getDescent();
        int baseline1 = baseline2 - fm.getHeight() - scaled(2);
        drawLegendRow(g2, catLabels, catColors, x, baseline1, maxX, sw, gap, fm);
        drawLegendRow(g2, layerLabels, layerColors, x, baseline2, maxX, sw, gap, fm);
    }

    private void drawLegendRow(Graphics2D g2, String[] labels, Color[] colors, int x, int baseline, int maxX,
                               int sw, int gap, FontMetrics fm) {
        int cx = x;
        for (int i = 0; i < labels.length; i++) {
            int w = sw + scaled(4) + fm.stringWidth(labels[i]);
            if (cx + w > maxX) {
                break;
            }
            g2.setColor(colors[i]);
            g2.fillRoundRect(cx, baseline - fm.getAscent() + (fm.getAscent() - sw) / 2, sw, sw, scaled(3), scaled(3));
            g2.setColor(DarkTheme.FG_SECONDARY);
            g2.drawString(labels[i], cx + sw + scaled(4), baseline);
            cx += w + gap;
        }
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
            default -> DarkTheme.ACCENT;
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

    private static Color starColor(Star star) {
        return switch (star.category()) {
            case MAIN -> STAR_MAIN;
            case LUCKY -> DarkTheme.ELEMENT_WOOD;
            case SHA -> DarkTheme.ELEMENT_FIRE;
            case LU_MA -> DarkTheme.ELEMENT_EARTH;
            case MINOR -> DarkTheme.FG_SECONDARY;
        };
    }
}
