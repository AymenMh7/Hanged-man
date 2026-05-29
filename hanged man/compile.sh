#!/usr/bin/env bash
# ====================================================================
#  Compile script for Hangman (macOS / Linux)
# ====================================================================
set -e

PATH_TO_FX="${PATH_TO_FX:-$HOME/javafx-sdk/lib}"

MYSQL_JAR="$(ls lib/mysql-connector-*.jar 2>/dev/null | head -n1 || true)"
if [[ -z "$MYSQL_JAR" ]]; then
    echo "[ERROR] No mysql-connector-*.jar in lib/."
    exit 1
fi

mkdir -p out

# Gather every .java under src/.
find src -name "*.java" > sources.tmp

javac --module-path "$PATH_TO_FX" --add-modules javafx.controls \
      -cp "$MYSQL_JAR" -d out @sources.tmp

rm -f sources.tmp

# Copy non-Java resources (theme.css) into out/ so the classpath
# can find them at runtime.
mkdir -p out/hangman/ui
cp src/hangman/ui/theme.css out/hangman/ui/theme.css

echo "[OK] Compiled into out/"
