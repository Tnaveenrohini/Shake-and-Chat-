package com.flikk.shakechat.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flikk.shakechat.data.Friend
import com.flikk.shakechat.data.Options
import com.flikk.shakechat.data.Profile
import com.flikk.shakechat.data.Repo
import com.flikk.shakechat.data.ShakeDetector
import com.flikk.shakechat.data.vibrate
import com.flikk.shakechat.data.vibrateMatch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    me: Profile,
    autoShake: Boolean,
    onMatch: (roomId: String) -> Unit,
    onOpenFriend: (Friend) -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onProfileChanged: (Profile) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ShakeTab(me, autoShake, onMatch)
                1 -> FriendsTab(onOpenFriend)
                else -> MeTab(me, onEditProfile, onLogout, onProfileChanged)
            }
        }
        NavigationBar(containerColor = C.card, modifier = Modifier.navigationBarsPadding()) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White, selectedTextColor = C.pink,
                indicatorColor = C.pink, unselectedIconColor = C.dim, unselectedTextColor = C.dim,
            )
            NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Vibration, null) }, label = { Text("Shake") }, colors = colors)
            NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.People, null) }, label = { Text("Friends") }, colors = colors)
            NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Filled.Person, null) }, label = { Text("Me") }, colors = colors)
        }
    }
}

private enum class St { Idle, Searching, NoOne, Error }

@Composable
private fun ShakeTab(me: Profile, autoShake: Boolean, onMatch: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var st by remember { mutableStateOf(St.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }
    var errMsg by remember { mutableStateOf("") }
    val matchCb by rememberUpdatedState(onMatch)

    fun start() {
        if (st == St.Searching) return
        vibrate(ctx, 80)
        st = St.Searching
        job = scope.launch {
            try {
                val rid = Repo.findMatch(me)
                if (rid != null) {
                    vibrateMatch(ctx)
                    st = St.Idle
                    matchCb(rid)
                } else st = St.NoOne
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                errMsg = friendlyError(e); st = St.Error
            }
        }
    }

    fun cancel() {
        job?.cancel(); job = null
        st = St.Idle
        scope.launch { Repo.cancelSearch() }
    }

    // Shake sensor – search lo lenappudu matrame
    val detector = remember { ShakeDetector(ctx) { } }
    val startRef by rememberUpdatedState({ start() })
    DisposableEffect(st) {
        val d = ShakeDetector(ctx) { startRef() }
        if (st != St.Searching) d.start()
        onDispose { d.stop() }
    }
    LaunchedEffect(Unit) { if (autoShake) start() }

    val inf = rememberInfiniteTransition(label = "shake")
    val wobble by inf.animateFloat(
        -12f, 12f,
        infiniteRepeatable(tween(if (st == St.Searching) 110 else 900, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobble",
    )
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "pulse")

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Shake and Chat", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Pill("🗣️ ${me.lang}", selected = false)
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier.size(260.dp).clickable(enabled = st != St.Searching) { start() },
            contentAlignment = Alignment.Center,
        ) {
            if (st == St.Searching) {
                // pulsing rings
                for (i in 0..2) {
                    val p = (pulse + i / 3f) % 1f
                    Box(
                        Modifier
                            .size(140.dp)
                            .scale(1f + p * 0.9f)
                            .alpha(1f - p)
                            .border(3.dp, C.pink, CircleShape)
                    )
                }
            }
            Box(
                Modifier.size(170.dp).clip(CircleShape).background(C.grad),
                contentAlignment = Alignment.Center,
            ) {
                Text("📱", fontSize = 76.sp, modifier = Modifier.rotate(wobble))
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            when (st) {
                St.Idle -> "Shake your phone!"
                St.Searching -> "Finding someone…"
                St.NoOne -> "Evaru dorakaledu 😕"
                St.Error -> "Oops!"
            },
            color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (st) {
                St.Idle -> if (detector.available) "Phone ni 2 saarlu gattiga oopandi, or circle ni tap cheyyandi"
                else "Circle ni tap cheyyandi"
                St.Searching -> "${me.interests.joinToString(" ") { Options.emoji(it) }}  same vibe unna vaallani vethukuthunnam"
                St.NoOne -> "Ippudu online lo evaru levu. Malli shake cheyyandi."
                St.Error -> errMsg
            },
            color = C.dim, fontSize = 14.sp, textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
        if (st == St.Searching) {
            TextButton(onClick = { cancel() }) { Text("Cancel", color = C.dim, fontSize = 16.sp) }
        } else {
            GradButton("🫨  Shake now") { start() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FriendsTab(onOpen: (Friend) -> Unit) {
    val friends by produceState(emptyList<Friend>()) {
        val q = Repo.friendsQuery()
        val reg = q?.addSnapshotListener { s, _ ->
            value = s?.documents?.map { d ->
                Friend(
                    uid = d.id,
                    name = d.getString("name") ?: "Friend",
                    age = (d.getLong("age") ?: 0L).toInt(),
                    roomId = d.getString("roomId") ?: "",
                    since = d.getLong("since") ?: 0L,
                )
            } ?: emptyList()
        }
        awaitDispose { reg?.remove() }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Friends", color = C.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Iddaru ➕ Friend nokkina vaallu ikkada untaru", color = C.dim, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        if (friends.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Inka friends levu.\nShake chesi kotha vaallani kalavandi 🫨",
                    color = C.dim, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(friends, key = { it.uid }) { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.card)
                            .clickable { onOpen(f) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(f.name, 46)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(f.name, color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("${f.age} • tap to chat", color = C.dim, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Avatar(name: String, sizeDp: Int) {
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(C.grad),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = (sizeDp * 0.42f).sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeTab(me: Profile, onEdit: () -> Unit, onLogout: () -> Unit, onProfileChanged: (Profile) -> Unit) {
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Avatar(me.name, 90)
        Spacer(Modifier.height(12.dp))
        Text(me.name, color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${me.age} • ${me.gender} • ${me.lang}", color = C.dim)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            me.interests.forEach { Pill("${Options.emoji(it)} $it", selected = false) }
        }
        Spacer(Modifier.height(28.dp))
        GradButton("✏️  Edit profile") { onEdit() }
        Spacer(Modifier.height(12.dp))
        MenuRow("📜  Community rules") { rules = true }
        if (me.blocked.isNotEmpty()) {
            MenuRow("🚫  Unblock all (${me.blocked.size})") {
                scope.launch {
                    runCatching { Repo.unblockAll() }
                    onProfileChanged(me.copy(blocked = emptyList()))
                }
            }
        }
        MenuRow("🚪  Logout") { onLogout() }
        Spacer(Modifier.height(20.dp))
        Text("Shake and Chat • Made in India 🇮🇳", color = C.dim, fontSize = 12.sp)
    }
    if (rules) {
        AlertDialog(
            onDismissRequest = { rules = false },
            confirmButton = { TextButton({ rules = false }) { Text("OK", color = C.pink) } },
            title = { Text("Community rules") },
            text = {
                Text(
                    "• 18+ only\n" +
                        "• Abuse, bullying, hate – no\n" +
                        "• Nudity / sexual messages – no\n" +
                        "• Spam, scams, money asking – no\n" +
                        "• Strangers tho phone number / Insta share avvadu – friends ayyaka matrame\n\n" +
                        "Evaraina ibbandi pedithe ⋮ menu lo Block / Report cheyyandi."
                )
            },
            containerColor = C.card,
        )
    }
}

@Composable
private fun MenuRow(t: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) { Text(t, color = C.text, fontSize = 16.sp) }
}
