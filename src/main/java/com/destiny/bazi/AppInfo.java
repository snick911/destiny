package com.destiny.bazi;

/**
 * 程式名稱與版本資訊。
 *
 * <p>{@link #VERSION} 為版本號的唯一來源：視窗標題、「關於」對話框、
 * 儲存檔以及 build.sh 產生的 JAR manifest 與壓縮檔名皆由此讀取。
 * 發佈新版時請同步更新 CHANGELOG.md。</p>
 */
public final class AppInfo {

    public static final String NAME = "子平八字排盤";

    public static final String VERSION = "1.0.1";

    private AppInfo() {
    }
}
