package com.joonsby.travelmate.spike

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Phase 0 실험 세 가지를 고르는 첫 화면. */
class SpikeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var screen by rememberSaveable { mutableStateOf("home") }
                    BackHandler(enabled = screen != "home") { screen = "home" }
                    Column(Modifier.safeDrawingPadding()) {
                        when (screen) {
                            "sticker" -> StickerLab()
                            "toss" -> TossLab()
                            "call" -> CallLab()
                            else -> Home { screen = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Home(open: (String) -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Phase 0 실험", style = MaterialTheme.typography.headlineSmall)
        Text("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Button(onClick = { open("sticker") }, Modifier.fillMaxWidth()) { Text("1. HEIC·Lottie 스티커") }
        Button(onClick = { open("toss") }, Modifier.fillMaxWidth()) { Text("2. 토스 송금 링크") }
        Button(onClick = { open("call") }, Modifier.fillMaxWidth()) { Text("3. LiveKit 그룹 통화") }
    }
}
