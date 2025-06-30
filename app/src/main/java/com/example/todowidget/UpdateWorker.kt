package com.example.todowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker class for updating the widget data
 */
class UpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "UpdateWorker: Starting work")
        
        return try {
            // Get the widget IDs to update
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val componentName = ComponentName(applicationContext, TodoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widgets to update")
                return Result.success()
            }

            // Update each widget
            for (widgetId in appWidgetIds) {
                TodoWidgetProvider.updateAppWidget(applicationContext, appWidgetManager, widgetId)
            }

            Log.d(TAG, "Successfully updated ${appWidgetIds.size} widgets")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in UpdateWorker", e)
            // Retry with exponential backoff
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UpdateWorker"
        const val WORKER_TAG = "TodoWidgetUpdateWork"
        private const val UNIQUE_WORK_NAME = "TodoWidgetUpdateWork"

        /**
         * Schedules the periodic update worker
         */
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
                    30, TimeUnit.MINUTES, // Update every 30 minutes
                    15, TimeUnit.MINUTES // Flex interval (last 15 minutes of the period)
                )
                .setConstraints(constraints)
                .addTag(WORKER_TAG)
                .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
                
                Log.d(TAG, "Scheduled periodic work")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling work", e)
                throw e
            }
        }

        /**
         * Enqueues an immediate one-time update
         */
        fun enqueueImmediateUpdate(context: Context) {
            try {
                val workRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
                    .setInitialDelay(0, TimeUnit.SECONDS)
                    .addTag(WORKER_TAG)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
                Log.d(TAG, "Enqueued immediate update")
            } catch (e: Exception) {
                Log.e(TAG, "Error enqueuing immediate update", e)
            }
        }
    }
}
