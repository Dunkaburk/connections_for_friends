package com.example.connections_for_friends.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Serializable
data class Friend(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val birthday: String = "", // Format: MM-DD (e.g., "12-25" for December 25)
    val notes: String = "",
    val reminderFrequencyDays: Int = 30,
    val lastContactedTime: Long? = null,
    val nextReminderTime: Long = calculateNextReminderTime(lastContactedTime, reminderFrequencyDays),
    val nextBirthdayTime: Long? = calculateNextBirthdayTime(birthday),
    val nextBirthdayReminderTime: Long? = calculateNextBirthdayReminderTime(birthday)
) {
    companion object {
        fun calculateNextReminderTime(lastContactedTime: Long?, reminderFrequencyDays: Int): Long {
            if (lastContactedTime == null) {
                return System.currentTimeMillis()
            }
            
            return lastContactedTime + (reminderFrequencyDays * 24 * 60 * 60 * 1000L)
        }
        fun calculateNextBirthdayTime(birthdayStr: String): Long? {
            if (birthdayStr.isBlank()) return null
            
            try {
                val parts = birthdayStr.split("-")
                if (parts.size != 2) return null
                
                val month = parts[0].toInt()
                val day = parts[1].toInt()
                
                if (month < 1 || month > 12 || day < 1 || day > 31) return null
                
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                
                val birthdayCalendar = Calendar.getInstance()
                birthdayCalendar.set(Calendar.MONTH, month - 1) // Calendar months are 0-based
                birthdayCalendar.set(Calendar.DAY_OF_MONTH, day)
                birthdayCalendar.set(Calendar.HOUR_OF_DAY, 9) // 9 AM notification
                birthdayCalendar.set(Calendar.MINUTE, 0)
                birthdayCalendar.set(Calendar.SECOND, 0)
                birthdayCalendar.set(Calendar.MILLISECOND, 0)
                
                // If birthday has already passed this year, set for next year
                if (month < currentMonth || (month == currentMonth && day < currentDay)) {
                    birthdayCalendar.set(Calendar.YEAR, currentYear + 1)
                } else {
                    birthdayCalendar.set(Calendar.YEAR, currentYear)
                }
                
                return birthdayCalendar.timeInMillis
            } catch (e: Exception) {
                return null
            }
        }
        
        fun calculateNextBirthdayReminderTime(birthdayStr: String): Long? {
            val birthdayTime = calculateNextBirthdayTime(birthdayStr) ?: return null
            
            // One week before birthday (7 * 24 * 60 * 60 * 1000 milliseconds)
            return birthdayTime - (7L * 24L * 60L * 60L * 1000L)
        }
    }
}