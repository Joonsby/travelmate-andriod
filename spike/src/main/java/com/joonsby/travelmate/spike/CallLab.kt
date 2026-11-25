package com.joonsby.travelmate.spike

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.livekit.android.LiveKit
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * 실험 3. Android 가 iOS 와 같은 LiveKit 방(trip-<id>)에 들어가 서로 들리는지.
 * 방법 A: 앱 로그인 토큰 + 여행 id 로 서버 /trips/:id/call/join 을 불러 LiveKit 주소·토큰을 받는다 (iOS 와 같은 경로)
 * 방법 B: LiveKit 주소·토큰을 직접 붙여 넣는다
 * 토큰은 화면에만 두고 저장·로그하지 않는다.
 */
@Composable
fun CallLab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apiBase by remember { mutableStateOf(DEFAULT_API_BASE) }
    var accessToken by remember { mutableStateOf("") }
    var tripId by remember { mutableStateOf("") }
    var lkUrl by remember { mutableStateOf("") }
    var lkToken by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf("") }
    var micOn by remember { mutableStateOf(true) }
    val room = remember { mutableStateOf<Room?>(null) }
    val eventsJob = remember { mutableStateOf<Job?>(null) }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        log = if (granted) "마이크 권한 허용" else "마이크 권한 거부 — 듣기만 됩니다"
    }

    fun refreshParticipants() {
        val r = room.value ?: return
        val me = r.localParticipant
        participants = buildString {
            appendLine("나: ${me.identity?.value} ${if (me.isSpeaking) "🔊" else ""}")
            r.remoteParticipants.values.forEach { appendLine("${it.name ?: it.identity?.value} ${if (it.isSpeaking) "🔊" else ""}") }
        }
    }

    fun connect(url: String, token: String) = scope.launch {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        runCatching {
            val r = LiveKit.create(context.applicationContext)
            room.value = r
            eventsJob.value = launch { r.events.collect { refreshParticipants() } }
            r.connect(url, token)
            runCatching { r.localParticipant.setMicrophoneEnabled(true) }
                .onFailure { log = "마이크 켜기 실패 (듣기만): ${it.message}" }
            refreshParticipants()
            log = "접속됨: ${r.name}"
        }.onFailure { log = "접속 실패: ${it.message}" }
    }

    fun joinViaApi() = scope.launch {
        log = "서버에 통화 참여 요청…"
        runCatching {
            withContext(Dispatchers.IO) { callApi(apiBase, accessToken, tripId, "join") }
        }.onSuccess { json -> connect(json.getString("url"), json.getString("token")) }
            .onFailure { log = "join 실패: ${it.message}" }
    }

    fun hangUp() = scope.launch {
        eventsJob.value?.cancel()
        room.value?.disconnect()
        room.value?.release()
        room.value = null
        participants = ""
        if (accessToken.isNotBlank() && tripId.isNotBlank()) {
            runCatching { withContext(Dispatchers.IO) { callApi(apiBase, accessToken, tripId, "leave") } }
        }
        log = "나감"
    }

    DisposableEffect(Unit) {
        onDispose {
            eventsJob.value?.cancel()
            room.value?.disconnect()
            room.value?.release()
        }
    }

    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("3. LiveKit 그룹 통화", style = MaterialTheme.typography.titleLarge)
        Text("iOS 기기로 같은 여행 채팅방 통화에 먼저 들어가 둔 뒤 시험하세요.", style = MaterialTheme.typography.bodySmall)

        Text("방법 A — 서버 경유 (iOS 와 같은 경로)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(apiBase, { apiBase = it }, Modifier.fillMaxWidth(), label = { Text("API 주소") }, singleLine = true)
        OutlinedTextField(accessToken, { accessToken = it.trim() }, Modifier.fillMaxWidth(), label = { Text("앱 access token (직접 붙여 넣기)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(tripId, { tripId = it.trim() }, Modifier.fillMaxWidth(), label = { Text("여행 id") }, singleLine = true)
        Button(onClick = { joinViaApi() }, Modifier.fillMaxWidth(), enabled = room.value == null && accessToken.isNotBlank() && tripId.isNotBlank()) {
            Text("서버로 참여")
        }

        Text("방법 B — LiveKit 직접", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(lkUrl, { lkUrl = it.trim() }, Modifier.fillMaxWidth(), label = { Text("LiveKit 주소 (wss://…)") }, singleLine = true)
        OutlinedTextField(lkToken, { lkToken = it.trim() }, Modifier.fillMaxWidth(), label = { Text("LiveKit 토큰") },
            singleLine = true, visualTransformation = PasswordVisualTransformation())
        Button(onClick = { connect(lkUrl, lkToken) }, Modifier.fillMaxWidth(), enabled = room.value == null && lkUrl.isNotBlank() && lkToken.isNotBlank()) {
            Text("직접 참여")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                micOn = !micOn
                scope.launch { room.value?.localParticipant?.setMicrophoneEnabled(micOn) }
            }, enabled = room.value != null) { Text(if (micOn) "마이크 끄기" else "마이크 켜기") }
            Button(onClick = { hangUp() }, enabled = room.value != null) { Text("나가기") }
        }
        Text(log, fontFamily = FontFamily.Monospace)
        Text(participants, fontFamily = FontFamily.Monospace)
    }
}

private fun callApi(base: String, accessToken: String, tripId: String, action: String): JSONObject {
    val request = Request.Builder()
        .url("${base.trimEnd('/')}/trips/$tripId/call/$action")
        .header("Authorization", "Bearer $accessToken")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .build()
    return Http.client.newCall(request).execute().use { response ->
        val body = response.body.string()
        check(response.isSuccessful) { "HTTP ${response.code} ${runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrDefault("")}" }
        JSONObject(body)
    }
}
