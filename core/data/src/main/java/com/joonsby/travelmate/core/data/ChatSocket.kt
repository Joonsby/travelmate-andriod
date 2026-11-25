package com.joonsby.travelmate.core.data

import com.joonsby.travelmate.core.model.ChatSocketPacket
import com.joonsby.travelmate.core.network.ApiConfig
import com.joonsby.travelmate.core.network.ApiJson
import com.joonsby.travelmate.core.network.TokenRefresher
import com.joonsby.travelmate.core.network.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.min

/**
 * 실시간 연결 (/api/chat/ws). 받기 전용이고 보내기는 REST 로 한다.
 * 연결하면 10초 안에 {"type":"auth","token"} 을 보내야 하고, 성공하면 {"type":"ready"} 가 온다.
 * 끊기면 1초부터 두 배씩 최대 30초까지 늘려 가며 다시 잇는다.
 */
@Singleton
class ChatSocket @Inject constructor(
    @Named(BASE_CLIENT) baseClient: OkHttpClient,
    private val config: ApiConfig,
    private val tokenStorage: TokenStorage,
    private val refresher: TokenRefresher,
    private val tripChanges: TripChangeBus,
    @Named(APP_SCOPE) private val scope: CoroutineScope,
) {
    enum class Status { Disconnected, Connecting, Ready }

    // 서버가 30초마다 ping 을 보낸다. OkHttp 는 pong 을 자동으로 답한다.
    private val client = baseClient.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()

    private val _packets = MutableSharedFlow<ChatSocketPacket>(extraBufferCapacity = 256)
    /** ready 를 뺀 모든 패킷 (message, messageUpdated, pin, read, call, tripChanged …) */
    val packets: SharedFlow<ChatSocketPacket> = _packets.asSharedFlow()

    private val _ready = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    /** 연결(재연결)될 때마다. 받은 쪽은 놓친 메시지를 채운다. */
    val ready: SharedFlow<Unit> = _ready.asSharedFlow()

    private val _status = MutableStateFlow(Status.Disconnected)
    val status: StateFlow<Status> = _status.asStateFlow()

    private var job: Job? = null
    private var currentUserId: String? = null

    @Synchronized
    fun start(userId: String) {
        currentUserId = userId
        job?.cancel()
        job = scope.launch { run() }
    }

    /** 앱이 다시 앞으로 왔을 때. 대기 중인 재연결을 기다리지 않고 바로 잇는다. */
    @Synchronized
    fun reconnect() {
        val userId = currentUserId ?: return
        if (_status.value == Status.Ready) return
        start(userId)
    }

    @Synchronized
    fun stop() {
        currentUserId = null
        job?.cancel()
        job = null
        _status.value = Status.Disconnected
    }

    private suspend fun run() {
        var backoffMs = 1_000L
        var connectedBefore = false
        while (scope.isActive) {
            val token = tokenStorage.load()?.accessToken ?: break
            _status.value = Status.Connecting
            val closeCode = connectOnce(token) {
                backoffMs = 1_000L
                _status.value = Status.Ready
                _ready.tryEmit(Unit)
                // 다시 이어졌으면 그사이 놓친 변경이 있을 수 있으니 보이는 화면을 모두 다시 불러오게 한다.
                if (connectedBefore) tripChanges.post(TripChange.Everything)
                connectedBefore = true
            }
            _status.value = Status.Disconnected
            // 토큰이 만료돼 인증에 실패했으면 갱신한 뒤 다시 잇는다.
            if (closeCode == AUTH_FAILED_CODE) runCatching { refresher.refresh(token) }
            delay(backoffMs)
            backoffMs = min(backoffMs * 2, 30_000L)
        }
        _status.value = Status.Disconnected
    }

    /** 연결 하나가 끝날 때까지 기다린다. 닫힘 코드(모르면 null)를 돌려준다. */
    private suspend fun connectOnce(token: String, onReady: () -> Unit): Int? {
        val closed = Channel<Int?>(Channel.CONFLATED)
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val auth = buildJsonObject {
                    put("type", "auth")
                    put("token", token)
                }
                webSocket.send(auth.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val packet = runCatching { ApiJson.decodeFromString(ChatSocketPacket.serializer(), text) }.getOrNull() ?: return
                when (packet.type) {
                    "ready" -> onReady()
                    "tripChanged" -> {
                        // 내가 바꾼 것은 이미 내 화면에 반영돼 있다.
                        if (packet.tripId != null && packet.by != currentUserId) {
                            tripChanges.post(TripChange(packet.tripId, TripChange.Scope.fromServerName(packet.scope)))
                        }
                        _packets.tryEmit(packet)
                    }
                    else -> _packets.tryEmit(packet)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                closed.trySend(code)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                closed.trySend(code)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                closed.trySend(null)
            }
        }
        val socket = client.newWebSocket(Request.Builder().url(config.chatSocketUrl).build(), listener)
        return try {
            closed.receive()
        } finally {
            socket.cancel()
        }
    }

    companion object {
        const val AUTH_FAILED_CODE = 4001
    }
}
