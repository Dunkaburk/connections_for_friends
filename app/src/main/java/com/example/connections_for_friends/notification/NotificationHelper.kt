package com.example.connections_for_friends.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.connections_for_friends.MainActivity
import com.example.connections_for_friends.R
import com.example.connections_for_friends.data.Friend
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "friend_reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Friend Reminders"
            val descriptionText = "Notifications to remind you to contact friends"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(friend: Friend, notificationType: String = "CONTACT") {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("FRIEND_ID", friend.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            "${friend.id}_${notificationType}".hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = when (notificationType) {
            "BIRTHDAY_REMINDER" -> createBirthdayReminderNotification(friend, pendingIntent)
            "BIRTHDAY" -> createBirthdayNotification(friend, pendingIntent)
            else -> createContactReminderNotification(friend, pendingIntent)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(
                "${friend.id}_${notificationType}".hashCode(), 
                notification
            )
        }
    }
    
    private fun createContactReminderNotification(friend: Friend, pendingIntent: PendingIntent): android.app.Notification {
        val timeSinceLastContact = if (friend.lastContactedTime != null) {
            val daysSince = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - friend.lastContactedTime)
            "It's been $daysSince days since you last contacted them."
        } else {
            "You haven't contacted them yet."
        }
        
        val lastContactedText = if (friend.lastContactedTime != null) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val date = Date(friend.lastContactedTime)
            "Last contacted on ${dateFormat.format(date)}"
        } else {
            "Never contacted"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to contact ${friend.name}")
            .setContentText(timeSinceLastContact)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$timeSinceLastContact\n$lastContactedText\nTap to mark as contacted."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
    
    private fun createBirthdayReminderNotification(friend: Friend, pendingIntent: PendingIntent): android.app.Notification {
        // Format the birthday to display month and day
        val birthdayText = if (friend.birthday.isNotBlank()) {
            try {
                val parts = friend.birthday.split("-")
                if (parts.size == 2) {
                    val monthNames = arrayOf("January", "February", "March", "April", "May", "June", 
                                           "July", "August", "September", "October", "November", "December")
                    val month = parts[0].toInt() - 1 // 0-based month
                    val day = parts[1].toInt()
                    "${monthNames[month]} $day"
                } else {
                    friend.birthday
                }
            } catch (e: Exception) {
                friend.birthday
            }
        } else {
            "Unknown date"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${friend.name}'s birthday is in one week")
            .setContentText("${friend.name}'s birthday is on $birthdayText (in 7 days)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${friend.name}'s birthday is on $birthdayText, which is coming up in 7 days. Don't forget to prepare something special!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
    
    private fun createBirthdayNotification(friend: Friend, pendingIntent: PendingIntent): android.app.Notification {
        val birthdayText = if (friend.birthday.isNotBlank()) {
            try {
                val parts = friend.birthday.split("-")
                if (parts.size == 2) {
                    val monthNames = arrayOf("January", "February", "March", "April", "May", "June", 
                                           "July", "August", "September", "October", "November", "December")
                    val month = parts[0].toInt() - 1 // 0-based month
                    val day = parts[1].toInt()
                    "${monthNames[month]} $day"
                } else {
                    friend.birthday
                }
            } catch (e: Exception) {
                friend.birthday
            }
        } else {
            "Unknown date"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎂 Happy Birthday to ${friend.name}!")
            .setContentText("Today is ${friend.name}'s birthday!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Today is ${friend.name}'s birthday! Don't forget to wish them a happy birthday."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}