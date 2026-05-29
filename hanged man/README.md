# Pirate's Cove — Java Hangman

A two-mode Hangman game with a pirate-cove visual theme, built in
plain Java with JavaFX for the UI and MySQL/JDBC for the leaderboard.
Implements an OOP class structure with a clean separation of
concerns — enums, DAO layer, domain models, manager controllers, and a
themed JavaFX view.

**Modes:** Single-player against the clock, or 1v1 duels between two
captains.
**Scoring:** Combined score from remaining chances + speed bonus.
**Persistence:** Top-10 leaderboard per difficulty stored in MySQL.

---

## Table of contents

1. [Prerequisites](#1-prerequisites)
2. [Quick start (TL;DR)](#2-quick-start-tldr)
3. [Detailed setup](#3-detailed-setup)
4. [Running the game](#4-running-the-game)
5. [How to play](#5-how-to-play)
6. [Project structure](#6-project-structure)
7. [Architecture & design notes](#7-architecture--design-notes)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Prerequisites

| Tool | Version | Why |
|------|---------|-----|
| **JDK** | **17 LTS** or **21 LTS** (recommended) | To compile + run Java |
| **JavaFX SDK** | 17 / 21 (match your JDK) | UI toolkit, no longer bundled with the JDK |
| **MySQL Server** | 8.0+ | Stores the dictionary + leaderboard |
| **MySQL Connector/J** | 8.x or 9.x | JDBC driver — a single `.jar` file |

> **Heads up on JDK 22+:** The game compiles fine with newer JDKs,
> but some JavaFX edge cases are still ironing out. If you hit weird
> graphics errors, drop down to JDK 21 LTS.

---

## 2. Quick start (TL;DR)

For experienced users — full walkthrough is in section 3.

```bash
# 1. Clone
git clone <your-repo-url>
cd "hanged man"

# 2. Drop the MySQL connector jar into lib/
#    (download from https://dev.mysql.com/downloads/connector/j/)

# 3. Unzip JavaFX SDK to C:\javafx-sdk  (or set PATH_TO_FX env var)

# 4. Copy the credentials template and edit it
copy config.properties.example config.properties
# then open config.properties and set db.password

# 5. Load the schema (Windows: double-click setup-db.bat)
.\setup-db.bat

# 6. Build + run
.\compile.bat
.\run.bat
```

---

## 3. Detailed setup

### 3.1 Clone the repository

```bash
git clone <your-repo-url>
cd "hanged man"
```

You'll see this top-level layout:

```
hanged man/
├── src/                       — Java source files
├── sql/schema.sql             — MySQL schema + seed words
├── lib/                       — (empty — YOU put jars here)
├── .vscode/                   — VS Code config (auto-configures launch)
├── config.properties.example  — credentials template
├── compile.bat / compile.sh   — build scripts
├── run.bat     / run.sh       — launch scripts
├── setup-db.bat               — one-click schema loader (Windows)
└── README.md
```

### 3.2 Install the JDK

Download from [Adoptium Temurin](https://adoptium.net/temurin/releases/):

- **OS:** Windows · **Architecture:** x64 · **Package:** JDK
- **Version:** 17 (LTS) or 21 (LTS)
- Install via the `.msi`. In the installer, **enable "Set JAVA_HOME"
  and "Add to PATH"** (often off by default).

Verify in a **new** PowerShell window:

```powershell
java -version
javac -version
```

### 3.3 Install JavaFX SDK

1. Go to [openjfx.io](https://openjfx.io/) → **Download**
2. **Version:** match your JDK (21 if you installed JDK 21)
   · **OS:** Windows · **Architecture:** x64 · **Type:** SDK
3. Unzip it. You'll get a folder like `javafx-sdk-21.0.5/`
4. Move/rename it so the path is exactly:
   ```
   C:\javafx-sdk
   ```
   (so `C:\javafx-sdk\lib\javafx.controls.jar` exists)

   If you want a different location, set the env var `PATH_TO_FX` or
   edit the top of `compile.bat` / `run.bat`.

### 3.4 Download MySQL Connector/J

1. <https://dev.mysql.com/downloads/connector/j/>
2. **Operating System:** "Platform Independent"
3. Download the **ZIP Archive** (~5 MB)
4. Click "No thanks, just start my download" (no Oracle account needed)
5. Unzip. Find the jar — `mysql-connector-j-X.Y.Z.jar`
6. Copy **just the .jar** into your project's `lib/` folder

### 3.5 Install MySQL Server

1. <https://dev.mysql.com/downloads/installer/>
2. Run the installer. Choose **"Server only"** setup type
3. Keep the default port **3306**
4. Pick **Strong Password Encryption**
5. **Set a root password** — write it down
6. Configure as a Windows Service (default)
7. Verify it's running: **Win+R → `services.msc` → find "MySQL80"**, should say *Running*

### 3.6 Set your credentials

```powershell
copy config.properties.example config.properties
```

Open `config.properties` in any editor and set:

```properties
db.password=your_root_password_here
```

> `config.properties` is **git-ignored** — your password never gets
> pushed. Anyone cloning the repo copies the `.example` and supplies
> their own password.

### 3.7 Load the database schema

Pick whichever works for you:

**Option A — One-click (recommended for Windows):**

Double-click **`setup-db.bat`** in the project folder. It finds your
MySQL install automatically, prompts for your password, runs the
schema, and reports success.

**Option B — PowerShell:**

```powershell
Get-Content ".\sql\schema.sql" | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

(Adjust the path if you have MySQL 8.4 or a different version.)

**Option C — Interactive:**

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

Then at the `mysql>` prompt:

```sql
source sql/schema.sql
```

Either way, you should see a bunch of "Query OK" lines. The script
creates `hangman_db`, the `Dictionary` and `Leaderboard` tables, and
seeds 40 sample words across the four difficulties.

---

## 4. Running the game

Three options — pick whichever suits your workflow.

### 4.1 From the command line (zero IDE required)

```powershell
.\compile.bat
.\run.bat
```

(On macOS/Linux: `./compile.sh && ./run.sh`)

`compile.bat` finds the MySQL jar automatically, copies the CSS theme
into `out/`, and produces compiled classes. `run.bat` then launches
with the right JavaFX module path.

### 4.2 From VS Code

The repo ships `.vscode/launch.json` and `.vscode/settings.json` that
auto-configure JavaFX + MySQL on the classpath.

1. Install **Extension Pack for Java** (Microsoft)
2. **File → Open Folder...** → pick the project
3. Press **F5** (or use the Run menu → **Launch Pirate's Cove**)

If your JavaFX SDK isn't at `C:\javafx-sdk`, edit the path in both
`.vscode/settings.json` and `.vscode/launch.json`.

### 4.3 From IntelliJ IDEA

**Open the project:** `File → Open` → select the project folder.

**Add JavaFX as a library:**

1. **Ctrl+Alt+Shift+S** (Project Structure)
2. **Libraries** → **+** → **Java**
3. Navigate to `C:\javafx-sdk\lib` — select the **lib folder itself** → OK
4. When prompted, attach to your module → OK

**Add the MySQL connector as a library:**

5. Still in Libraries: **+** → **Java**
6. Navigate to `<project>\lib\` — click the **`.jar` file directly**
   (not the folder) → OK
7. Attach to your module → OK
8. **Apply**

**Verify both are attached:**

9. Switch to the **Modules** panel → your module → **Dependencies** tab
10. You should see entries for JavaFX and the MySQL jar — **no red text**.
    If MySQL shows red, it means the path is wrong — remove it and
    re-add pointing at the actual jar file (not the folder).
11. Close Project Structure

**Create the run configuration:**

12. **Run → Edit Configurations** → **+** → **Application**
13. Fields:
    - **Name:** `Pirate's Cove`
    - **Main class:** `hangman.Main`
    - **Working directory:** the project root (defaults are usually fine)
    - **VM options** *(click "Modify options → Add VM options" if hidden)*:
      ```
      --module-path "C:/javafx-sdk/lib" --add-modules javafx.controls
      ```
14. **Apply** → **OK**

**Launch:** click the green ▶ button at the top right.

---

## 5. How to play

- **Set Sail Alone** — pick a difficulty (Cabin Boy / First Mate /
  Captain / Dread Pirate). You get fewer chances at higher
  difficulties. Win fast with chances to spare to crack the top 10.
- **Duel of Captains** — enter two captain names, pick a difficulty,
  pick how many rounds each. Each round one captain types a secret
  word (password-style) and the other guesses. Chances = unique
  letters in the word, capped at 7.
- **Tiebreaker** — if a duel ends tied, sudden death pulls a word
  from the next-harder difficulty.
- **Wall of Legends** — top 10 scores per difficulty, fetched live
  from MySQL.

**Score formula:** `(remainingChances × 100) + max(0, 1000 − seconds × 10)`

So an EASY win with 10 chances left in 5 seconds = `1000 + 950 = 1950`.
The same formula awards multiplayer doubloons per solved round.

---

## 6. Project structure

```
src/hangman/
├── Main.java                       — entry point (IDE-friendly launcher)
├── enums/Difficulty.java
├── db/
│   ├── DBConnection.java           — singleton JDBC connection
│   ├── DictionaryDAO.java          — word fetcher
│   └── ScoreDAO.java               — leaderboard reader/writer
├── models/
│   ├── GameSession.java            — the dumb game engine
│   ├── Player.java
│   └── ScoreRecord.java
├── services/ScoreBoard.java        — business wrapper over ScoreDAO
├── managers/
│   ├── GameManager.java            — abstract base
│   ├── SinglePlayerManager.java
│   └── MultiplayerManager.java     — 1v1 + tiebreaker logic
└── ui/
    ├── GameWindow.java             — main JavaFX Application
    ├── BackgroundPane.java         — scenic beach backdrop
    ├── HangmanCanvas.java          — driftwood gibbet + skeleton pirate
    ├── VirtualKeyboard.java        — clickable A–Z wooden coins
    ├── MatchEndMenu.java           — post-match popup
    └── theme.css                   — pirate-cove styling
```

---

## 7. Architecture & design notes

- **GameSession is dumb.** It owns a word, a chance count, and a
  guess history. It doesn't know whether it's solo or multiplayer —
  the managers wrap that context around it.
- **DAO + Singleton DB.** All SQL lives in `db/`. The rest of the
  code never sees `java.sql.*` types, so MySQL could be swapped for
  another store without touching the game logic.
- **Inverted MP chances.** Counting unique letters means rarer
  characters grant *more* chances; the cap at 7 stops long pangrams
  from being trivial.
- **HangmanCanvas scales by ratio.** The figure fills in across
  4 body stages, mapped onto whatever `maxChances` the difficulty
  gave you — same canvas works for 4-chance INSANE and 10-chance EASY.
- **One Scene, swap root.** The window keeps its size/maximize state
  across screen transitions because we replace `Scene.setRoot(...)`
  rather than building new scenes.

---

## 8. Troubleshooting

### "JavaFX runtime components are missing"

JavaFX isn't on the module path.

- **From CLI:** check `PATH_TO_FX` or the value at the top of
  `compile.bat` — it should point at the JavaFX SDK's `lib\` folder.
- **From IntelliJ:** add the VM options to your run config
  (section 4.3, step 13).
- **From VS Code:** make sure you're launching via **F5** (uses
  `launch.json`), not the inline ▶ in the editor (which can skip the
  VM args).

### "package javafx.geometry does not exist"

JavaFX isn't on the **compile** classpath. In IntelliJ, your
**Project Structure → Modules → Dependencies** tab has no JavaFX
entry. Re-do step 4.3 (1–4) to add it.

### "MySQL JDBC Driver not found. Place mysql-connector-j-*.jar in lib/"

The MySQL jar isn't on the classpath at runtime.

- **From CLI:** is there a `mysql-connector-j-*.jar` file in `lib/`?
- **From IntelliJ:** is it showing as red in your Dependencies tab?
  Remove and re-add pointing at the actual jar file (not the lib folder).

### "Couldn't check the leaderboard: Unknown column 'score'"

The schema is out of date. Re-run `setup-db.bat` (or
`Get-Content .\sql\schema.sql | mysql -u root -p`). The DAO expects
a `score` column, not `time_elapsed`.

### "Could not connect to MySQL — check config.properties"

Either MySQL isn't running, the port/user/password is wrong, or the
database doesn't exist:

- Verify MySQL is running (`services.msc` → MySQL80)
- Re-check `db.password` in `config.properties` matches your root password
- Make sure you ran `setup-db.bat` (creates the `hangman_db` database)

### Game window opens, then immediately closes

Run from the command line so you can see the stack trace
(`.\run.bat`). The most common cause is a missing jar — the error
will name the missing class.

### Title bar says "PIRATE'S COVE" but the menu card is empty / blank

Window is too small for the parchment card. Maximize the window, or
resize it larger by dragging the corner. The minimum is 720 × 620.

### PowerShell error: `compile.bat : The term 'compile.bat' is not recognized`

PowerShell needs `.\` to run scripts in the current folder. Use:

```powershell
.\compile.bat
```

### PowerShell error redirecting `<` to MySQL

PowerShell doesn't support bash-style `<` for stdin redirection. Use
`Get-Content ... | mysql ...` instead, or just double-click `setup-db.bat`.

---

## License

Free to use for educational purposes. Have fun, and may the wind
fill your sails.
