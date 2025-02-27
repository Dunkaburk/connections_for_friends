package com.example.connections_for_friends.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.connections_for_friends.data.ContactData
import com.example.connections_for_friends.data.ContactImporter
import com.example.connections_for_friends.data.Friend
import com.example.connections_for_friends.data.FriendRepository
import com.example.connections_for_friends.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class FriendsViewModel(private val repository: FriendRepository) : ViewModel() {
    
    // Original unsorted friends list from repository
    private val _friends: Flow<List<Friend>> = repository.friends
    
    // Sorted friends list with advanced sorting logic
    val friends: Flow<List<Friend>> = _friends.map { friendsList ->
        friendsList.sortedWith(compareBy<Friend> { 
            // Primary sort: First show friends who need contact now (past due)
            if (it.nextReminderTime <= System.currentTimeMillis()) {
                0L
            } else {
                // Sort upcoming contacts by time until contact
                it.nextReminderTime - System.currentTimeMillis()
            }
        }.thenBy { 
            // Secondary sort: If both friends have same contact time, sort by name alphabetically
            it.name 
        })
    }
    
    // Contact import state
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState
    
    // Selected contacts for import
    private val _availableContacts = MutableStateFlow<List<ContactData>>(emptyList())
    val availableContacts: StateFlow<List<ContactData>> = _availableContacts
    
    // Count of contacts successfully imported
    private val _contactsImportedCount = MutableStateFlow(0)
    val contactsImportedCount: StateFlow<Int> = _contactsImportedCount
    
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
                val allFriends = _friends.first()
                Timber.d("Scheduling reminders for ${allFriends.size} friends")
                
                allFriends.forEach { friend ->
                    reminderScheduler.scheduleReminder(friend)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error scheduling reminders")
            }
        }
    }
    
    /**
     * Load contacts from the device's contacts
     */
    fun loadContacts(context: Context) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.Loading
                
                val contactImporter = ContactImporter(context)
                val contacts = contactImporter.getContacts()
                
                // Filter out contacts without names
                val validContacts = contacts.filter { it.name.isNotBlank() }
                
                _availableContacts.value = validContacts
                _importState.value = ImportState.Success
                
                Timber.d("Loaded ${validContacts.size} contacts")
            } catch (e: Exception) {
                Timber.e(e, "Error loading contacts")
                _importState.value = ImportState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }
    
    /**
     * Import selected contacts as friends
     */
    fun importContacts(contacts: List<ContactData>, defaultReminderDays: Int) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.Loading
                _contactsImportedCount.value = 0
                
                // Get existing friends to avoid duplicates
                val existingFriends = _friends.first()
                val existingNames = existingFriends.map { it.name.lowercase() }
                
                // Filter out contacts that are already friends (by name, case-insensitive)
                val newContacts = contacts.filter { contact ->
                    !existingNames.contains(contact.name.lowercase())
                }
                
                var importCount = 0
                
                // Add each contact as a friend
                for (contact in newContacts) {
                    val friend = Friend(
                        name = contact.name,
                        birthday = contact.birthday,
                        notes = "Imported from contacts",
                        reminderFrequencyDays = defaultReminderDays
                    )
                    
                    repository.addFriend(friend)
                    importCount++
                }
                
                _contactsImportedCount.value = importCount
                _importState.value = ImportState.Success
                
                Timber.d("Imported $importCount contacts as friends")
            } catch (e: Exception) {
                Timber.e(e, "Error importing contacts")
                _importState.value = ImportState.Error("Failed to import contacts: ${e.message}")
            }
        }
    }
    
    /**
     * Reset the import state
     */
    fun resetImportState() {
        _importState.value = ImportState.Idle
        _contactsImportedCount.value = 0
    }
}

// Import state to track the contact import process
sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
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