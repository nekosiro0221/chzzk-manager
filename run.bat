@echo off
REM CHZZK Manager - Windows build and run script
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out -sourcepath src src\app\Main.java
if errorlevel 1 (
    echo [ERROR] Compile failed. Check that JDK 17+ is installed and "javac -version" works.
    pause
    exit /b 1
)
java -cp out app.Main %*
pause
