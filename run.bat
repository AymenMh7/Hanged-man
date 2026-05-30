@echo off
REM ====================================================================
REM  Script de lancement pour Hangman (Windows)
REM  Executez compile.bat en premier.
REM ====================================================================

setlocal enabledelayedexpansion

if "%PATH_TO_FX%"=="" set PATH_TO_FX=C:\javafx-sdk\lib

for %%F in (lib\mysql-connector-*.jar) do set MYSQL_JAR=%%F
if "%MYSQL_JAR%"=="" (
    echo [ERREUR] Aucun mysql-connector-*.jar trouve dans lib\.
    exit /b 1
)

java --module-path "%PATH_TO_FX%" --add-modules javafx.controls ^
     -cp "out;%MYSQL_JAR%" hangman.Main
