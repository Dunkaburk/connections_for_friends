package com.example.connections_for_friends.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.connections_for_friends.data.FriendRepository
import com.example.connections_for_friends.notification.NotificationHelper
import com.example.connections_for_friends.notification.ReminderScheduler
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ReminderCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = FriendRepository(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)
        val reminderScheduler = ReminderScheduler(applicationContext)
        
        try {
            Timber.d("Running reminder check")
            
            val currentTime = System.currentTimeMillis()
            
            val friends = repository.friends.first()
            
            val dueFriends = friends.filter { it.nextReminderTime <= currentTime }
            
            Timber.d("Found ${dueFriends.size} friends due for reminder")
            
            dueFriends.forEach { friend ->
                Timber.d("Sending reminder for ${friend.name}")
                notificationHelper.showReminderNotification(friend)
                
                val nextReminderTime = currentTime + (friend.reminderFrequencyDays * 24 * 60 * 60 * 1000L)
                val updatedFriend = friend.copy(nextReminderTime = nextReminderTime)
                repository.updateFriend(updatedFriend)
                
                reminderScheduler.updateReminder(updatedFriend)
            }
            
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error checking reminders")
            return Result.failure()
        }
    }
}