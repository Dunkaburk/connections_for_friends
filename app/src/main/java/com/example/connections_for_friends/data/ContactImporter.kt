package com.example.connections_for_friends.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactData(
    val name: String,
    val birthday: String = "" // MM-DD format
)

class ContactImporter(private val context: Context) {
    
    suspend fun getContacts(): List<ContactData> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactData>()
        val contentResolver: ContentResolver = context.contentResolver
        
        try {
            // Query for contacts
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                ),
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            )
            
            cursor?.use {
                val idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                
                // Skip invalid cursor
                if (idColumnIndex == -1 || nameColumnIndex == -1) {
                    return@withContext emptyList<ContactData>()
                }
                
                while (it.moveToNext()) {
                    try {
                        val contactId = it.getString(idColumnIndex)
                        val displayName = it.getString(nameColumnIndex)
                        
                        // Skip contacts without a name
                        if (contactId == null || displayName.isNullOrBlank()) continue
                        
                        // Get birthday if available
                        val birthday = getBirthday(contentResolver, contactId)
                        
                        contacts.add(ContactData(displayName, birthday))
                    } catch (e: Exception) {
                        // Skip problematic contacts
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty list in case of error instead of crashing
            e.printStackTrace()
            return@withContext emptyList<ContactData>()
        }
        
        return@withContext contacts
    }
    
    private fun getBirthday(contentResolver: ContentResolver, contactId: String): String {
        var birthday = ""
        
        try {
            val birthdayCursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Event.DATA),
                "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                        "${ContactsContract.Data.MIMETYPE} = ? AND " +
                        "${ContactsContract.CommonDataKinds.Event.TYPE} = ?",
                arrayOf(
                    contactId,
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
                ),
                null
            )
            
            birthdayCursor?.use {
                if (it.moveToFirst()) {
                    val birthdayColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.DATA)
                    if (birthdayColumn != -1) {
                        val birthdayValue = it.getString(birthdayColumn)
                        
                        // Format the birthday to MM-DD format for our app
                        if (!birthdayValue.isNullOrBlank()) {
                            birthday = formatBirthdayToMMDD(birthdayValue) ?: ""
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If anything goes wrong, just return an empty string
            e.printStackTrace()
            birthday = ""
        }
        
        return birthday
    }
    
    private fun formatBirthdayToMMDD(rawBirthday: String?): String? {
        if (rawBirthday.isNullOrBlank()) return null
        
        // Try various date formats commonly used in contacts
        val possibleFormats = listOf(
            "yyyy-MM-dd", // ISO format
            "MM/dd/yyyy", // US format
            "dd/MM/yyyy", // European format
            "--MM-dd",    // Contact birthday without year
            "yyyyMMdd"    // Compact format
        )
        
        // First try to extract using known date formats
        for (format in possibleFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false // Strict parsing
                val date = sdf.parse(rawBirthday) ?: continue
                
                // Once parsed, extract month and day
                val calendar = Calendar.getInstance()
                calendar.time = date
                
                val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                
                // Return as MM-DD
                return String.format("%02d-%02d", month, day)
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }
        
        // If standard formats fail, try regex pattern matching as a fallback
        // This will match patterns like MM-DD, M-D, etc.
        val monthDayPattern = Regex("(\\d{1,2})[/-](\\d{1,2})")
        val matchResult = monthDayPattern.find(rawBirthday)
        
        matchResult?.let {
            try {
                val (monthStr, dayStr) = it.destructured
                val month = monthStr.toInt()
                val day = dayStr.toInt()
                
                // Basic validation
                if (month in 1..12 && day in 1..31) {
                    return String.format("%02d-%02d", month, day)
                }
            } catch (e: Exception) {
                // Fall through to return null
            }
        }
        
        return null
    }
}