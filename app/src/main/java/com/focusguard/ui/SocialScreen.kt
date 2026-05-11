package com.focusguard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.social.SocialActionResult
import com.focusguard.social.SocialFriendRequest
import com.focusguard.social.SocialProfile
import com.focusguard.social.SocialSnapshot
import com.focusguard.social.SupabaseSocialRepository
import com.focusguard.social.normalizeUsername
import com.focusguard.social.socialProfileId
import com.focusguard.state.EarnedItStore
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors
import kotlinx.coroutines.launch

@Composable
fun SocialScreen() {
    val appState by EarnedItStore.state.collectAsState()
    val repository = remember { SupabaseSocialRepository() }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val socialProfileId = appState.socialProfileId()

    var snapshot by remember { mutableStateOf<SocialSnapshot?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showAddFriend by remember { mutableStateOf(false) }
    var profileUsername by remember(appState.profile.username) { mutableStateOf(appState.profile.username) }

    fun refresh() {
        if (!repository.isConfigured) {
            message = "Add SUPABASE_ANON_KEY to local.properties to enable live friends."
            return
        }
        scope.launch {
            loading = true
            repository.loadSnapshot(appState)
                .onSuccess {
                    snapshot = it
                    message = null
                }
                .onFailure { message = it.message ?: "Could not load social data." }
            loading = false
        }
    }

    LaunchedEffect(
        socialProfileId,
        appState.profile.username,
        appState.points,
        appState.weeklyFocusMinutes,
        appState.pet.species,
        appState.pet.stage,
    ) {
        refresh()
    }

    if (showAddFriend) {
        AddFriendDialog(
            onDismiss = { showAddFriend = false },
            onSend = { username ->
                haptics.confirm()
                scope.launch {
                    loading = true
                    message = socialActionMessage(repository.sendFriendRequest(appState, username))
                    showAddFriend = false
                    repository.loadSnapshot(appState).onSuccess { snapshot = it }
                    loading = false
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SocialHeader(
                loading = loading,
                onAddFriend = {
                    haptics.tap()
                    showAddFriend = true
                },
                onRefresh = {
                    haptics.tap()
                    refresh()
                }
            )
        }

        item {
            SocialProfileCard(
                username = profileUsername,
                displayName = appState.profile.displayName,
                userId = socialProfileId,
                onUsernameChange = { profileUsername = it },
                onSave = {
                    haptics.confirm()
                    EarnedItStore.updateSocialProfile(appState.profile.displayName, profileUsername)
                }
            )
        }

        if (!repository.isConfigured) {
            item { SocialSetupCard() }
        }

        message?.let { text ->
            item { SocialMessageCard(text) }
        }

        item {
            SocialPodium(snapshot?.leaderboard.orEmpty())
        }

        item {
            val leaderboard = snapshot?.leaderboard.orEmpty()
            val myRank = leaderboard.indexOfFirst { it.id == socialProfileId }.takeIf { it >= 0 }?.plus(1)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialKpiCard(
                    label = "Your rank",
                    value = myRank?.let { "#$it" } ?: "-",
                    icon = Icons.Filled.EmojiEvents,
                    tint = EarnedColors.Points,
                    modifier = Modifier.weight(1f)
                )
                SocialKpiCard(
                    label = "Friends",
                    value = "${snapshot?.friends?.size ?: 0}",
                    icon = Icons.Filled.Person,
                    tint = EarnedColors.Focus,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SocialTabs(selectedTab = selectedTab, onSelected = { selectedTab = it })
        }

        when (selectedTab) {
            0 -> {
                val rows = snapshot?.leaderboard.orEmpty()
                if (rows.isEmpty()) item { EmptySocialPanel("No leaderboard yet", "Add friends to compare weekly focus and points.") }
                items(rows, key = { it.id }) { profile ->
                    LeaderboardProfileRow(
                        rank = rows.indexOf(profile) + 1,
                        profile = profile,
                        isMe = profile.id == socialProfileId
                    )
                }
            }
            1 -> {
                val friends = snapshot?.friends.orEmpty()
                if (friends.isEmpty()) item { EmptySocialPanel("No friends yet", "Tap the add button and send a request by username.") }
                items(friends, key = { it.id }) { profile ->
                    FriendProfileRow(profile)
                }
            }
            else -> {
                val incoming = snapshot?.incomingRequests.orEmpty()
                val outgoing = snapshot?.outgoingRequests.orEmpty()
                if (incoming.isEmpty() && outgoing.isEmpty()) {
                    item { EmptySocialPanel("No open requests", "Friend requests you send or receive will show up here.") }
                }
                if (incoming.isNotEmpty()) {
                    item { RequestSectionTitle("Incoming") }
                    items(incoming, key = { it.id }) { request ->
                        RequestRow(
                            request = request,
                            incoming = true,
                            onAccept = {
                                haptics.confirm()
                                scope.launch {
                                    loading = true
                                    message = socialActionMessage(repository.acceptRequest(request.id))
                                    repository.loadSnapshot(appState).onSuccess { snapshot = it }
                                    loading = false
                                }
                            },
                            onDecline = {
                                haptics.tap()
                                scope.launch {
                                    loading = true
                                    message = socialActionMessage(repository.declineRequest(request.id))
                                    repository.loadSnapshot(appState).onSuccess { snapshot = it }
                                    loading = false
                                }
                            }
                        )
                    }
                }
                if (outgoing.isNotEmpty()) {
                    item { RequestSectionTitle("Sent") }
                    items(outgoing, key = { it.id }) { request ->
                        RequestRow(request = request, incoming = false, onAccept = {}, onDecline = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialHeader(
    loading: Boolean,
    onAddFriend: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Social", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Friends, requests, and the weekly leaderboard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onRefresh, shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Surface(onClick = onAddFriend, shape = CircleShape, color = EarnedColors.Primary, shadowElevation = 2.dp, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = "Add friend", tint = Color.White, modifier = Modifier.size(21.dp))
                }
            }
        }
    }
}

@Composable
private fun SocialProfileCard(
    username: String,
    displayName: String,
    userId: String,
    onUsernameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your friend username", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = username,
                    onValueChange = { onUsernameChange(normalizeUsername(it)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    prefix = { Text("@") }
                )
                Button(onClick = onSave, enabled = username.isNotBlank()) {
                    Text("Save")
                }
            }
            Text("$displayName · ID ${userId.take(8)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SocialSetupCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = EarnedColors.Warning.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Supabase key needed", fontWeight = FontWeight.Bold, color = EarnedColors.Warning)
            Text(
                "Add SUPABASE_ANON_KEY to focus-guard/local.properties. The app is wired for live profiles, friend requests, friends, and leaderboard rows.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SocialMessageCard(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(14.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SocialPodium(leaderboard: List<SocialProfile>) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Image(
            painter = painterResource(id = R.drawable.social_podium_garden),
            contentDescription = "Top friends podium garden",
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun SocialKpiCard(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.14f), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SocialTabs(selectedTab: Int, onSelected: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, divider = {}) {
            listOf("Leaderboard", "Friends", "Requests").forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { onSelected(index) }, text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            }
        }
    }
}

@Composable
private fun LeaderboardProfileRow(rank: Int, profile: SocialProfile, isMe: Boolean) {
    Surface(shape = RoundedCornerShape(18.dp), color = if (isMe) EarnedColors.Primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(rank.toString(), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = podiumColor(rank))
            SocialAvatar(profile)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isMe) "@${profile.username}  You" else "@${profile.username}", fontWeight = FontWeight.Bold)
                Text("${profile.weeklyFocusMinutes}m this week · ${profile.streakDays}d streak", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = EarnedColors.Points, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("%,d".format(profile.points), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FriendProfileRow(profile: SocialProfile) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SocialAvatar(profile)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.displayName, fontWeight = FontWeight.Bold)
                Text("@${profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text("${profile.weeklyFocusMinutes}m", fontWeight = FontWeight.Bold, color = EarnedColors.Focus)
        }
    }
}

@Composable
private fun RequestRow(
    request: SocialFriendRequest,
    incoming: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val profile = if (incoming) request.requester else request.addressee
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (profile != null) SocialAvatar(profile) else PlaceholderAvatar()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile?.displayName ?: "Unknown user", fontWeight = FontWeight.Bold)
                Text(profile?.let { "@${it.username}" } ?: "Pending profile", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            if (incoming) {
                IconButton(onClick = onDecline) { Icon(Icons.Filled.Close, contentDescription = "Decline") }
                IconButton(onClick = onAccept) { Icon(Icons.Filled.Check, contentDescription = "Accept", tint = EarnedColors.Focus) }
            } else {
                Text("Sent", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SocialAvatar(profile: SocialProfile) {
    Surface(shape = CircleShape, color = speciesColor(profile.petSpecies).copy(alpha = 0.13f), modifier = Modifier.size(54.dp)) {
        Box(contentAlignment = Alignment.Center) {
            PetSprite(profile.petProfile, size = 52.dp, glow = false)
        }
    }
}

@Composable
private fun PlaceholderAvatar() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(54.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptySocialPanel(title: String, detail: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RequestSectionTitle(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add friend") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Send a friend request using their username.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                TextField(
                    value = username,
                    onValueChange = { username = normalizeUsername(it) },
                    singleLine = true,
                    prefix = { Text("@") },
                    placeholder = { Text("username") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(username) }, enabled = username.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val SocialProfile.petProfile: PetProfile
    get() = PetProfile(
        name = displayName,
        species = petSpecies,
        stage = petStage,
        fullness = 86,
        mood = "Focused",
    )

private fun socialActionMessage(result: SocialActionResult): String =
    when (result) {
        SocialActionResult.Success -> "Done."
        SocialActionResult.NotConfigured -> "Supabase is not configured yet."
        SocialActionResult.UserNotFound -> "No user found with that username."
        SocialActionResult.CannotAddSelf -> "That is your username."
        SocialActionResult.AlreadyConnected -> "You already have a request or friendship with that user."
        SocialActionResult.UsernameTaken -> "That username is already taken."
        is SocialActionResult.Failure -> result.message
    }

private fun podiumColor(rank: Int): Color =
    when (rank) {
        1 -> EarnedColors.Points
        2 -> EarnedColors.Secondary
        3 -> EarnedColors.Focus
        else -> Color(0xFF667267)
    }

private fun speciesColor(species: String): Color =
    when (species) {
        "kitsu" -> EarnedColors.Primary
        "lumi" -> EarnedColors.Focus
        "owly" -> EarnedColors.Secondary
        else -> EarnedColors.Primary
    }
