#!/usr/bin/env bash
# 編譯並打包：產生可執行的 dist/destiny-bazi.jar，以及可直接分享的
# dist/destiny-bazi-<版本>.zip（內含 JAR、啟動檔、README、CHANGELOG）。
# 需要 JDK 17 以上與 zip 指令。版本號讀自 AppInfo.java。
set -euo pipefail
cd "$(dirname "$0")"

NAME=destiny-bazi
VERSION=$(sed -n 's/.*String VERSION = "\(.*\)";.*/\1/p' src/main/java/com/destiny/bazi/AppInfo.java)
if [ -z "$VERSION" ]; then
    echo "無法從 AppInfo.java 讀取版本號" >&2
    exit 1
fi

BUILD=build
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" dist

echo "編譯 $NAME $VERSION ..."
find src/main/java -name '*.java' > "$BUILD/sources.txt"
javac --release 17 -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/sources.txt"

# 測試未全部通過就停止打包。沒有顯示環境時若有 xvfb-run，以虛擬顯示執行介面測試。
echo "執行測試 ..."
mkdir -p "$BUILD/test-classes"
find src/test/java -name '*.java' > "$BUILD/test-sources.txt"
javac --release 17 -encoding UTF-8 -cp "$BUILD/classes" -d "$BUILD/test-classes" @"$BUILD/test-sources.txt"
RUNNER=()
if [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null 2>&1; then
    RUNNER=(xvfb-run -a)
fi
"${RUNNER[@]}" java -Dstdout.encoding=UTF-8 -cp "$BUILD/classes:$BUILD/test-classes" com.destiny.bazi.tests.AllTests

cat > "$BUILD/MANIFEST.MF" <<EOF
Main-Class: com.destiny.bazi.Main
Implementation-Title: $NAME
Implementation-Version: $VERSION
EOF
jar --create --file "dist/$NAME.jar" --manifest "$BUILD/MANIFEST.MF" -C "$BUILD/classes" .

PKG="$NAME-$VERSION"
mkdir -p "$BUILD/$PKG"
cp "dist/$NAME.jar" README.md CHANGELOG.md packaging/* "$BUILD/$PKG/"
chmod +x "$BUILD/$PKG/run-mac-linux.sh"
rm -f "dist/$PKG.zip"
(cd "$BUILD" && zip -qrX "../dist/$PKG.zip" "$PKG")

echo "完成："
echo "  dist/$NAME.jar"
echo "  dist/$PKG.zip"
