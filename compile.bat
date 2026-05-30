@echo off
REM ====================================================================
REM  Script de compilation pour Hangman (Windows)
REM  Prérequis :
REM    - JDK 17+ installé (javac dans le PATH)
REM    - SDK JavaFX installé ; définir PATH_TO_FX ci-dessous (ou
REM      en variable d'environnement) vers son dossier lib/
REM    - Jar du connecteur MySQL déposé dans le dossier lib\
REM ====================================================================

setlocal enabledelayedexpansion

REM === Modifiez ceci si PATH_TO_FX n'est pas déjà défini ===
if "%PATH_TO_FX%"=="" set PATH_TO_FX=C:\javafx-sdk\lib

REM Cherche le jar du connecteur MySQL dans lib\
for %%F in (lib\mysql-connector-*.jar) do set MYSQL_JAR=%%F
if "%MYSQL_JAR%"=="" (
    echo [ERREUR] Aucun mysql-connector-*.jar trouve dans lib\. Telechargez-le depuis MySQL.
    exit /b 1
)

if not exist out mkdir out

REM Rassemble tous les .java sous src\. On convertit les antislashes
REM en slashs car le format @argfile de javac traite l'antislash comme
REM un caractere d'echappement (donc des chemins comme C:\Users\Setup
REM sont massacres). Chaque chemin est entoure de guillemets pour
REM survivre aux espaces ("Hanged man", etc.).
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
    REM Copie les ressources non-Java (theme.css, etc.) dans out\ pour
    REM que le classpath les voie a l'execution.
    if not exist out\hangman\ui mkdir out\hangman\ui
    copy /Y src\hangman\ui\theme.css out\hangman\ui\theme.css >nul
    echo.
    echo [OK] Compile dans out\
) else (
    echo.
    echo [ECHEC] javac a renvoye %RC%
)
exit /b %RC%
