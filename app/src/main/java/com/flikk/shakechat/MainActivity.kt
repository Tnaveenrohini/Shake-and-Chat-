package com.flikk.shakechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.flikk.shakechat.data.Profile
import com.flikk.shakechat.data.Repo
import com.flikk.shakechat.ui.AppTheme
import com.flikk.shakechat.ui.C
import com.flikk.shakechat.ui.HomeScreen
import com.flikk.shakechat.ui.LoginScreen
import com.flikk.shakechat.ui.ProfileSetup
import com.flikk.shakechat.ui.RoomScreen
import com.flikk.shakechat.ui.SetupNeeded
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Box(Modifier.fillMaxSize().background(C.bg)) {
                    if (App.firebaseReady) Root() else SetupNeeded()
                }
            }
        }
    }
}

private sealed interface Scr {
    data object Loading : Scr
    data object Login : Scr
    data class EditProfile(val first: Boolean) : Scr
    data object Home : Scr
    data class Room(val id: String) : Scr
}

@Composable
private fun Root() {
    var scr by remember { mutableStateOf<Scr>(Scr.Loading) }
    var me by remember { mutableStateOf<Profile?>(null) }
    var autoShake by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        if (Repo.uid == null) {
            scr = Scr.Login
        } else {
            me = runCatching { Repo.loadProfile() }.getOrNull()
            scr = if (me == null) Scr.EditProfile(first = true) else Scr.Home
        }
    }

    when (val s = scr) {
        Scr.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = C.pink)
        }

        Scr.Login -> LoginScreen(onDone = { scr = Scr.Loading; reload++ })

        is Scr.EditProfile -> {
            if (!s.first) BackHandler { scr = Scr.Home }
            ProfileSetup(existing = me) { p -> me = p; scr = Scr.Home }
        }

        Scr.Home -> {
            val p = me
            if (p == null) {
                scr = Scr.Loading; reload++
            } else {
                HomeScreen(
                    me = p,
                    autoShake = autoShake,
                    onMatch = { rid -> autoShake = false; scr = Scr.Room(rid) },
                    onOpenFriend = { f -> autoShake = false; scr = Scr.Room(f.roomId) },
                    onEditProfile = { scr = Scr.EditProfile(first = false) },
                    onLogout = { Repo.signOut(); me = null; scr = Scr.Login },
                    onProfileChanged = { me = it },
                )
            }
        }

        is Scr.Room -> {
            val p = me
            if (p != null) {
                RoomScreen(roomId = s.id, me = p) { next ->
                    // Block chesi unte kotha list tho match avvali
                    scope.launch {
                        me = runCatching { Repo.loadProfile() }.getOrNull() ?: p
                        autoShake = next
                        scr = Scr.Home
                    }
                }
            }
        }
    }
}
