#!/usr/bin/env bash
# ====================================================================
#  Script de compilation pour Hangman (macOS / Linux)
# ====================================================================
set -e

PATH_TO_FX="${PATH_TO_FX:-$HOME/javafx-sdk/lib}"

MYSQL_JAR="$(ls lib/mysql-connector-*.jar 2>/dev/null | head -n1 || true)"
if [[ -z "$MYSQL_JAR" ]]; then
    echo "[ERREUR] Aucun mysql-connector-*.jar dans lib/."
    exit 1
fi

mkdir -p out

# Rassemble tous les .java sous src/.
find src -name "*.java" > sources.tmp

javac --module-path "$PATH_TO_FX" --add-modules javafx.controls \
      -cp "$MYSQL_JAR" -d out @sources.tmp

rm -f sources.tmp

# Copie les ressources non-Java (theme.css) dans out/ pour que le
# classpath les trouve à l'exécution.
mkdir -p out/hangman/ui
cp src/hangman/ui/theme.css out/hangman/ui/theme.css

echo "[OK] Compilé dans out/"
