package com.joonsby.travelmate.core.network

import com.joonsby.travelmate.core.model.AuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthTest {
    private val server = MockWebServer()
    private val refreshCount = AtomicInteger()

    private class MemoryStorage(var tokens: AuthTokens?) : TokenStorage {
        override fun load() = tokens
        override fun save(tokens: AuthTokens) { this.tokens = tokens }
        override fun clear() { tokens = null }
    }

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private fun client(storage: TokenStorage, refresher: TokenRefresher) = NetworkFactory.apiClient(
        base = NetworkFactory.baseClient(),
        storage = storage,
        refresher = refresher,
        cache = ResponseCache(Files.createTempDirectory("cache").toFile()),
        offlineStatus = OfflineStatus(),
    )

    @Test
    fun `동시에 401 을 받아도 갱신은 한 번만 하고 모두 새 토큰으로 다시 보낸다`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/api/auth/refresh" -> {
                    refreshCount.incrementAndGet()
                    Thread.sleep(200)
                    MockResponse.Builder().body("""{"accessToken":"new","refreshToken":"r2"}""").build()
                }
                else -> if (request.headers["Authorization"] == "Bearer new") {
                    MockResponse.Builder().body("""{"ok":true}""").build()
                } else {
                    MockResponse.Builder().code(401).body("""{"error":{"message":"만료"}}""").build()
                }
            }
        }
        val config = ApiConfig(server.url("/api").toString())
        val storage = MemoryStorage(AuthTokens("old", "r1"))
        val refresher = TokenRefresher(storage, NetworkFactory.refreshCall(config, NetworkFactory.baseClient()))
        val http = client(storage, refresher)

        val codes = withContext(Dispatchers.IO) {
            (1..5).map { i ->
                async { http.newCall(Request.Builder().url(server.url("/api/trips/$i")).build()).execute().use { it.code } }
            }.awaitAll()
        }

        assertEquals(listOf(200, 200, 200, 200, 200), codes)
        assertEquals(1, refreshCount.get())
        assertEquals(AuthTokens("new", "r2"), storage.tokens)
    }

    @Test
    fun `갱신도 401 이면 토큰을 지우고 만료를 알린다`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse.Builder().code(401).build()
        }
        val config = ApiConfig(server.url("/api").toString())
        val storage = MemoryStorage(AuthTokens("old", "r1"))
        val refresher = TokenRefresher(storage, NetworkFactory.refreshCall(config, NetworkFactory.baseClient()))
        var expired = false
        refresher.onExpired = { expired = true }

        val code = withContext(Dispatchers.IO) {
            client(storage, refresher).newCall(Request.Builder().url(server.url("/api/me")).build()).execute().use { it.code }
        }

        assertEquals(401, code)
        assertNull(storage.tokens)
        assertTrue(expired)
    }

    @Test
    fun `로그인 요청에는 토큰을 붙이지 않고 표시 헤더도 서버로 보내지 않는다`() = runBlocking {
        server.enqueue(MockResponse.Builder().body("""{"accessToken":"a","refreshToken":"r","user":{"id":"1","nickname":"n"}}""").build())
        val config = ApiConfig(server.url("/api").toString())
        val storage = MemoryStorage(AuthTokens("old", "r1"))
        val refresher = TokenRefresher(storage) { error("부르면 안 됨") }
        val api = NetworkFactory.retrofit(config, client(storage, refresher)).create(AuthApi::class.java)

        val session = apiCall { api.loginKakao(AccessTokenBody("kakao-token")) }

        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
        assertNull(recorded.headers["X-TM-No-Auth"])
        assertEquals("1", session.user.id)
    }
}
