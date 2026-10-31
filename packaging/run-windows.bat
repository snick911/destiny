@echo off
chcp 65001 >nul
rem 子平八字排盤啟動檔（Windows），需已安裝 Java 17 以上。
where javaw >nul 2>nul
if errorlevel 1 (
    echo 找不到 Java，請先安裝 Java 17 以上版本。
    pause
    exit /b 1
)
start "" javaw -jar "%~dp0destiny-bazi.jar"
