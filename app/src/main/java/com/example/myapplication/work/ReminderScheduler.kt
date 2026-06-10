package com.example.myapplication.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily_reminder_worker"

    fun schedule(context: Context) {
        try {
            val constraints = Constraints.Builder()
                // 移除电池限制，确保提醒能正常工作
                .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                .build()

            // 每15分钟检查一次，确保及时提醒
            // 注意：PeriodicWorkRequest 的最小间隔是15分钟
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

