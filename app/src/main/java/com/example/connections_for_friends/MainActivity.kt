package com.example.connections_for_friends

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.connections_for_friends.data.Friend
import com.example.connections_for_friends.notification.ReminderScheduler
import com.example.connections_for_friends.ui.theme.Connections_for_friendsTheme
import com.example.connections_for_friends.ui.theme.NeedContactRed
import com.example.connections_for_friends.ui.theme.NeutralCardColor
import com.example.connections_for_friends.ui.theme.RecentlyContactedGreen
import com.example.connections_for_friends.viewmodel.FriendsViewModel
import com.example.connections_for_friends.viewmodel.FriendsViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    
    private val viewModel: FriendsViewModel by viewModels {
        FriendsViewModelFactory((application as FriendsApplication).repository)
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, schedule reminders
            scheduleReminders()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scheduleReminders()
            }
        } else {
            scheduleReminders()
        }
        
        setContent {
            Connections_for_friendsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FriendsApp(viewModel)
                }
            }
        }
    }
    
    private fun scheduleReminders() {
        val reminderScheduler = ReminderScheduler(this)
        viewModel.scheduleAllReminders(reminderScheduler)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsApp(viewModel: FriendsViewModel) {
    val friends by viewModel.friends.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFriendId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Connections") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Friend"
                )
            }
        }
    ) { innerPadding ->
        if (showAddDialog) {
            AddFriendDialog(
                onDismiss = { showAddDialog = false },
                onAddFriend = { name, birthday, notes, reminderFrequencyDays ->
                    viewModel.addFriend(name, birthday, notes, reminderFrequencyDays)
                }
            )
        }
        
        selectedFriendId?.let { friendId ->
            val friend = friends.find { it.id == friendId }
            if (friend != null) {
                FriendDetailDialog(
                    friend = friend,
                    onDismiss = { selectedFriendId = null },
                    onContactedClick = {
                        viewModel.markAsContacted(friendId)
                        selectedFriendId = null
                    },
                    onDeleteClick = {
                        viewModel.deleteFriend(friendId)
                        selectedFriendId = null
                    }
                )
            }
        }
        
        if (friends.isEmpty()) {
            EmptyFriendsList(modifier = Modifier.padding(innerPadding))
        } else {
            FriendsList(
                friends = friends,
                onFriendClick = { selectedFriendId = it },
                onContactedClick = { viewModel.markAsContacted(it) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun EmptyFriendsList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No friends added yet",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Tap the + button to add a friend",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun FriendsList(
    friends: List<Friend>,
    onFriendClick: (String) -> Unit,
    onContactedClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group friends by their contact status
    val currentTime = System.currentTimeMillis()
    val needContactNow = friends.filter { it.nextReminderTime <= currentTime }
    val upcomingContact = friends.filter { it.nextReminderTime > currentTime }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section: Need Contact Now
        if (needContactNow.isNotEmpty()) {
            item {
                Text(
                    text = "Need Contact Now",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(
                items = needContactNow,
                key = { it.id }
            ) { friend ->
                FriendItem(
                    friend = friend,
                    onClick = { onFriendClick(friend.id) },
                    onContactedClick = { onContactedClick(friend.id) }
                )
            }
        }
        
        // Section: Upcoming Contacts
        if (upcomingContact.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            items(
                items = upcomingContact,
                key = { it.id }
            ) { friend ->
                FriendItem(
                    friend = friend,
                    onClick = { onFriendClick(friend.id) },
                    onContactedClick = { onContactedClick(friend.id) }
                )
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    onClick: () -> Unit,
    onContactedClick: () -> Unit
) {
    // Calculate days since last contact
    val daysSinceLastContact = if (friend.lastContactedTime != null) {
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - friend.lastContactedTime)
    } else {
        null
    }
    
    // Calculate days until next contact
    val daysUntilNextContact = if (friend.nextReminderTime > System.currentTimeMillis()) {
        TimeUnit.MILLISECONDS.toDays(friend.nextReminderTime - System.currentTimeMillis())
    } else {
        0L // Contact needed now
    }
    
    // Determine card background color based on contact status
    val cardBackgroundColor = when {
        // Recently contacted (within last 2 days)
        daysSinceLastContact != null && daysSinceLastContact <= 2L -> RecentlyContactedGreen
        // Needs contact soon (within next 2 days)
        daysUntilNextContact <= 2L -> NeedContactRed
        // Normal status
        else -> NeutralCardColor
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (daysSinceLastContact != null) {
                        if (daysSinceLastContact == 0L) "Contacted today" else "Last contacted $daysSinceLastContact days ago"
                    } else {
                        "Never contacted"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "Remind every ${friend.reminderFrequencyDays} days",
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Add the days until next contact
                Text(
                    text = if (daysUntilNextContact == 0L) {
                        "Contact needed now"
                    } else if (daysUntilNextContact == 1L) {
                        "Contact needed tomorrow"
                    } else {
                        "Contact in $daysUntilNextContact days"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        daysUntilNextContact == 0L -> MaterialTheme.colorScheme.error
                        daysUntilNextContact <= 2L -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        daysUntilNextContact <= 7L -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.typography.bodySmall.color
                    }
                )
                
                // Show birthday information if available
                if (friend.birthday.isNotBlank() && friend.nextBirthdayTime != null) {
                    val daysUntilBirthday = TimeUnit.MILLISECONDS.toDays(
                        friend.nextBirthdayTime - System.currentTimeMillis()
                    ).coerceAtLeast(0) // Never show negative days
                    
                    if (daysUntilBirthday <= 14) { // Show only if within 2 weeks
                        Text(
                            text = if (daysUntilBirthday == 0L) {
                                "🎂 Birthday today!"
                            } else if (daysUntilBirthday == 1L) {
                                "🎂 Birthday tomorrow!"
                            } else {
                                "🎂 Birthday in $daysUntilBirthday days"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            IconButton(onClick = onContactedClick) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Mark as contacted"
                )
            }
        }
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAddFriend: (name: String, birthday: String, notes: String, reminderFrequencyDays: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reminderFrequencyDays by remember { mutableIntStateOf(30) }
    
    // For birthday input validation
    var isValidBirthday by remember { mutableStateOf(true) }
    var birthdayError by remember { mutableStateOf("") }
    
    fun validateBirthday(input: String): Boolean {
        if (input.isBlank()) return true // Optional field
        
        val regex = Regex("^(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$")
        if (!regex.matches(input)) {
            birthdayError = "Use format MM-DD (e.g., 12-25)"
            return false
        }
        
        try {
            val parts = input.split("-")
            val month = parts[0].toInt()
            val day = parts[1].toInt()
            
            // Basic validation of month and day
            if (month < 1 || month > 12) {
                birthdayError = "Month must be between 1-12"
                return false
            }
            
            // Simple validation for days in month
            val maxDays = when(month) {
                2 -> 29 // Simplification for February (allowing leap year)
                4, 6, 9, 11 -> 30
                else -> 31
            }
            
            if (day < 1 || day > maxDays) {
                birthdayError = "Invalid day for month $month"
                return false
            }
            
            return true
        } catch (e: Exception) {
            birthdayError = "Invalid format"
            return false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { 
                        birthday = it
                        isValidBirthday = validateBirthday(it)
                    },
                    label = { Text("Birthday (MM-DD) (optional)") },
                    placeholder = { Text("Example: 12-25") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isValidBirthday,
                    supportingText = {
                        if (!isValidBirthday) {
                            Text(birthdayError)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                Text("Remind every $reminderFrequencyDays days")
                
                Slider(
                    value = reminderFrequencyDays.toFloat(),
                    onValueChange = { reminderFrequencyDays = it.roundToInt() },
                    valueRange = 1f..60f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && isValidBirthday) {
                        onAddFriend(name, birthday, notes, reminderFrequencyDays)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank() && isValidBirthday
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FriendDetailDialog(
    friend: Friend,
    onDismiss: () -> Unit,
    onContactedClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(friend.name) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (friend.birthday.isNotBlank()) {
                    // Format the birthday to display month and day
                    val birthdayText = try {
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
                    
                    Text(
                        text = "Birthday: $birthdayText",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // If birthday is set, show days until next birthday
                    friend.nextBirthdayTime?.let { nextBirthdayTime ->
                        val daysUntilBirthday = TimeUnit.MILLISECONDS.toDays(
                            nextBirthdayTime - System.currentTimeMillis()
                        ).coerceAtLeast(0) // Never show negative days
                        
                        Text(
                            text = "Days until birthday: $daysUntilBirthday",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (friend.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${friend.notes}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = "Reminder: Every ${friend.reminderFrequencyDays} days",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (friend.lastContactedTime != null) {
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val date = Date(friend.lastContactedTime)
                    
                    val daysSince = TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - friend.lastContactedTime
                    )
                    
                    Text(
                        text = "Last contacted: ${dateFormat.format(date)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Days since last contact: $daysSince",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    val nextReminderDate = Date(friend.nextReminderTime)
                    Text(
                        text = "Next reminder: ${dateFormat.format(nextReminderDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Calculate and display days until next contact
                    val daysUntilNextContact = if (friend.nextReminderTime > System.currentTimeMillis()) {
                        TimeUnit.MILLISECONDS.toDays(friend.nextReminderTime - System.currentTimeMillis())
                    } else {
                        0L
                    }
                    
                    Text(
                        text = if (daysUntilNextContact == 0L) {
                            "Contact needed now"
                        } else {
                            "Days until next contact: $daysUntilNextContact"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            daysUntilNextContact <= 2L -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                } else {
                    Text(
                        text = "Never contacted",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // For never contacted friends, next contact is due immediately
                    Text(
                        text = "Contact needed now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onContactedClick) {
                Text("Mark as Contacted")
            }
        },
        dismissButton = {
            Button(onClick = onDeleteClick) {
                Text("Delete")
            }
        }
    )
}