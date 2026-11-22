package com.example.shakefit

import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.GpsStatus
import com.example.shakefit.model.RunEnvironment
import com.example.shakefit.model.RunSessionUiState
import com.example.shakefit.model.SessionPhase
import com.example.shakefit.service.RunningForegroundService
import com.example.shakefit.util.formatDistanceKm
import com.example.shakefit.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RunSessionCoordinatorTest {

    @Test
    fun testForegroundServiceConstants() {
        assertEquals("shakefit_running_channel", RunningForegroundService.CHANNEL_ID)
        assertEquals(202612, RunningForegroundService.NOTIFICATION_ID)
        assertEquals("com.example.shakefit.action.START_RUNNING", RunningForegroundService.ACTION_START)
        assertEquals("com.example.shakefit.action.PAUSE_RUNNING", RunningForegroundService.ACTION_PAUSE)
        assertEquals("com.example.shakefit.action.RESUME_RUNNING", RunningForegroundService.ACTION_RESUME)
        assertEquals("com.example.shakefit.action.FINISH_RUNNING", RunningForegroundService.ACTION_FINISH)
        assertEquals("com.example.shakefit.action.STOP_RUNNING", RunningForegroundService.ACTION_STOP)
        assertEquals("com.example.shakefit.action.UPDATE_RUNNING", RunningForegroundService.ACTION_UPDATE)
    }

    @Test
    fun testNotificationFormatters() {
        // Durasi 75 detik -> "01:15"
        val durationStr = formatDuration(75)
        assertEquals("01:15", durationStr)

        // Durasi 3661 detik -> "1:01:01" (H:MM:SS sesuai kontrak)
        val longDurationStr = formatDuration(3661)
        assertEquals("1:01:01", longDurationStr)

        // Jarak 5230 meter -> "5.23 km"
        val distanceStr = formatDistanceKm(5230f)
        assertEquals("5.23 km", distanceStr)

        // Jarak 0 meter -> "0.00 km"
        val zeroDistanceStr = formatDistanceKm(0f)
        assertEquals("0.00 km", zeroDistanceStr)
    }

    @Test
    fun testInitialRunSessionUiState() {
        val state = RunSessionUiState(
            phase = SessionPhase.IDLE,
            environment = RunEnvironment.OUTDOOR,
            countdownRemaining = 5,
            elapsedSec = 0,
            distanceMeters = 0f,
            distanceSource = DistanceSource.GPS,
            currentPaceSecPerKm = null,
            avgPaceSecPerKm = null,
            cadenceSpm = null,
            caloriesEst = 0f,
            splits = emptyList(),
            gpsStatus = GpsStatus.OFF,
            needsLocationPermission = false,
            needsNotificationPermission = false,
            isBackgroundTracking = false,
            summary = null,
            error = null
        )

        assertEquals(SessionPhase.IDLE, state.phase)
        assertEquals(RunEnvironment.OUTDOOR, state.environment)
        assertEquals(0, state.elapsedSec)
        assertEquals(0f, state.distanceMeters, 0.001f)
        assertFalse(state.isBackgroundTracking)
        assertNull(state.summary)
        assertNull(state.error)
    }
}
