package com.example.connections_for_friends.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.connections_for_friends.data.Friend
import com.example.connections_for_friends.data.FriendRepository
import com.example.connections_for_friends.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class FriendsViewModel(private val repository: FriendRepository) : ViewModel() {
    
    val friends: Flow<List<Friend>> = repository.friends
    
    fun addFriend(name: String, birthday: String, notes: String, reminderFrequencyDays: Int) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            val friend = Friend(
                name = name,
                birthday = birthday,
                notes = notes,
                reminderFrequencyDays = reminderFrequencyDays
            )
            
            repository.addFriend(friend)
            Timber.d("Added friend: $name with birthday: $birthday")
        }
    }
    
    fun markAsContacted(friendId: String) {
        viewModelScope.launch {
            repository.markAsContacted(friendId)
            Timber.d("Marked friend as contacted: $friendId")
        }
    }
    
    fun deleteFriend(friendId: String) {
        viewModelScope.launch {
            repository.deleteFriend(friendId)
            Timber.d("Deleted friend: $friendId")
        }
    }
    
    fun scheduleAllReminders(reminderScheduler: ReminderScheduler) {
        viewModelScope.launch {
            try {
                val allFriends = repository.friends.first()
                Timber.d("Scheduling reminders for ${allFriends.size} friends")
                
                allFriends.forEach { friend ->
                    reminderScheduler.scheduleReminder(friend)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error scheduling reminders")
            }
        }
    }
}

class FriendsViewModelFactory(private val repository: FriendRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}