@echo off
REM ====================================================================
REM  Run script for Hangman (Windows)
REM  Run compile.bat first.
REM ====================================================================

setlocal enabledelayedexpansion

if "%PATH_TO_FX%"=="" set PATH_TO_FX=C:\javafx-sdk\lib

for %%F in (lib\mysql-connector-*.jar) do set MYSQL_JAR=%%F
if "%MYSQL_JAR%"=="" (
    echo [ERROR] No mysql-connector-*.jar found in lib\.
    exit /b 1
)

java --module-path "%PATH_TO_FX%" --add-modules javafx.controls ^
     -cp "out;%MYSQL_JAR%" hangman.Main
