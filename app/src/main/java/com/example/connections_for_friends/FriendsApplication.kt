package com.example.connections_for_friends

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.connections_for_friends.data.FriendRepository
import com.example.connections_for_friends.worker.ReminderCheckWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FriendsApplication : Application(), Configuration.Provider {

    lateinit var repository: FriendRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
        
        // Initialize the repository
        repository = FriendRepository(this)
        
        // Initialize WorkManager for periodic reminder checks
        val workManager = WorkManager.getInstance(this)
        
        // Set up daily reminder check
        val reminderCheckRequest = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            1, TimeUnit.DAYS
        ).build()
        
        // Enqueue the periodic work request
        workManager.enqueueUniquePeriodicWork(
            "FRIEND_REMINDER_CHECK",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderCheckRequest
        )
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}