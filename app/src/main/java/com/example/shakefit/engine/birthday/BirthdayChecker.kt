package com.example.shakefit.engine.birthday

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilitas dan engine murni untuk perayaan notifikasi ulang tahun (F-13).
 * Kelas Kotlin murni tanpa dependensi Android framework (bisa diuji via JUnit).
 */
object BirthdayChecker {

    /**
     * Memeriksa apakah hari ini adalah hari ulang tahun pengguna dan belum ternotifikasi di tahun ini.
     *
     * Aturan F-13:
     * - Hanya sekali per tahun (cek lastBirthdayNotifiedYear != currentYear).
     * - Tanggal 29 Februari: dirayakan 28 Februari pada tahun bukan kabisat.
     */
    fun shouldNotifyBirthdayToday(
        birthDateIso: String?,
        lastNotifiedYear: Int?,
        todayCalendar: Calendar = Calendar.getInstance()
    ): Boolean {
        if (birthDateIso.isNullOrBlank()) return false

        val parts = birthDateIso.split("-")
        if (parts.size != 3) return false

        val birthMonth = parts[1].toIntOrNull() ?: return false
        val birthDay = parts[2].toIntOrNull() ?: return false

        val currentYear = todayCalendar.get(Calendar.YEAR)
        val currentMonth = todayCalendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val currentDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

        // Aturan: Hanya sekali per tahun
        if (lastNotifiedYear != null && lastNotifiedYear >= currentYear) {
            return false
        }

        // Tangani kasus khusus 29 Februari
        val isLeap = isLeapYear(currentYear)
        if (birthMonth == 2 && birthDay == 29) {
            return if (isLeap) {
                // Di tahun kabisat, tepat pada 29 Februari
                currentMonth == 2 && currentDay == 29
            } else {
                // Di tahun bukan kabisat, dirayakan pada 28 Februari
                currentMonth == 2 && currentDay == 28
            }
        }

        // Tanggal ulang tahun normal
        return currentMonth == birthMonth && currentDay == birthDay
    }

    /**
     * Cek apakah suatu tahun adalah tahun kabisat.
     */
    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Menghitung usia pengguna saat ini berdasarkan tanggal lahir (yyyy-MM-dd).
     */
    fun calculateAgeYears(birthDateIso: String?, todayCalendar: Calendar = Calendar.getInstance()): Int? {
        if (birthDateIso.isNullOrBlank()) return null
        val parts = birthDateIso.split("-")
        if (parts.size != 3) return null

        val birthYear = parts[0].toIntOrNull() ?: return null
        val birthMonth = parts[1].toIntOrNull() ?: return null
        val birthDay = parts[2].toIntOrNull() ?: return null

        val currentYear = todayCalendar.get(Calendar.YEAR)
        val currentMonth = todayCalendar.get(Calendar.MONTH) + 1
        val currentDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

        var age = currentYear - birthYear
        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }

        return if (age >= 0) age else null
    }

    /**
     * Validasi format tanggal yyyy-MM-dd dan memastikan tidak di masa depan.
     */
    fun isBirthDateValid(birthDateIso: String, todayCalendar: Calendar = Calendar.getInstance()): Boolean {
        val parts = birthDateIso.trim().split("-")
        if (parts.size != 3) return false

        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false

        if (month !in 1..12) return false
        if (day !in 1..31) return false

        // Cek tanggal di masa depan
        val curYear = todayCalendar.get(Calendar.YEAR)
        val curMonth = todayCalendar.get(Calendar.MONTH) + 1
        val curDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

        if (year > curYear) return false
        if (year == curYear && month > curMonth) return false
        if (year == curYear && month == curMonth && day > curDay) return false

        return year in 1900..curYear
    }

    /**
     * Membuat pesan ucapan ulang tahun teks panjang lokal (tanpa internet).
     * Gaya pesan ramah dan memotivasi, menggunakan nickname bila ada.
     */
    fun generateBirthdayMessage(nickname: String?): String {
        val name = if (!nickname.isNullOrBlank()) nickname.trim() else "Sobat ShakeFit"
        val templates = listOf(
            "Selamat ulang tahun, %s! 🎉 Semoga sehat selalu, panjang umur, dan semakin konsisten menjaga kebugaran tubuhmu. ShakeFit bangga menjadi bagian dari perjalanan hidup sehatmu hari ini!",
            "Happy Birthday, %s! 🎂 Hari ini adalah momen istimewamu untuk merayakan pencapaian hidup. Tetap semangat, jaga pola hidup aktif, dan raih target kebugaran impianmu bersama ShakeFit!",
            "Selamat bertambah usia, %s! ✨ Jadikan tahun ini lebih bugar, kuat, dan penuh energi positif. Jangan lupa luangkan waktu untuk bergerak aktif dan tetap bahagia hari ini!"
        )

        val index = (Math.abs(name.hashCode())) % templates.size
        return String.format(Locale.US, templates[index], name)
    }
}
