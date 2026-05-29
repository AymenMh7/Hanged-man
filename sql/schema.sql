-- =====================================================================
-- Hangman Game - MySQL Schema
-- Run this script once to create the database and populate sample data.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS hangman_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hangman_db;

-- ---------------------------------------------------------------------
-- Dictionary table: words classified by difficulty.
-- Difficulty mirrors the Java enum: EASY, MEDIUM, HARD, INSANE.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS Dictionary;
CREATE TABLE Dictionary (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    word       VARCHAR(64) NOT NULL,
    difficulty ENUM('EASY','MEDIUM','HARD','INSANE') NOT NULL,
    INDEX idx_difficulty (difficulty)
);

-- ---------------------------------------------------------------------
-- Leaderboard table: best single-player scores per difficulty.
-- Score combines remaining chances + a time bonus — HIGHER is better.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS Leaderboard;
CREATE TABLE Leaderboard (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    player_name  VARCHAR(32) NOT NULL,
    difficulty   ENUM('EASY','MEDIUM','HARD','INSANE') NOT NULL,
    score        BIGINT NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_diff_score (difficulty, score DESC)
);

-- ---------------------------------------------------------------------
-- Sample words
-- ---------------------------------------------------------------------
INSERT INTO Dictionary (word, difficulty) VALUES
    ('cat',     'EASY'),
    ('dog',     'EASY'),
    ('sun',     'EASY'),
    ('book',    'EASY'),
    ('tree',    'EASY'),
    ('apple',   'EASY'),
    ('house',   'EASY'),
    ('water',   'EASY'),
    ('music',   'EASY'),
    ('happy',   'EASY');

INSERT INTO Dictionary (word, difficulty) VALUES
    ('garden',   'MEDIUM'),
    ('planet',   'MEDIUM'),
    ('puzzle',   'MEDIUM'),
    ('forest',   'MEDIUM'),
    ('window',   'MEDIUM'),
    ('rocket',   'MEDIUM'),
    ('basket',   'MEDIUM'),
    ('purple',   'MEDIUM'),
    ('castle',   'MEDIUM'),
    ('bridge',   'MEDIUM');

INSERT INTO Dictionary (word, difficulty) VALUES
    ('keyboard',     'HARD'),
    ('elephant',     'HARD'),
    ('umbrella',     'HARD'),
    ('mountain',     'HARD'),
    ('hospital',     'HARD'),
    ('strategy',     'HARD'),
    ('symphony',     'HARD'),
    ('triangle',     'HARD'),
    ('chocolate',    'HARD'),
    ('telescope',    'HARD');

INSERT INTO Dictionary (word, difficulty) VALUES
    ('xylophone',     'INSANE'),
    ('jazz',          'INSANE'),
    ('quiz',          'INSANE'),
    ('rhythm',        'INSANE'),
    ('jukebox',       'INSANE'),
    ('zephyr',        'INSANE'),
    ('quartz',        'INSANE'),
    ('whisky',        'INSANE'),
    ('mnemonic',      'INSANE'),
    ('lynx',          'INSANE');
