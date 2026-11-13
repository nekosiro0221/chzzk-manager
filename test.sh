#!/bin/sh
# CHZZK Manager - run automated self tests (macOS / Linux)
cd "$(dirname "$0")" || exit 1
rm -rf out-test
javac -encoding UTF-8 -d out-test -sourcepath "src:test" test/selftest/SelfTest.java || exit 1
java -cp out-test selftest.SelfTest
