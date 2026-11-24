package com.flikk.shakechat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flikk.shakechat.data.Options
import com.flikk.shakechat.data.Profile
import com.flikk.shakechat.data.Repo
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetup(existing: Profile?, onSaved: (Profile) -> Unit) {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var birth by remember { mutableStateOf(existing?.birthYear?.toString() ?: "") }
    var gender by remember { mutableStateOf(existing?.gender ?: "") }
    var lang by remember { mutableStateOf(existing?.lang ?: "Telugu") }
    val interests = remember { mutableStateListOf<String>().apply { existing?.interests?.let { addAll(it) } } }
    var agree by remember { mutableStateOf(existing != null) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val by = birth.toIntOrNull()
    val age = by?.let { year - it }
    val ageOk = age != null && age in 18..100

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
    ) {
        Text(if (existing == null) "Set up your profile" else "Edit profile",
            color = C.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Strangers ki mee nickname, age, interests matrame kanipistayi.", color = C.dim, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name, onValueChange = { if (it.length <= 20) name = it },
            label = { Text("Nickname") }, singleLine = true,
            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = birth, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) birth = it },
            label = { Text("Birth year (e.g. 2001)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            supportingText = {
                if (birth.length == 4 && !ageOk) Text("Shake and Chat 18+ vaallaki matrame 🔞", color = C.red)
            },
        )

        Section("Gender")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Options.genders.forEach { g -> Pill(g, gender == g) { gender = g } }
        }

        Section("Chat language")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Options.languages.forEach { l -> Pill(l, lang == l) { lang = l } }
        }

        Section("Interests (1–5)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Options.interests.forEach { (e, n) ->
                val sel = n in interests
                Pill("$e $n", sel) {
                    if (sel) interests.remove(n) else if (interests.size < 5) interests.add(n)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().clickable { agree = !agree },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agree, onCheckedChange = { agree = it },
                colors = CheckboxDefaults.colors(checkedColor = C.pink),
            )
            Text(
                "I am 18+ and I'll follow the community rules (no abuse, no nudity, no spam).",
                color = C.dim, fontSize = 13.sp,
            )
        }

        err?.let { Text(it, color = C.red, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))

        val ok = name.isNotBlank() && ageOk && gender.isNotBlank() && interests.isNotEmpty() && agree
        GradButton(if (busy) "Saving…" else "Let's shake 🫨", enabled = ok && !busy) {
            val uid = Repo.uid ?: return@GradButton
            val p = Profile(
                uid = uid, name = name.trim(), birthYear = by!!, gender = gender, lang = lang,
                interests = interests.toList(), blocked = existing?.blocked ?: emptyList(),
            )
            busy = true; err = null
            scope.launch {
                try {
                    Repo.saveProfile(p); onSaved(p)
                } catch (e: Exception) {
                    err = friendlyError(e)
                } finally {
                    busy = false
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Section(t: String) {
    Spacer(Modifier.height(18.dp))
    Text(t, color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(10.dp))
}
