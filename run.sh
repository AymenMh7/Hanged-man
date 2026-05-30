#!/usr/bin/env bash
# ====================================================================
#  Script de lancement pour Hangman (macOS / Linux)
# ====================================================================
set -e

PATH_TO_FX="${PATH_TO_FX:-$HOME/javafx-sdk/lib}"

MYSQL_JAR="$(ls lib/mysql-connector-*.jar 2>/dev/null | head -n1 || true)"
if [[ -z "$MYSQL_JAR" ]]; then
    echo "[ERREUR] Aucun mysql-connector-*.jar dans lib/."
    exit 1
fi

java --module-path "$PATH_TO_FX" --add-modules javafx.controls \
     -cp "out:$MYSQL_JAR" hangman.Main
