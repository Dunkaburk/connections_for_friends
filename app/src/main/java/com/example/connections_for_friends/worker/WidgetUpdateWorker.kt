package com.example.connections_for_friends.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.connections_for_friends.widget.FriendsToContactWidget
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker that updates the friends widget periodically
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Updating Friends widget")
            
            val glanceAppWidgetManager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = glanceAppWidgetManager.getGlanceIds(FriendsToContactWidget::class.java)
            
            FriendsToContactWidget().updateAll(applicationContext)
            
            Timber.d("Widget update successful")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error updating widget")
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_update_worker"
        

         // widget scheduler

        fun schedule(context: Context) {
            Timber.d("Scheduling widget updates")
            
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.SECONDS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}