package com.example.connections_for_friends.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.connections_for_friends.data.Friend
import com.example.connections_for_friends.data.FriendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import timber.log.Timber

class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val friendId = intent.getStringExtra("FRIEND_ID") ?: return
        val notificationType = intent.getStringExtra("NOTIFICATION_TYPE") ?: "CONTACT"
        
        Timber.d("Received $notificationType notification for friend ID: $friendId")
        
        val notificationHelper = NotificationHelper(context)
        val repository = FriendRepository(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val friends = repository.friends.first()
                val friend = friends.find { it.id == friendId }
                
                friend?.let { 
                    when (notificationType) {
                        "CONTACT" -> {
                            notificationHelper.showReminderNotification(it, "CONTACT")
                        }
                        "BIRTHDAY_REMINDER" -> {
                            notificationHelper.showReminderNotification(it, "BIRTHDAY_REMINDER")
                            
                            val reminderScheduler = ReminderScheduler(context)
                            val updatedBirthdayTime = Friend.calculateNextBirthdayTime(it.birthday)
                            
                            if (updatedBirthdayTime != null && updatedBirthdayTime != it.nextBirthdayTime) {
                                val updatedFriend = it.copy(
                                    nextBirthdayTime = updatedBirthdayTime,
                                    nextBirthdayReminderTime = Friend.calculateNextBirthdayReminderTime(it.birthday)
                                )
                                repository.updateFriend(updatedFriend)
                                reminderScheduler.updateReminder(updatedFriend)
                            }
                        }
                        "BIRTHDAY" -> {
                            notificationHelper.showReminderNotification(it, "BIRTHDAY")
                            
                            val reminderScheduler = ReminderScheduler(context)
                            val nextYearBirthdayTime = Friend.calculateNextBirthdayTime(it.birthday)
                            
                            if (nextYearBirthdayTime != null && nextYearBirthdayTime != it.nextBirthdayTime) {
                                val updatedFriend = it.copy(
                                    nextBirthdayTime = nextYearBirthdayTime,
                                    nextBirthdayReminderTime = Friend.calculateNextBirthdayReminderTime(it.birthday)
                                )
                                repository.updateFriend(updatedFriend)
                                reminderScheduler.updateReminder(updatedFriend)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error showing notification: ${e.message}")
            }
        }
    }
}