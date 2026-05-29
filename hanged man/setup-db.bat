@echo off
REM ====================================================================
REM  One-click MySQL schema loader for Pirate's Cove
REM  Double-click this file (or run from cmd / PowerShell with .\setup-db.bat)
REM  to (re)create the hangman_db database and seed words.
REM ====================================================================

setlocal

REM Common install paths — first one found wins.
set "MYSQL_EXE="
for %%P in (
    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
    "C:\xampp\mysql\bin\mysql.exe"
) do (
    if exist %%P set "MYSQL_EXE=%%~P"
)

if "%MYSQL_EXE%"=="" (
    echo [ERROR] Couldn't find mysql.exe in the usual locations.
    echo Edit this script and add your MySQL install path to the loop above.
    pause
    exit /b 1
)

echo Using: %MYSQL_EXE%
echo Loading sql\schema.sql into hangman_db ...
echo (You'll be prompted for your MySQL root password.)
echo.

"%MYSQL_EXE%" -u root -p < "%~dp0sql\schema.sql"

set RC=%ERRORLEVEL%
echo.
if %RC% EQU 0 (
    echo [OK] Schema loaded. You can launch the game now.
) else (
    echo [FAIL] mysql returned %RC%
)
pause
exit /b %RC%
