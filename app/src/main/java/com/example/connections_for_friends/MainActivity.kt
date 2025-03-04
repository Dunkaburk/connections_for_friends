package com.example.connections_for_friends

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.connections_for_friends.data.ContactData
import com.example.connections_for_friends.data.Friend
import com.example.connections_for_friends.notification.ReminderScheduler
import com.example.connections_for_friends.ui.theme.Connections_for_friendsTheme
import com.example.connections_for_friends.ui.theme.NeedContactRed
import com.example.connections_for_friends.ui.theme.NeutralCardColor
import com.example.connections_for_friends.ui.theme.RecentlyContactedGreen
import com.example.connections_for_friends.viewmodel.FriendsViewModel
import com.example.connections_for_friends.viewmodel.FriendsViewModelFactory
import com.example.connections_for_friends.viewmodel.ImportState
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
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedFriendId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connections") },
                actions = {
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Import contacts",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
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
        
        if (showImportDialog) {
            ImportContactsDialog(
                viewModel = viewModel,
                onDismiss = { showImportDialog = false }
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
        
        // Section: Upcoming Contacts, maybe add more?
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
    val daysSinceLastContact = if (friend.lastContactedTime != null) {
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - friend.lastContactedTime)
    } else {
        null
    }
    
    val daysUntilNextContact = if (friend.nextReminderTime > System.currentTimeMillis()) {
        TimeUnit.MILLISECONDS.toDays(friend.nextReminderTime - System.currentTimeMillis())
    } else {
        0L
    }
    
    val cardBackgroundColor = when {
        daysSinceLastContact != null && daysSinceLastContact <= 2L -> RecentlyContactedGreen
        daysUntilNextContact <= 2L -> NeedContactRed
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
                    ).coerceAtLeast(0)
                    
                    if (daysUntilBirthday <= 14) {
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
    
    var isValidBirthday by remember { mutableStateOf(true) }
    var birthdayError by remember { mutableStateOf("") }
    
    fun validateBirthday(input: String): Boolean {
        if (input.isBlank()) return true
        
        val regex = Regex("^(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$")
        if (!regex.matches(input)) {
            birthdayError = "Use format MM-DD (e.g., 12-25)"
            return false
        }
        
        try {
            val parts = input.split("-")
            val month = parts[0].toInt()
            val day = parts[1].toInt()
            
            if (month < 1 || month > 12) {
                birthdayError = "Month must be between 1-12"
                return false
            }
            
            val maxDays = when(month) {
                2 -> 29
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

@Composable
fun ImportContactsDialog(
    viewModel: FriendsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsState()
    val availableContacts by viewModel.availableContacts.collectAsState()
    val contactsImportedCount by viewModel.contactsImportedCount.collectAsState()
    
    val hasContactPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        if (isGranted) {
            viewModel.loadContacts(context)
        }
    }
    
    val selectedContacts = remember { mutableStateListOf<ContactData>() }
    var reminderFrequencyDays by remember { mutableIntStateOf(30) }
    var isSelectAll by remember { mutableStateOf(false) }
    
    LaunchedEffect(hasContactPermission) {
        if (hasContactPermission && importState is ImportState.Idle) {
            viewModel.loadContacts(context)
        }
    }
    
    LaunchedEffect(isSelectAll, availableContacts) {
        if (isSelectAll) {
            selectedContacts.clear()
            selectedContacts.addAll(availableContacts)
        } else if (selectedContacts.size == availableContacts.size) {
            selectedContacts.clear()
        }
    }
    
    AlertDialog(
        onDismissRequest = {
            viewModel.resetImportState()
            onDismiss()
        },
        title = { Text("Import Friends from Contacts") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (importState) {
                    is ImportState.Idle -> {
                        if (hasContactPermission) {
                            Text("Loading contacts...")
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Permission needed to access contacts")
                                Button(onClick = { 
                                    requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                    
                    is ImportState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading contacts...")
                        }
                    }
                    
                    is ImportState.Success -> {
                        if (contactsImportedCount > 0) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Successfully imported $contactsImportedCount friends!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(onClick = {
                                    viewModel.resetImportState()
                                    onDismiss()
                                }) {
                                    Text("Done")
                                }
                            }
                        } else if (availableContacts.isEmpty()) {
                            Text("No contacts found on your device.")
                        } else {
                            Column {
                                // Default reminder frequency slider
                                Text("Default reminder frequency: $reminderFrequencyDays days")
                                Slider(
                                    value = reminderFrequencyDays.toFloat(),
                                    onValueChange = { reminderFrequencyDays = it.roundToInt() },
                                    valueRange = 1f..365f,
                                    steps = 364,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Select all checkbox
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelectAll,
                                        onCheckedChange = { isSelectAll = it }
                                    )
                                    
                                    Text(
                                        text = "Select All (${availableContacts.size} contacts)",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                
                                // Contacts list
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(availableContacts) { contact ->
                                        val isSelected = selectedContacts.contains(contact)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedContacts.remove(contact)
                                                    } else {
                                                        selectedContacts.add(contact)
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    if (it) {
                                                        selectedContacts.add(contact)
                                                    } else {
                                                        selectedContacts.remove(contact)
                                                    }
                                                }
                                            )
                                            
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    text = contact.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                
                                                if (contact.birthday.isNotBlank()) {
                                                    Text(
                                                        text = "Has birthday",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Selected count
                                Text(
                                    text = "${selectedContacts.size} contacts selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    
                    is ImportState.Error -> {
                        val errorState = importState as ImportState.Error
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: ${errorState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(onClick = {
                                viewModel.resetImportState()
                                if (!hasContactPermission) {
                                    requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                } else {
                                    viewModel.loadContacts(context)
                                }
                            }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (importState is ImportState.Success && contactsImportedCount == 0) {
                Button(
                    onClick = {
                        if (selectedContacts.isNotEmpty()) {
                            viewModel.importContacts(selectedContacts, reminderFrequencyDays)
                        } else {
                            onDismiss()
                        }
                    },
                    enabled = selectedContacts.isNotEmpty() || availableContacts.isEmpty()
                ) {
                    Text(if (selectedContacts.isNotEmpty()) "Import Selected" else "Close")
                }
            }
        },
        dismissButton = {
            if (importState is ImportState.Success && contactsImportedCount == 0) {
                Button(onClick = {
                    viewModel.resetImportState()
                    onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        }
    )
}