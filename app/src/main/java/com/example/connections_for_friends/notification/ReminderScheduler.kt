package com.example.connections_for_friends.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.connections_for_friends.data.Friend
import timber.log.Timber

class ReminderScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleReminder(friend: Friend) {
        scheduleContactReminder(friend)
        
        if (!friend.birthday.isBlank()) {
            scheduleBirthdayReminders(friend)
        }
    }
    
    private fun scheduleContactReminder(friend: Friend) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("FRIEND_ID", friend.id)
            putExtra("NOTIFICATION_TYPE", "CONTACT")
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            friend.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        Timber.d("Scheduling contact reminder for ${friend.name} at ${friend.nextReminderTime}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // For API 31+, check if we have exact alarm permission
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                friend.nextReminderTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                friend.nextReminderTime,
                pendingIntent
            )
        }
    }
    
    private fun scheduleBirthdayReminders(friend: Friend) {
        val birthdayTime = friend.nextBirthdayTime ?: return
        val birthdayReminderTime = friend.nextBirthdayReminderTime ?: return
        
        val reminderIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("FRIEND_ID", friend.id)
            putExtra("NOTIFICATION_TYPE", "BIRTHDAY_REMINDER")
        }
        
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            "${friend.id}_birthday_reminder".hashCode(),
            reminderIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        Timber.d("Scheduling birthday reminder for ${friend.name} at $birthdayReminderTime")
        
        val birthdayIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("FRIEND_ID", friend.id)
            putExtra("NOTIFICATION_TYPE", "BIRTHDAY")
        }
        
        val birthdayPendingIntent = PendingIntent.getBroadcast(
            context,
            "${friend.id}_birthday".hashCode(),
            birthdayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        Timber.d("Scheduling birthday notification for ${friend.name} at $birthdayTime")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // For API 31+, check if we have exact alarm permission
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                birthdayReminderTime,
                reminderPendingIntent
            )
            
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                birthdayTime,
                birthdayPendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                birthdayReminderTime,
                reminderPendingIntent
            )
            
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                birthdayTime,
                birthdayPendingIntent
            )
        }
    }
    
    fun cancelReminder(friendId: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("FRIEND_ID", friendId)
            putExtra("NOTIFICATION_TYPE", "CONTACT")
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            friendId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        
        val birthdayReminderIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("FRIEND_ID", friendId)
            putExtra("NOTIFICATION_TYPE", "BIRTHDAY_REMINDER")
        }
        
        val birthdayReminderPendingIntent = PendingIntent.getBroadcast(
            context,
            "${friendId}_birthday_reminder".hashCode(),
            birthdayReminderIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        
        birthdayReminderPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        
        val birthdayIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("FRIEND_ID", friendId)
            putExtra("NOTIFICATION_TYPE", "BIRTHDAY")
        }
        
        val birthdayPendingIntent = PendingIntent.getBroadcast(
            context,
            "${friendId}_birthday".hashCode(),
            birthdayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        
        birthdayPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
    
    fun updateReminder(friend: Friend) {
        cancelReminder(friend.id)
        scheduleReminder(friend)
    }
}