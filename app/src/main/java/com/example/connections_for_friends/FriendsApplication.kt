package com.example.connections_for_friends

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.connections_for_friends.data.FriendRepository
import com.example.connections_for_friends.worker.ReminderCheckWorker
import com.example.connections_for_friends.worker.WidgetUpdateWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FriendsApplication : Application(), Configuration.Provider {

    lateinit var repository: FriendRepository
    
    override fun onCreate() {
        super.onCreate()
        
        Timber.plant(Timber.DebugTree())
        repository = FriendRepository(this)
        val workManager = WorkManager.getInstance(this)
        val reminderCheckRequest = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            1, TimeUnit.DAYS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "FRIEND_REMINDER_CHECK",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderCheckRequest
        )
        
        WidgetUpdateWorker.schedule(this)
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}