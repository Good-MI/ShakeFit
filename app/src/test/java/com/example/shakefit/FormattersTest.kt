package com.example.shakefit

import com.example.shakefit.util.formatCalories
import com.example.shakefit.util.formatDistanceKm
import com.example.shakefit.util.formatDuration
import com.example.shakefit.util.formatPace
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun testFormatPace() {
        assertEquals("5:45 /km", formatPace(345))
        assertEquals("--:--", formatPace(null))
        assertEquals("--:--", formatPace(0))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("1:02:05", formatDuration(3725))
        assertEquals("01:35", formatDuration(95))
        assertEquals("00:00", formatDuration(0))
    }

    @Test
    fun testFormatDistanceKm() {
        assertEquals("5.23 km", formatDistanceKm(5230f))
        assertEquals("0.00 km", formatDistanceKm(0f))
    }

    @Test
    fun testFormatCalories() {
        assertEquals("~120 kkal", formatCalories(120f))
    }
}
