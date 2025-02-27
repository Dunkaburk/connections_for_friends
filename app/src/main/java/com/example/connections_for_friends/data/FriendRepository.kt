package com.example.connections_for_friends.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "friends_prefs")

class FriendRepository(private val context: Context) {
    private val friendsKey = stringPreferencesKey("friends_list")
    
    val friends: Flow<List<Friend>> = context.dataStore.data.map { preferences ->
        val friendsJson = preferences[friendsKey] ?: "[]"
        try {
            Json.decodeFromString<List<Friend>>(friendsJson)
        } catch (e: Exception) {
            Timber.e(e, "Error decoding friends data")
            emptyList()
        }
    }
    
    suspend fun addFriend(friend: Friend) {
        context.dataStore.edit { preferences ->
            val friendsJson = preferences[friendsKey] ?: "[]"
            val currentFriends = try {
                Json.decodeFromString<List<Friend>>(friendsJson)
            } catch (e: Exception) {
                Timber.e(e, "Error decoding friends data")
                emptyList()
            }
            
            val updatedFriends = currentFriends + friend
            preferences[friendsKey] = Json.encodeToString(updatedFriends)
        }
    }
    
    suspend fun updateFriend(updatedFriend: Friend) {
        context.dataStore.edit { preferences ->
            val friendsJson = preferences[friendsKey] ?: "[]"
            val currentFriends = try {
                Json.decodeFromString<List<Friend>>(friendsJson)
            } catch (e: Exception) {
                Timber.e(e, "Error decoding friends data")
                emptyList()
            }
            
            val updatedFriends = currentFriends.map { friend ->
                if (friend.id == updatedFriend.id) updatedFriend else friend
            }
            
            preferences[friendsKey] = Json.encodeToString(updatedFriends)
        }
    }
    
    suspend fun markAsContacted(friendId: String) {
        context.dataStore.edit { preferences ->
            val friendsJson = preferences[friendsKey] ?: "[]"
            val currentFriends = try {
                Json.decodeFromString<List<Friend>>(friendsJson)
            } catch (e: Exception) {
                Timber.e(e, "Error decoding friends data")
                emptyList()
            }
            
            val updatedFriends = currentFriends.map { friend ->
                if (friend.id == friendId) {
                    val now = System.currentTimeMillis()
                    val nextReminder = now + (friend.reminderFrequencyDays * 24 * 60 * 60 * 1000L)
                    friend.copy(
                        lastContactedTime = now,
                        nextReminderTime = nextReminder
                    )
                } else {
                    friend
                }
            }
            
            preferences[friendsKey] = Json.encodeToString(updatedFriends)
        }
    }
    
    suspend fun deleteFriend(friendId: String) {
        context.dataStore.edit { preferences ->
            val friendsJson = preferences[friendsKey] ?: "[]"
            val currentFriends = try {
                Json.decodeFromString<List<Friend>>(friendsJson)
            } catch (e: Exception) {
                Timber.e(e, "Error decoding friends data")
                emptyList()
            }
            
            val updatedFriends = currentFriends.filter { it.id != friendId }
            preferences[friendsKey] = Json.encodeToString(updatedFriends)
        }
    }
    
    suspend fun getFriendsDueForReminder(): List<Friend> {
        val currentTime = System.currentTimeMillis()
        
        var result = emptyList<Friend>()
        context.dataStore.data.collect { preferences ->
            val friendsJson = preferences[friendsKey] ?: "[]"
            try {
                val allFriends = Json.decodeFromString<List<Friend>>(friendsJson)
                result = allFriends.filter { it.nextReminderTime <= currentTime }
            } catch (e: Exception) {
                Timber.e(e, "Error getting friends due for reminder")
            }
        }
        
        return result
    }
}