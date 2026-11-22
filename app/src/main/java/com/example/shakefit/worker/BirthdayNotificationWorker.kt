package com.example.shakefit.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shakefit.R
import com.example.shakefit.ShakeFitApplication
import com.example.shakefit.engine.birthday.BirthdayChecker
import java.util.Calendar

/**
 * Worker terjadwal harian untuk memeriksa dan mengirimkan notifikasi ulang tahun (F-13).
 */
class BirthdayNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "shakefit_birthday_check"
        const val CHANNEL_ID = "shakefit_birthday_channel"
        const val NOTIFICATION_ID = 202611
    }

    override suspend fun doWork(): Result {
        try {
            val app = context.applicationContext as? ShakeFitApplication ?: return Result.success()
            val profileRepo = app.container.profileRepository

            val profile = profileRepo.getProfile() ?: return Result.success()
            val today = Calendar.getInstance()

            val shouldNotify = BirthdayChecker.shouldNotifyBirthdayToday(
                birthDateIso = profile.birthDateIso,
                lastNotifiedYear = profile.lastBirthdayNotifiedYear,
                todayCalendar = today
            )

            if (shouldNotify) {
                sendBirthdayNotification(profile.nickname)
                profileRepo.updateLastBirthdayNotifiedYear(today.get(Calendar.YEAR))
            }

            return Result.success()
        } catch (e: Exception) {
            // Pekerjaan background tidak boleh menyebabkan crash
            return Result.success()
        }
    }

    private fun sendBirthdayNotification(nickname: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat channel untuk Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ucapan Ulang Tahun",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi ucapan selamat ulang tahun dari ShakeFit"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Cek izin POST_NOTIFICATIONS untuk Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val message = BirthdayChecker.generateBirthdayMessage(nickname)
        val title = if (!nickname.isNullOrBlank()) "Happy Birthday, ${nickname.trim()}! 🎂" else "Selamat Ulang Tahun! 🎂"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Buka ShakeFit untuk pesan spesial hari ini!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
