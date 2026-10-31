#!/bin/sh
# 子平八字排盤啟動檔（macOS / Linux），需已安裝 Java 17 以上。
cd "$(dirname "$0")" || exit 1
if ! command -v java >/dev/null 2>&1; then
    echo "找不到 Java，請先安裝 Java 17 以上版本。" >&2
    exit 1
fi
exec java -jar destiny-bazi.jar
