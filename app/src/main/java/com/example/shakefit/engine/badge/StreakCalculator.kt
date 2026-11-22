package com.example.shakefit.engine.badge

import java.util.Calendar

/**
 * StreakCalculator: Menghitung streak harian berturut-turut (F-12c).
 * Logika murni Kotlin independen sehingga dapat diuji secara komprehensif lewat unit test.
 */
object StreakCalculator {

    /**
     * Menghitung jumlah hari beruntun dengan minimal 1 sesi tersimpan.
     * Jika hari ini belum berolahraga, streak tetap aktif jika kemarin berolahraga.
     */
    fun calculateStreak(
        referenceCalendar: Calendar = Calendar.getInstance(),
        hasSessionOnDate: (startMs: Long, endMs: Long) -> Boolean
    ): Int {
        val cal = referenceCalendar.clone() as Calendar
        var streak = 0

        while (true) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endMs = cal.timeInMillis

            val hasSession = hasSessionOnDate(startMs, endMs)

            if (hasSession) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (streak == 0) {
                    // Cek kemarin jika hari ini belum ada sesi yang tersimpan
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val yestStart = cal.timeInMillis

                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val yestEnd = cal.timeInMillis

                    if (hasSessionOnDate(yestStart, yestEnd)) {
                        streak++
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }
        return streak
    }
}
