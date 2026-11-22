package com.example.shakefit.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLiteOpenHelper untuk ShakeFit.
 * Skema mengikuti prd.md bagian 6.
 * Foreign key diaktifkan (restriction.md bagian 6).
 * Seed awal: 3 latihan (exercises) + 7 badge (badges).
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "shakefit.db"
        const val DATABASE_VERSION = 1
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true) // PRAGMA foreign_keys = ON
    }

    override fun onCreate(db: SQLiteDatabase) {
        // --- user_profile ---
        db.execSQL(
            """
            CREATE TABLE user_profile (
                id INTEGER PRIMARY KEY CHECK(id = 1),
                nickname TEXT,
                gender TEXT CHECK(gender IN ('MALE', 'FEMALE')),
                height_cm REAL,
                weight_kg REAL,
                birth_date TEXT,
                last_birthday_notified_year INTEGER
            )
            """.trimIndent()
        )

        // --- exercises ---
        db.execSQL(
            """
            CREATE TABLE exercises (
                id INTEGER PRIMARY KEY,
                code TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                kind TEXT NOT NULL CHECK(kind IN ('REPS', 'RUN')),
                calories_per_rep REAL NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // --- workout_sessions ---
        db.execSQL(
            """
            CREATE TABLE workout_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                mode TEXT NOT NULL CHECK(mode IN ('FREE', 'CHALLENGE_60S', 'GOAL')),
                started_at INTEGER NOT NULL,
                duration_sec INTEGER NOT NULL,
                reps INTEGER,
                avg_tempo_ms INTEGER,
                quality_score INTEGER,
                calories_est REAL NOT NULL DEFAULT 0,
                note TEXT DEFAULT '',
                FOREIGN KEY (exercise_id) REFERENCES exercises(id)
            )
            """.trimIndent()
        )

        // --- run_details ---
        db.execSQL(
            """
            CREATE TABLE run_details (
                session_id INTEGER PRIMARY KEY,
                environment TEXT NOT NULL CHECK(environment IN ('OUTDOOR', 'TREADMILL')),
                distance_m REAL NOT NULL DEFAULT 0,
                avg_pace_sec_per_km INTEGER,
                best_pace_sec_per_km INTEGER,
                avg_cadence_spm INTEGER,
                distance_source TEXT NOT NULL CHECK(distance_source IN ('GPS', 'STEPS_ESTIMATE')),
                FOREIGN KEY (session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // --- run_splits ---
        db.execSQL(
            """
            CREATE TABLE run_splits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                km_index INTEGER NOT NULL,
                duration_sec INTEGER NOT NULL,
                pace_sec_per_km INTEGER NOT NULL,
                FOREIGN KEY (session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // --- daily_goals ---
        db.execSQL(
            """
            CREATE TABLE daily_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                target_value REAL NOT NULL,
                unit TEXT NOT NULL CHECK(unit IN ('REPS', 'KM')),
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (exercise_id) REFERENCES exercises(id)
            )
            """.trimIndent()
        )

        // --- badges ---
        db.execSQL(
            """
            CREATE TABLE badges (
                id INTEGER PRIMARY KEY,
                code TEXT UNIQUE NOT NULL,
                unlocked_at INTEGER
            )
            """.trimIndent()
        )

        // --- settings ---
        db.execSQL(
            """
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT
            )
            """.trimIndent()
        )

        // === SEED DATA ===
        seedExercises(db)
        seedBadges(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Fase 1: versi 1 saja, belum ada migrasi.
        // Migrasi akan ditambahkan bertahap di fase berikutnya.
    }

    // ----- Seed helpers -----

    private fun seedExercises(db: SQLiteDatabase) {
        val exercises = listOf(
            Triple("SQUAT", "Squat", "REPS"),
            Triple("PUSHUP", "Push-up", "REPS"),
            Triple("RUNNING", "Lari", "RUN")
        )
        // Kalori per rep / per km diambil dari Tuning.kt
        // SQUAT id=1, PUSHUP id=2, RUNNING id=3
        val caloriesPerUnit = listOf(0.32f, 0.29f, 1.036f)

        exercises.forEachIndexed { index, (code, name, kind) ->
            val cv = ContentValues().apply {
                put("id", index + 1)
                put("code", code)
                put("name", name)
                put("kind", kind)
                put("calories_per_rep", caloriesPerUnit[index])
                put("is_active", 1)
            }
            db.insert("exercises", null, cv)
        }
    }

    private fun seedBadges(db: SQLiteDatabase) {
        val badges = listOf(
            1 to "FIRST_SESSION",
            2 to "FIRST_10_REPS",
            3 to "STREAK_3",
            4 to "STREAK_7",
            5 to "REPS_100_DAY",
            6 to "RUN_1KM",
            7 to "RUN_5KM"
        )
        badges.forEach { (id, code) ->
            val cv = ContentValues().apply {
                put("id", id)
                put("code", code)
                // unlocked_at = null (belum terbuka)
            }
            db.insert("badges", null, cv)
        }
    }
}
