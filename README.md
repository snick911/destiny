# destiny

子平八字（四柱）排盤程式，使用 Java Swing 打造，內建護眼暗色主題。

輸入西元出生年、月、日、時（可選填分鐘），程式會依「立春」判定流年、
以十二節劃分月柱、以儒略日數推算日柱、並依五鼠遁推算時柱，顯示年柱、
月柱、日柱、時柱的天干地支與五行。

## 編譯與執行

需要 JDK 17 以上（純 JDK，不依賴任何第三方套件）。

```bash
# 編譯
javac -d out -encoding UTF-8 $(find src/main/java -name "*.java")

# 執行
java -cp out com.destiny.bazi.Main
```

## 排盤規則說明

- **年柱**：以「立春」時刻為界，而非西曆 1 月 1 日或農曆春節。
- **月柱**：以十二個「節」（立春、驚蟄、清明…）劃分月支交界，
  月支固定為寅卯辰巳午未申酉戌亥子丑；月干依年干以「五虎遁」推算。
- **日柱**：以儒略日數（Julian Day Number）對 60 取餘推算干支，
  並依「晚子時」慣例，23:00~23:59 出生者日柱順延至次日推算。
- **時柱**：時支依出生時刻對應十二時辰，時干依日干以「五鼠遁」推算。
- 節氣時刻採天文平均法估算，適用於西元 1900~2100 年間，
  時刻誤差通常在數十分鐘內，足以判斷排盤所需的年柱與月柱交界。
- 所有輸入時間預設視為當地（北京時間 UTC+8）出生時刻。

## 專案結構

```
src/main/java/com/destiny/bazi/
├── Main.java                 # 程式進入點
├── core/
│   ├── GanZhi.java            # 天干地支五行生肖資料表
│   ├── Pillar.java            # 單柱（干支）資料模型
│   ├── SolarTerm.java         # 二十四節氣時刻推算
│   ├── BaziCalculator.java    # 排盤核心邏輯
│   └── BaziResult.java        # 排盤結果
└── ui/
    ├── DarkTheme.java          # 護眼暗色主題配色
    ├── PillarPanel.java        # 單柱顯示卡片
    └── BaziFrame.java          # 主視窗
```
