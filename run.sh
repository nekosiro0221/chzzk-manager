#!/bin/sh
# CHZZK Manager - macOS / Linux build and run script
cd "$(dirname "$0")" || exit 1
rm -rf out
javac -encoding UTF-8 -d out -sourcepath src src/app/Main.java || {
    echo "[ERROR] Compile failed. Check that JDK 17+ is installed and 'javac -version' works."
    exit 1
}
java -cp out app.Main "$@"
