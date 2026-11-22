package com.example.shakefit

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.shakefit.di.AppContainer
import com.example.shakefit.worker.BirthdayNotificationWorker
import java.util.concurrent.TimeUnit

/**
 * Application class untuk DI manual dan inisialisasi background tasks.
 * AppContainer dibuat di sini dan diakses oleh ViewModelFactory.
 */
class ShakeFitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        setupBirthdayNotificationSchedule()
    }

    /**
     * Menjadwalkan pengecekan harian ulang tahun via WorkManager (F-13)
     * serta menjalankan satu kali pengecekan langsung saat aplikasi dibuka sebagai cadangan.
     */
    private fun setupBirthdayNotificationSchedule() {
        try {
            val workManager = WorkManager.getInstance(this)

            // 1. Pengecekan harian berkala (1 hari sekali)
            val periodicRequest = PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(
                1, TimeUnit.DAYS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                BirthdayNotificationWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            // 2. Pengecekan cadangan saat app dibuka
            val backupRequest = OneTimeWorkRequestBuilder<BirthdayNotificationWorker>().build()
            workManager.enqueue(backupRequest)
        } catch (_: Exception) {
            // WorkManager initialization safety
        }
    }
}
