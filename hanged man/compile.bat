@echo off
REM ====================================================================
REM  Compile script for Hangman (Windows)
REM  Requires:
REM    - JDK 17+ installed (javac on PATH)
REM    - JavaFX SDK installed; set PATH_TO_FX below (or env var) to its lib/
REM    - MySQL connector jar dropped into the lib\ folder
REM ====================================================================

setlocal enabledelayedexpansion

REM === Edit this if PATH_TO_FX isn't already set ===
if "%PATH_TO_FX%"=="" set PATH_TO_FX=C:\javafx-sdk\lib

REM Find the MySQL connector jar inside lib\
for %%F in (lib\mysql-connector-*.jar) do set MYSQL_JAR=%%F
if "%MYSQL_JAR%"=="" (
    echo [ERROR] No mysql-connector-*.jar found in lib\. Download it from MySQL.
    exit /b 1
)

if not exist out mkdir out

REM Gather every .java under src\. We convert backslashes to forward
REM slashes because javac's @argfile format treats backslash as an
REM escape character (so paths like C:\Users\Setup get mangled).
REM Each path is quoted to survive spaces ("Hanged man", etc.).
if exist sources.tmp del sources.tmp
for /r src %%F in (*.java) do (
    set "P=%%F"
    set "P=!P:\=/!"
    echo "!P!">> sources.tmp
)

javac --module-path "%PATH_TO_FX%" --add-modules javafx.controls ^
      -cp "%MYSQL_JAR%" -d out @sources.tmp

set RC=%ERRORLEVEL%
del sources.tmp >nul 2>&1

if %RC% EQU 0 (
    REM Copy non-Java resources (theme.css, etc.) into out\ so the
    REM classpath sees them at runtime.
    if not exist out\hangman\ui mkdir out\hangman\ui
    copy /Y src\hangman\ui\theme.css out\hangman\ui\theme.css >nul
    echo.
    echo [OK] Compiled into out\
) else (
    echo.
    echo [FAIL] javac returned %RC%
)
exit /b %RC%
