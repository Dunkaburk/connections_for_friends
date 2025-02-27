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
            
            // Get current time
            val currentTime = System.currentTimeMillis()
            
            // Get all friends
            val friends = repository.friends.first()
            
            // Find friends due for reminder
            val dueFriends = friends.filter { it.nextReminderTime <= currentTime }
            
            Timber.d("Found ${dueFriends.size} friends due for reminder")
            
            // Send notifications for each friend
            dueFriends.forEach { friend ->
                Timber.d("Sending reminder for ${friend.name}")
                notificationHelper.showReminderNotification(friend)
                
                // Schedule next reminder
                val nextReminderTime = currentTime + (friend.reminderFrequencyDays * 24 * 60 * 60 * 1000L)
                val updatedFriend = friend.copy(nextReminderTime = nextReminderTime)
                repository.updateFriend(updatedFriend)
                
                // Update the alarm for the next reminder
                reminderScheduler.updateReminder(updatedFriend)
            }
            
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error checking reminders")
            return Result.failure()
        }
    }
}