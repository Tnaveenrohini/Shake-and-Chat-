package com.shakeandchat.app

import android.os.Bundle
import android.hardware.*
import android.content.Context
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
 private lateinit var manager: SensorManager
 private var lastShake = 0L
 private var shakes by mutableIntStateOf(0)
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
  setContent { MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFFB9A2FF), background=Color(0xFF101326), surface=Color(0xFF20243C))) { ShakeApp(shakes) } }
 }
 override fun onResume() { super.onResume(); manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { manager.registerListener(this,it,SensorManager.SENSOR_DELAY_UI) } }
 override fun onPause() { manager.unregisterListener(this); super.onPause() }
 override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
 override fun onSensorChanged(event: SensorEvent) {
  val (x,y,z)=event.values
  val now=SystemClock.elapsedRealtime()
  if(sqrt(x*x+y*y+z*z)>24f && now-lastShake>1500) { lastShake=now; shakes++ }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShakeApp(shakes:Int) {
 var screen by remember { mutableStateOf("home") }
 var language by remember { mutableStateOf("Telugu") }
 var expanded by remember { mutableStateOf(false) }
 var draft by remember { mutableStateOf("") }
 var notice by remember { mutableStateOf("") }
 var partner by remember { mutableIntStateOf(0) }
 val messages=remember { mutableStateListOf<Pair<Boolean,String>>() }
 fun start() { notice=""; messages.clear(); draft=""; screen="search" }
 LaunchedEffect(shakes) { if(shakes>0 && screen=="home") start() }
 LaunchedEffect(screen) {
  if(screen=="search") { delay(2200); partner++; screen="match" }
 }
 Scaffold(topBar={ TopAppBar(title={Text("Shake & Chat")}, actions={Text("V1 DEMO",modifier=Modifier.padding(16.dp),color=Color(0xFFB9A2FF))}) }) { padding ->
 Column(Modifier.fillMaxSize().padding(padding).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Offline prototype • All partners are simulated",style=MaterialTheme.typography.labelMedium)
  when(screen) {
   "home","search" -> {
    Spacer(Modifier.weight(1f))
    Radar(screen=="search")
    Text(if(screen=="search") "Finding demo partner…" else "Shake to connect",fontSize=26.sp)
    if(screen=="home") {
     Box { OutlinedButton(onClick={expanded=true}) { Text("Language: "+language) }
      DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { listOf("Telugu","Tamil","Hindi","English").forEach { lang -> DropdownMenuItem(text={Text(lang)},onClick={language=lang;expanded=false}) } }
     }
     Button(onClick={start()},modifier=Modifier.fillMaxWidth()){Text("Tap to Connect")}
    } else OutlinedButton(onClick={screen="home"}){Text("Cancel")}
    Spacer(Modifier.weight(1f))
   }
   "match" -> {
    Spacer(Modifier.weight(1f))
    Text("✦",fontSize=80.sp,color=MaterialTheme.colorScheme.primary)
    Text("Demo partner "+partner,fontSize=28.sp)
    Text(language+" • Simulated match")
    Button(onClick={messages.add(false to "Hello! This is a simulated chat.");screen="chat"},modifier=Modifier.fillMaxWidth()){Text("Start chat")}
    OutlinedButton(onClick={start()}){Text("Skip")}
    TextButton(onClick={screen="home"}){Text("Back")}
    Spacer(Modifier.weight(1f))
   }
   "chat" -> {
    Text("Demo partner "+partner)
    LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     items(messages) { msg -> Box(Modifier.fillMaxWidth(),contentAlignment=if(msg.first) Alignment.CenterEnd else Alignment.CenterStart) {
      Surface(shape=RoundedCornerShape(16.dp),color=if(msg.first) Color(0xFF5940A0) else MaterialTheme.colorScheme.surface) { Text(msg.second,Modifier.padding(12.dp)) }
     } }
    }
    Row(verticalAlignment=Alignment.CenterVertically) {
     OutlinedTextField(value=draft,onValueChange={draft=it.take(500)},modifier=Modifier.weight(1f),placeholder={Text("Message")})
     TextButton(enabled=draft.isNotBlank(),onClick={messages.add(true to draft.trim());draft="";messages.add(false to "Demo reply: Thanks for your message!")}){Text("Send")}
    }
    Row { TextButton(onClick={start()}){Text("Next")};TextButton(onClick={messages.clear();screen="home"}){Text("End")};TextButton(onClick={messages.clear();notice="Demo partner blocked locally.";screen="home"}){Text("Block")} }
   }
  }
  if(notice.isNotEmpty()) Text(notice)
 }
 }
}

@Composable
fun Radar(searching:Boolean) {
 val transition=rememberInfiniteTransition(label="radar")
 val pulse by transition.animateFloat(initialValue=0f,targetValue=1f,animationSpec=infiniteRepeatable(tween(1600,easing=LinearEasing)),label="pulse")
 Canvas(Modifier.size(240.dp)) {
  val r=size.minDimension/2
  for(i in 1..4) drawCircle(Color(0xFF8B5CF6).copy(alpha=0.45f),radius=r*i/4,style=Stroke(2.dp.toPx()))
  if(searching) drawCircle(Color(0xFFB9A2FF).copy(alpha=1f-pulse),radius=r*pulse,style=Stroke(5.dp.toPx()))
  drawCircle(Color(0xFFB9A2FF),radius=12.dp.toPx())
 }
}
