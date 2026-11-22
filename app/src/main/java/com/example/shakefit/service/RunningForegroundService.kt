package com.example.shakefit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.shakefit.MainActivity
import com.example.shakefit.R
import com.example.shakefit.ShakeFitApplication
import com.example.shakefit.model.RunSessionEvent

/**
 * RunningForegroundService: Layanan latar depan bertipe location untuk melacak lari
 * saat layar dimatikan atau aplikasi berada di background (F-07).
 */
class RunningForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "shakefit_running_channel"
        const val NOTIFICATION_ID = 202612

        const val ACTION_START = "com.example.shakefit.action.START_RUNNING"
        const val ACTION_PAUSE = "com.example.shakefit.action.PAUSE_RUNNING"
        const val ACTION_RESUME = "com.example.shakefit.action.RESUME_RUNNING"
        const val ACTION_FINISH = "com.example.shakefit.action.FINISH_RUNNING"
        const val ACTION_STOP = "com.example.shakefit.action.STOP_RUNNING"
        const val ACTION_UPDATE = "com.example.shakefit.action.UPDATE_RUNNING"

        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_IS_PAUSED = "extra_is_paused"

        fun startService(context: Context) {
            val intent = Intent(context, RunningForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RunningForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateNotification(context: Context, durationStr: String, distanceStr: String, isPaused: Boolean) {
            val intent = Intent(context, RunningForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_DURATION, durationStr)
                putExtra(EXTRA_DISTANCE, distanceStr)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                // Hindari crash jika service sedang dalam transisi berhenti
            }
        }
    }

    private var currentDuration: String = "00:00"
    private var currentDistance: String = "0.00 km"
    private var isPaused: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        val coordinator = (applicationContext as? ShakeFitApplication)?.container?.runSessionCoordinator

        when (action) {
            ACTION_START -> {
                currentDuration = intent.getStringExtra(EXTRA_DURATION) ?: "00:00"
                currentDistance = intent.getStringExtra(EXTRA_DISTANCE) ?: "0.00 km"
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
                startAsForeground()
            }
            ACTION_UPDATE -> {
                currentDuration = intent.getStringExtra(EXTRA_DURATION) ?: currentDuration
                currentDistance = intent.getStringExtra(EXTRA_DISTANCE) ?: currentDistance
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, isPaused)
                updateForegroundNotification()
            }
            ACTION_PAUSE -> {
                coordinator?.onEvent(RunSessionEvent.Pause)
            }
            ACTION_RESUME -> {
                coordinator?.onEvent(RunSessionEvent.Resume)
            }
            ACTION_FINISH -> {
                coordinator?.onEvent(RunSessionEvent.Finish)
            }
            ACTION_STOP -> {
                stopForegroundSafely()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val finishIntent = Intent(this, RunningForegroundService::class.java).apply {
            action = ACTION_FINISH
        }
        val finishPendingIntent = PendingIntent.getService(
            this,
            1,
            finishIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isPaused) "Sesi Lari Dijeda" else "Sedang Berlari")
            .setContentText("Waktu: $currentDuration • Jarak: $currentDistance")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Tombol aksi Jeda atau Lanjutkan
        if (isPaused) {
            val resumeIntent = Intent(this, RunningForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Lanjutkan", resumePendingIntent)
        } else {
            val pauseIntent = Intent(this, RunningForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                3,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Jeda", pausePendingIntent)
        }

        // Tombol aksi Selesai
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Selesai", finishPendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sesi Lari ShakeFit",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pelacakan lari di latar belakang"
                enableVibration(false)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopForegroundSafely()
        super.onDestroy()
    }
}
