@echo off
REM ====================================================================
REM  Chargeur de schema MySQL en un clic pour Pirate's Cove
REM  Double-cliquez sur ce fichier (ou executez-le depuis cmd /
REM  PowerShell avec .\setup-db.bat) pour (re)creer la base
REM  hangman_db et inserer les mots de depart.
REM ====================================================================

setlocal

REM Chemins d'installation courants — le premier trouve gagne.
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
    echo [ERREUR] mysql.exe introuvable aux emplacements habituels.
    echo Modifiez ce script et ajoutez le chemin de votre installation MySQL dans la boucle ci-dessus.
    pause
    exit /b 1
)

echo Utilisation de : %MYSQL_EXE%
echo Chargement de sql\schema.sql dans hangman_db ...
echo (Le mot de passe root MySQL vous sera demande.)
echo.

"%MYSQL_EXE%" -u root -p < "%~dp0sql\schema.sql"

set RC=%ERRORLEVEL%
echo.
if %RC% EQU 0 (
    echo [OK] Schema charge. Vous pouvez lancer le jeu maintenant.
) else (
    echo [ECHEC] mysql a renvoye %RC%
)
pause
exit /b %RC%
