package dev.amandevelops.solax.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Captures a sample every ~15 minutes so the weekly chart survives days when
 * the app is not open. WorkManager's minimum periodic interval is 15 minutes.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).current()
        if (settings.demoMode || !settings.isConfigured || !settings.backgroundSync) {
            return Result.success()
        }

        return try {
            val reading = SolaxClient().fetch(settings.host, settings.dongleSerial)
            HistoryRepository(applicationContext).append(reading)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "solax-background-sync"

        fun sync(context: Context, settings: AppSettings) {
            val manager = WorkManager.getInstance(context)
            val shouldRun = settings.backgroundSync && !settings.demoMode && settings.isConfigured

            if (!shouldRun) {
                manager.cancelUniqueWork(WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
