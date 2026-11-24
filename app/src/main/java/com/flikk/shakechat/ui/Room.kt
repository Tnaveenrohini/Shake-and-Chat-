package com.flikk.shakechat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flikk.shakechat.data.Filter
import com.flikk.shakechat.data.Msg
import com.flikk.shakechat.data.Options
import com.flikk.shakechat.data.Peer
import com.flikk.shakechat.data.Profile
import com.flikk.shakechat.data.Repo
import com.flikk.shakechat.data.vibrate
import kotlinx.coroutines.launch

/**
 * Chat room. isFriend = false ante stranger room (Next / Add friend / number hide).
 * onExit(next=true) ante malli shake start avvali.
 */
@Composable
fun RoomScreen(roomId: String, me: Profile, onExit: (next: Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val room by produceState<Map<String, Any?>?>(null, roomId) {
        val reg = Repo.rooms.document(roomId).addSnapshotListener { s, _ -> value = s?.data }
        awaitDispose { reg.remove() }
    }
    val msgs by produceState(emptyList<Msg>(), roomId) {
        val reg = Repo.messagesQuery(roomId).addSnapshotListener { s, _ ->
            value = s?.documents?.map {
                Msg(it.id, it.getString("from") ?: "", it.getString("text") ?: "", it.getLong("cts") ?: 0L)
            } ?: value
        }
        awaitDispose { reg.remove() }
    }

    val r = room
    val isFriend = r?.get("isFriend") == true || roomId.startsWith("f_")
    @Suppress("UNCHECKED_CAST")
    val members = (r?.get("members") as? List<String>) ?: emptyList()
    val otherUid = members.firstOrNull { it != me.uid } ?: ""
    @Suppress("UNCHECKED_CAST")
    val peer: Peer? = ((r?.get("info") as? Map<String, Any?>)?.get(otherUid) as? Map<String, Any?>)?.let {
        Peer(
            uid = otherUid,
            name = it["name"] as? String ?: "Stranger",
            age = (it["age"] as? Number)?.toInt() ?: 18,
            interests = (it["interests"] as? List<String>) ?: emptyList(),
        )
    }
    val leftBy = r?.get("leftBy") as? String
    val strangerLeft = !isFriend && leftBy != null && leftBy != me.uid
    @Suppress("UNCHECKED_CAST")
    val friendMap = (r?.get("friend") as? Map<String, Any?>) ?: emptyMap()
    val iWant = friendMap[me.uid] == true
    val theyWant = otherUid.isNotEmpty() && friendMap[otherUid] == true
    var savedFriend by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    // Iddaru "Add friend" nokkithe -> friends list lo save
    LaunchedEffect(iWant, theyWant, peer) {
        if (!isFriend && iWant && theyWant && peer != null && !savedFriend) {
            runCatching { Repo.saveFriend(me, peer) }.onSuccess {
                savedFriend = true
                vibrate(ctx, 120)
                toast = "🎉 You're now friends! Friends tab lo untaru."
            }
        }
    }
    LaunchedEffect(strangerLeft) { if (strangerLeft) vibrate(ctx, 150) }

    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size - 1) }

    fun exit(next: Boolean) {
        scope.launch {
            if (!isFriend) Repo.leave(roomId)
            onExit(next)
        }
    }

    BackHandler {
        if (isFriend || strangerLeft) exit(false) else confirmLeave = true
    }

    Column(Modifier.fillMaxSize().background(C.bg).imePadding()) {
        // ---- Top bar ----
        Row(
            Modifier.fillMaxWidth().background(C.card).statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton({ if (isFriend || strangerLeft) exit(false) else confirmLeave = true }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.text)
            }
            Avatar(peer?.name ?: "?", 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(peer?.name ?: "Connecting…", color = C.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val common = peer?.interests?.filter { it in me.interests } ?: emptyList()
                Text(
                    when {
                        peer == null -> ""
                        isFriend -> "Friend • ${peer.age}"
                        common.isNotEmpty() -> "${peer.age} • both like " + common.joinToString(" ") { Options.emoji(it) }
                        else -> "${peer.age} • " + peer.interests.joinToString(" ") { Options.emoji(it) }
                    },
                    color = C.dim, fontSize = 13.sp,
                )
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "Menu", tint = C.text) }
                DropdownMenu(menu, { menu = false }, containerColor = C.card2) {
                    DropdownMenuItem(text = { Text("🚫  Block") }, onClick = {
                        menu = false
                        scope.launch {
                            runCatching { Repo.block(otherUid) }
                            exit(false)
                        }
                    })
                    DropdownMenuItem(text = { Text("🚩  Report") }, onClick = { menu = false; reportOpen = true })
                    if (isFriend) {
                        DropdownMenuItem(text = { Text("➖  Remove friend") }, onClick = {
                            menu = false
                            scope.launch { runCatching { Repo.removeFriend(otherUid) }; exit(false) }
                        })
                    }
                }
            }
        }

        // ---- Stranger actions ----
        if (!isFriend) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionChip(
                    when {
                        savedFriend -> "✅ Friends"
                        iWant && !theyWant -> "⏳ Request sent"
                        theyWant && !iWant -> "➕ Accept friend"
                        else -> "➕ Add friend"
                    },
                    highlight = theyWant && !iWant,
                    modifier = Modifier.weight(1f),
                    enabled = !iWant && !strangerLeft && peer != null,
                ) { scope.launch { runCatching { Repo.wantFriend(roomId) } } }
                ActionChip("⏭ Next", highlight = strangerLeft, modifier = Modifier.weight(1f)) { exit(true) }
            }
        }

        // ---- Messages ----
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            state = list,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    if (isFriend) "Friends chat 💬" else
                        "🫨 Matched! Say hi 👋\nNumbers, Insta IDs & links hide avutayi – friends ayyaka share cheyyochu.",
                    color = C.dim, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                )
            }
            items(msgs, key = { it.id }) { m -> Bubble(m, mine = m.from == me.uid) }
            if (theyWant && !iWant && !savedFriend) {
                item { SystemNote("${peer?.name ?: "Stranger"} wants to be friends! ➕ Accept nokkandi") }
            }
            if (strangerLeft) {
                item { SystemNote("${peer?.name ?: "Stranger"} left the chat 👋  ⏭ Next nokki kotha vaallani kalavandi") }
            }
            toast?.let { t -> item { SystemNote(t) } }
        }

        // ---- Input ----
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 500) input = it },
                placeholder = { Text(if (strangerLeft) "Chat ended" else "Message…", color = C.dim) },
                enabled = !strangerLeft && peer != null,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.pink, unfocusedBorderColor = C.line,
                    focusedContainerColor = C.card, unfocusedContainerColor = C.card,
                    disabledContainerColor = C.card, disabledBorderColor = C.line, cursorColor = C.pink,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            val canSend = input.isNotBlank() && !strangerLeft && peer != null
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (canSend) C.grad else androidx.compose.ui.graphics.SolidColor(C.card2))
                    .clickable(enabled = canSend) {
                        val raw = input.trim()
                        val clean = Filter.clean(raw, stranger = !isFriend)
                        if (!isFriend && Filter.hadPrivateInfo(raw)) {
                            toast = "📵 Strangers tho numbers / IDs share cheyyakandi. Friends ayyaka try cheyyandi."
                        }
                        input = ""
                        scope.launch { runCatching { Repo.send(roomId, clean) } }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White) }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave chat?") },
            text = { Text("Ee stranger tho chat close avutundi. Friends kakapothe malli kalavalekaporu.") },
            confirmButton = { TextButton({ confirmLeave = false; exit(false) }) { Text("Leave", color = C.red) } },
            dismissButton = { TextButton({ confirmLeave = false }) { Text("Stay", color = C.text) } },
            containerColor = C.card,
        )
    }

    if (reportOpen) {
        val reasons = listOf("Abuse / bad words", "Sexual / nudity", "Spam / scam", "Under 18", "Other")
        var pick by remember { mutableStateOf(reasons[0]) }
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("Report ${peer?.name ?: "user"}") },
            text = {
                Column {
                    reasons.forEach { rs ->
                        Row(
                            Modifier.fillMaxWidth().clickable { pick = rs },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(pick == rs, { pick = rs }, colors = RadioButtonDefaults.colors(selectedColor = C.pink))
                            Text(rs)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Report chesthe ee user automatic ga block avutaru.", color = C.dim, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton({
                    reportOpen = false
                    scope.launch {
                        runCatching { Repo.report(otherUid, roomId, pick) }
                        runCatching { Repo.block(otherUid) }
                        exit(false)
                    }
                }) { Text("Report & block", color = C.red) }
            },
            dismissButton = { TextButton({ reportOpen = false }) { Text("Cancel", color = C.text) } },
            containerColor = C.card,
        )
    }
}

@Composable
private fun Bubble(m: Msg, mine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp,
                    )
                )
                .background(if (mine) C.grad else androidx.compose.ui.graphics.SolidColor(C.card2))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) { Text(m.text, color = Color.White, fontSize = 16.sp) }
    }
}

@Composable
private fun SystemNote(t: String) {
    Text(
        t, color = C.orange, fontSize = 13.sp, textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.card)
            .padding(10.dp),
    )
}

@Composable
private fun ActionChip(
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(if (highlight) C.grad else androidx.compose.ui.graphics.SolidColor(C.card2))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled || highlight) Color.White else C.dim, fontWeight = FontWeight.SemiBold)
    }
}
