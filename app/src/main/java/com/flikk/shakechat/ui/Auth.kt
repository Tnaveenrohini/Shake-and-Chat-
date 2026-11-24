package com.flikk.shakechat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flikk.shakechat.data.Repo
import kotlinx.coroutines.launch

@Composable
fun SetupNeeded() {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🫨", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Setup needed", color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Firebase config (google-services.json) ee build lo ledu.\n" +
                "Repo lo app/google-services.json add chesi malli build cheyyandi.",
            color = C.dim, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoginScreen(onDone: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var signUp by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("🫨", fontSize = 80.sp)
        Spacer(Modifier.height(8.dp))
        Text("Shake and Chat", color = C.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Shake your phone. Meet someone new.", color = C.dim, fontSize = 15.sp)
        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("Password (6+ letters)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        GradButton(
            if (busy) "Please wait…" else if (signUp) "Create account" else "Login",
            enabled = !busy && email.contains("@") && pass.length >= 6,
        ) {
            busy = true; msg = null
            scope.launch {
                try {
                    if (signUp) Repo.signUp(email, pass) else Repo.signIn(email, pass)
                    onDone()
                } catch (e: Exception) {
                    msg = friendlyError(e)
                } finally {
                    busy = false
                }
            }
        }

        msg?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = C.red, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { signUp = !signUp; msg = null }) {
            Text(
                if (signUp) "Already have an account? Login" else "New here? Create account",
                color = C.orange,
            )
        }
        if (!signUp) {
            TextButton(onClick = {
                scope.launch {
                    msg = try {
                        Repo.resetPassword(email); "Password reset link sent to your email 📩"
                    } catch (e: Exception) {
                        friendlyError(e)
                    }
                }
            }) { Text("Forgot password?", color = C.dim) }
        }
    }
}

fun friendlyError(e: Exception): String {
    val m = e.message ?: ""
    return when {
        "already in use" in m -> "Ee email tho already account undi. Login cheyyandi."
        "password is invalid" in m || "INVALID_LOGIN_CREDENTIALS" in m || "credential" in m ->
            "Email or password tappu."
        "badly formatted" in m -> "Email correct ga ledu."
        "PERMISSION_DENIED" in m ->
            "Firebase lo Firestore Rules publish avvaledu. Rules tab lo firestore.rules paste chesi Publish cheyyandi."
        "NOT_FOUND" in m || "does not exist" in m ->
            "Firestore Database inka create avvaledu. Firebase console lo Create database cheyyandi."
        "network" in m.lowercase() -> "Internet ledu. Malli try cheyyandi."
        else -> m.ifBlank { "Something went wrong" }
    }
}
