package com.example.shakefit.data.repository

import android.content.ContentValues
import com.example.shakefit.data.DbHelper
import com.example.shakefit.model.Gender
import com.example.shakefit.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ProfileRepository mengelola tabel user_profile (id = 1) di SQLite.
 * Berjalan di Dispatchers.IO.
 */
class ProfileRepository(private val dbHelper: DbHelper) {

    /**
     * Membaca profil pengguna dari database.
     */
    suspend fun getProfile(): UserProfile? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT nickname, gender, height_cm, weight_kg, birth_date, last_birthday_notified_year
            FROM user_profile
            WHERE id = 1
            """.trimIndent(),
            null
        )

        cursor.use { c ->
            if (c.moveToNext()) {
                val nickname = if (c.isNull(0)) null else c.getString(0)
                val gender = if (c.isNull(1)) null else Gender.valueOf(c.getString(1))
                val heightCm = if (c.isNull(2)) null else c.getFloat(2)
                val weightKg = if (c.isNull(3)) null else c.getFloat(3)
                val birthDate = if (c.isNull(4)) null else c.getString(4)
                val lastYear = if (c.isNull(5)) null else c.getInt(5)

                UserProfile(
                    id = 1,
                    nickname = nickname,
                    gender = gender,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    birthDateIso = birthDate,
                    lastBirthdayNotifiedYear = lastYear
                )
            } else {
                null
            }
        }
    }

    /**
     * Menyimpan atau memperbarui data profil pengguna (id = 1).
     */
    suspend fun saveProfile(
        nickname: String,
        gender: Gender,
        heightCm: Float,
        weightKg: Float,
        birthDateIso: String
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", 1)
            put("nickname", nickname.trim())
            put("gender", gender.name)
            put("height_cm", heightCm)
            put("weight_kg", weightKg)
            put("birth_date", birthDateIso.trim())
        }

        db.insertWithOnConflict(
            "user_profile",
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Memperbarui tahun terakhir notifikasi ulang tahun terkirim.
     */
    suspend fun updateLastBirthdayNotifiedYear(year: Int) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("last_birthday_notified_year", year)
        }
        db.update("user_profile", cv, "id = 1", null)
    }
}
