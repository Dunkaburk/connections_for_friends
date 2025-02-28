package com.example.connections_for_friends.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.connections_for_friends.MainActivity
import com.example.connections_for_friends.R
import com.example.connections_for_friends.data.Friend
import com.example.connections_for_friends.data.FriendRepository
import kotlinx.coroutines.flow.first

class FriendsToContactWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = FriendRepository(context)
        val now = System.currentTimeMillis()
        
        val allFriends = repository.friends.first()
        
        timber.log.Timber.d("Widget: Total friends count: ${allFriends.size}")
        allFriends.forEach { friend ->
            val isOverdue = friend.nextReminderTime <= now
            val timeDiff = friend.nextReminderTime - now
            val daysDiff = timeDiff / (24 * 60 * 60 * 1000)
            timber.log.Timber.d("Friend: ${friend.name}, Reminder: ${friend.nextReminderTime}, Current: $now, Overdue: $isOverdue, Days until due: $daysDiff")
        }
        
        val friendsNeedingContact = allFriends.filter {
            it.lastContactedTime == null || it.nextReminderTime <= now
        }

        val friendsToContact = friendsNeedingContact
            .sortedWith(compareBy<Friend> {
                if (it.lastContactedTime == null) {
                    0L // Default value for never contacted, push it to the bottom lol
                } else {
                    now - it.nextReminderTime
                }
            }.reversed())
        
        provideContent {
            WidgetContent(
                context = context,
                friendsToContact = friendsToContact
            )
        }
    }
    
    @Composable
    private fun WidgetContent(
        context: Context,
        friendsToContact: List<Friend>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .clickable(actionStartActivity<MainActivity>())
        ) {
                if (friendsToContact.isEmpty()) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.widget_empty),
                            style = TextStyle(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                    // Friends list
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.widget_title),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(bottom = 8.dp)
                        )
                        
                        // Show up to 5 friends currently to avoid overcrowding the widget, maybe add scrolling in future?
                        val displayedFriends = friendsToContact.take(5)
                        
                        displayedFriends.forEach { friend ->
                            FriendToContactItem(context, friend)
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                        
                        if (friendsToContact.size > 5) {
                            Text(
                                text = "+${friendsToContact.size - 5} more",
                                style = TextStyle(
                                    textAlign = TextAlign.Center
                                ),
                                modifier = GlanceModifier.fillMaxWidth()
                            )
                        }

                    }
                }
            }
        }
    }
    
    @Composable
    private fun FriendToContactItem(context: Context, friend: Friend) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = friend.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                val statusText = if (friend.lastContactedTime == null) {
                    "Never contacted"
                } else {
                    val now = System.currentTimeMillis()
                    val daysOverdue = ((now - friend.nextReminderTime) / 
                            (1000 * 60 * 60 * 24)).toInt()
                    
                    if (daysOverdue > 0) {
                        "$daysOverdue days overdue"
                    } else {
                        context.getString(R.string.widget_due)
                    }
                }
                
                Text(
                    text = statusText,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }


