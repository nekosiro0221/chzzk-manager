@echo off
REM CHZZK Manager - run automated self tests (Windows)
cd /d "%~dp0"
if exist out-test rmdir /s /q out-test
javac -encoding UTF-8 -d out-test -sourcepath "src;test" test\selftest\SelfTest.java
if errorlevel 1 (
    echo [ERROR] Compile failed.
    pause
    exit /b 1
)
java -cp out-test selftest.SelfTest
pause
