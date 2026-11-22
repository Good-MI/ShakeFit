package com.example.shakefit

import com.example.shakefit.engine.birthday.BirthdayChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ProfileAndBirthdayTest {

    @Test
    fun testBirthDateValidation() {
        val today = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21)
        }

        // Tanggal valid
        assertTrue(BirthdayChecker.isBirthDateValid("2000-05-15", today))
        assertTrue(BirthdayChecker.isBirthDateValid("1995-12-31", today))
        assertTrue(BirthdayChecker.isBirthDateValid("2026-11-21", today)) // Hari ini

        // Tanggal di masa depan
        assertFalse(BirthdayChecker.isBirthDateValid("2026-11-22", today)) // Besok
        assertFalse(BirthdayChecker.isBirthDateValid("2027-01-01", today))
        assertFalse(BirthdayChecker.isBirthDateValid("2099-12-31", today))

        // Format tidak valid
        assertFalse(BirthdayChecker.isBirthDateValid("invalid-date", today))
        assertFalse(BirthdayChecker.isBirthDateValid("2000-13-10", today)) // Bulan 13
        assertFalse(BirthdayChecker.isBirthDateValid("2000-05-35", today)) // Hari 35
        assertFalse(BirthdayChecker.isBirthDateValid("1850-01-01", today)) // Di bawah 1900
    }

    @Test
    fun testAgeCalculation() {
        val today = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21) // 21 Nov 2026
        }

        // Sudah berulang tahun tahun ini (15 Mei 2000 -> 26 tahun)
        assertEquals(26, BirthdayChecker.calculateAgeYears("2000-05-15", today))

        // Tepat hari ini ulang tahun (21 Nov 2000 -> 26 tahun)
        assertEquals(26, BirthdayChecker.calculateAgeYears("2000-11-21", today))

        // Belum berulang tahun tahun ini (25 Des 2000 -> 25 tahun)
        assertEquals(25, BirthdayChecker.calculateAgeYears("2000-12-25", today))

        // Besok ulang tahun (22 Nov 2000 -> 25 tahun)
        assertEquals(25, BirthdayChecker.calculateAgeYears("2000-11-22", today))

        // Null atau format salah
        assertNull(BirthdayChecker.calculateAgeYears(null, today))
        assertNull(BirthdayChecker.calculateAgeYears("", today))
    }

    @Test
    fun testStandardBirthdayNotification() {
        val today = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 15) // 15 Mei 2026
        }

        // Ulang tahun hari ini dan belum pernah ternotifikasi tahun ini
        assertTrue(BirthdayChecker.shouldNotifyBirthdayToday("1998-05-15", null, today))
        assertTrue(BirthdayChecker.shouldNotifyBirthdayToday("1998-05-15", 2025, today))

        // Sudah pernah ternotifikasi di tahun 2026 -> tidak boleh kirim lagi (aturan sekali per tahun)
        assertFalse(BirthdayChecker.shouldNotifyBirthdayToday("1998-05-15", 2026, today))

        // Bukan hari ulang tahunnya (hari ini 15 Mei, lahir 16 Mei)
        assertFalse(BirthdayChecker.shouldNotifyBirthdayToday("1998-05-16", null, today))
    }

    @Test
    fun testLeapYearFeb29Rules() {
        // Cek fungsi kabisat
        assertTrue(BirthdayChecker.isLeapYear(2020))
        assertTrue(BirthdayChecker.isLeapYear(2024))
        assertTrue(BirthdayChecker.isLeapYear(2028))
        assertFalse(BirthdayChecker.isLeapYear(2025))
        assertFalse(BirthdayChecker.isLeapYear(2026))
        assertFalse(BirthdayChecker.isLeapYear(2027))

        // 1. Tahun BUKAN kabisat (2026): lahir 29 Feb -> dirayakan pada 28 Feb
        val nonLeapFeb28 = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 28)
        }
        val nonLeapMarch1 = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 1)
        }

        // Pada 28 Feb di tahun 2026: harus ternotifikasi!
        assertTrue(BirthdayChecker.shouldNotifyBirthdayToday("2000-02-29", null, nonLeapFeb28))
        // Pada 1 Maret di tahun 2026: tidak
        assertFalse(BirthdayChecker.shouldNotifyBirthdayToday("2000-02-29", null, nonLeapMarch1))

        // 2. Tahun KABISAT (2028): lahir 29 Feb -> tepat dirayakan pada 29 Feb
        val leapFeb28 = Calendar.getInstance().apply {
            set(2028, Calendar.FEBRUARY, 28)
        }
        val leapFeb29 = Calendar.getInstance().apply {
            set(2028, Calendar.FEBRUARY, 29)
        }

        // Pada 28 Feb tahun kabisat -> belum waktunya
        assertFalse(BirthdayChecker.shouldNotifyBirthdayToday("2000-02-29", null, leapFeb28))
        // Pada 29 Feb tahun kabisat -> waktu yang tepat!
        assertTrue(BirthdayChecker.shouldNotifyBirthdayToday("2000-02-29", null, leapFeb29))
    }

    @Test
    fun testBirthdayMessageGeneration() {
        val msgWithNickname = BirthdayChecker.generateBirthdayMessage("Budi")
        assertTrue("Pesan harus menyapa Budi", msgWithNickname.contains("Budi"))
        assertTrue("Pesan harus teks panjang (> 40 karakter)", msgWithNickname.length > 40)

        val msgWithoutNickname = BirthdayChecker.generateBirthdayMessage(null)
        assertTrue("Pesan default harus menyapa Sobat ShakeFit", msgWithoutNickname.contains("Sobat ShakeFit"))

        val msgBlankNickname = BirthdayChecker.generateBirthdayMessage("   ")
        assertTrue("Pesan default harus menyapa Sobat ShakeFit", msgBlankNickname.contains("Sobat ShakeFit"))
    }
}
