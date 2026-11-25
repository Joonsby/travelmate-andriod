package com.joonsby.travelmate.core.data

import com.joonsby.travelmate.core.model.AuthTokens
import com.joonsby.travelmate.core.model.SocialProvider
import com.joonsby.travelmate.core.network.ApiConfig
import com.joonsby.travelmate.core.network.AuthApi
import com.joonsby.travelmate.core.network.NetworkFactory
import com.joonsby.travelmate.core.network.OfflineStatus
import com.joonsby.travelmate.core.network.ResponseCache
import com.joonsby.travelmate.core.network.TokenRefresher
import com.joonsby.travelmate.core.network.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class SessionAndSocketTest {
    private val server = MockWebServer()
    private lateinit var storage: MemoryStorage
    private lateinit var config: ApiConfig
    private lateinit var refresher: TokenRefresher
    private lateinit var session: SessionManager

    private class MemoryStorage(var tokens: AuthTokens?) : TokenStorage {
        override fun load() = tokens
        override fun save(tokens: AuthTokens) { this.tokens = tokens }
        override fun clear() { tokens = null }
    }

    @Before
    fun setUp() {
        server.start()
        config = ApiConfig(server.url("/api").toString())
        storage = MemoryStorage(AuthTokens("a1", "r1"))
        val base = NetworkFactory.baseClient()
        refresher = TokenRefresher(storage, NetworkFactory.refreshCall(config, base))
        val cache = ResponseCache(Files.createTempDirectory("cache").toFile())
        val client = NetworkFactory.apiClient(base, storage, refresher, cache, OfflineStatus())
        val api = NetworkFactory.retrofit(config, client).create(AuthApi::class.java)
        session = SessionManager(api, storage, cache, refresher)
    }

    @After fun tearDown() = server.close()

    private val me = """{"id":"7","nickname":"여행자","providers":["kakao"]}"""

    @Test
    fun `저장된 토큰으로 복원한다`() = runBlocking {
        server.enqueue(MockResponse.Builder().body(me).build())
        withContext(Dispatchers.IO) { session.restore() }
        val state = session.state.value as SessionState.SignedIn
        assertEquals("7", state.user.id)
        assertEquals(listOf(SocialProvider.Kakao), state.user.linkedProviders)
    }

    @Test
    fun `액세스 토큰이 만료됐으면 갱신한 뒤 복원한다`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().body("""{"accessToken":"a2","refreshToken":"r2"}""").build())
        server.enqueue(MockResponse.Builder().body(me).build())
        withContext(Dispatchers.IO) { session.restore() }
        assertTrue(session.state.value is SessionState.SignedIn)
        assertEquals(AuthTokens("a2", "r2"), storage.tokens)
    }

    @Test
    fun `갱신도 만료면 로그아웃하고 안내한다`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().code(401).build())
        withContext(Dispatchers.IO) { session.restore() }
        assertEquals(SessionState.SignedOut, session.state.value)
        assertEquals(SessionManager.EXPIRED_MESSAGE, session.login.value.errorMessage)
        assertNull(storage.tokens)
    }

    @Test
    fun `연결 오류는 로그아웃하지 않고 복원 실패로 둔다`() = runBlocking {
        server.close()
        withContext(Dispatchers.IO) { session.restore() }
        assertTrue(session.state.value is SessionState.RestoreFailed)
        assertEquals(AuthTokens("a1", "r1"), storage.tokens)
    }

    @Test
    fun `카카오 로그인은 토큰을 서버에 보내고 로그인 상태로 바꾼다`() = runBlocking {
        server.enqueue(MockResponse.Builder().body("""{"accessToken":"a9","refreshToken":"r9","user":$me,"isNewUser":true}""").build())
        storage.tokens = null
        withContext(Dispatchers.IO) { session.login(SocialProvider.Kakao) { "kakao-token" } }
        assertEquals("""{"accessToken":"kakao-token"}""", server.takeRequest().body?.utf8())
        assertTrue(session.state.value is SessionState.SignedIn)
        assertEquals(AuthTokens("a9", "r9"), storage.tokens)
        assertNull(session.login.value.loggingInProvider)
    }

    @Test
    fun `사용자가 취소하면 오류를 보여 주지 않는다`() = runBlocking {
        session.login(SocialProvider.Google) { throw SocialLoginException.Cancelled() }
        assertNull(session.login.value.errorMessage)
    }

    @Test
    fun `실시간 연결은 인증을 보내고 ready 와 다른 사람의 여행 변경을 전달한다`() = runBlocking {
        val received = LinkedBlockingQueue<String>()
        server.enqueue(MockResponse.Builder().webSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                received.add(text)
                webSocket.send("""{"type":"ready"}""")
                webSocket.send("""{"type":"tripChanged","tripId":"3","scope":"itinerary","by":"me"}""")
                webSocket.send("""{"type":"tripChanged","tripId":"3","scope":"expenses","by":"friend"}""")
            }
        }).build())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val bus = TripChangeBus()
        val socket = ChatSocket(NetworkFactory.baseClient(), config, storage, refresher, bus, scope)
        try {
            val pending = scope.async { bus.changes.first() }
            val ready = scope.async { socket.ready.first() }
            socket.start("me")
            withTimeout(5_000) { ready.await() }
            val change = withTimeout(5_000) { pending.await() }
            assertEquals("""{"type":"auth","token":"a1"}""", received.poll(5, TimeUnit.SECONDS))
            // 내가 바꾼 것(by=me)은 건너뛰고 친구가 바꾼 것만 온다.
            assertEquals(TripChange("3", TripChange.Scope.Expenses), change)
        } finally {
            socket.stop()
            scope.cancel()
        }
    }

    @Test
    fun `여행 변경 신호가 화면과 관계있는지 판단한다`() {
        val change = TripChange("1", TripChange.Scope.Expenses)
        assertTrue(change.affects("1", setOf(TripChange.Scope.Expenses)))
        assertTrue(change.affects(null, setOf(TripChange.Scope.Expenses)))
        assertTrue(!change.affects("2", setOf(TripChange.Scope.Expenses)))
        assertTrue(!change.affects("1", setOf(TripChange.Scope.Itinerary)))
        assertTrue(TripChange.Everything.affects("9", setOf(TripChange.Scope.Members)))
    }
}
