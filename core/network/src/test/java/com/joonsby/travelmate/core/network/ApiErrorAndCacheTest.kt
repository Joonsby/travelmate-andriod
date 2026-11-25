package com.joonsby.travelmate.core.network

import com.joonsby.travelmate.core.model.AuthTokens
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

class ApiErrorAndCacheTest {
    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    @Test
    fun `서버 오류 본문에서 메시지와 필드별 오류를 읽는다`() {
        val error = ApiError.fromResponse(400, """{"error":{"message":"입력을 확인해 주세요.","details":{"email":"형식이 틀렸어요"}}}""")
        error as ApiError.Server
        assertEquals("입력을 확인해 주세요.", error.userMessage)
        assertEquals(mapOf("email" to "형식이 틀렸어요"), error.fieldErrors)
    }

    @Test
    fun `본문이 JSON 이 아니면 상태 코드로 안내한다`() {
        val error = ApiError.fromResponse(502, "<html>Bad gateway</html>") as ApiError.Server
        assertNull(error.serverMessage)
        assertEquals("서버 오류가 발생했습니다. (502)", error.userMessage)
        assertTrue(ApiError.fromResponse(401, null) is ApiError.Unauthorized)
    }

    @Test
    fun `서버가 멈추면 마지막으로 받은 조회 응답을 돌려주고 오프라인으로 표시한다`() {
        val status = OfflineStatus()
        val storage = object : TokenStorage {
            override fun load() = AuthTokens("a", "r")
            override fun save(tokens: AuthTokens) = Unit
            override fun clear() = Unit
        }
        val http = NetworkFactory.apiClient(
            NetworkFactory.baseClient(), storage, TokenRefresher(storage) { error("갱신 안 함") },
            ResponseCache(Files.createTempDirectory("cache").toFile()), status,
        )
        val request = Request.Builder().url(server.url("/api/trips")).build()

        server.enqueue(MockResponse.Builder().body("""[{"id":"1"}]""").build())
        assertEquals("""[{"id":"1"}]""", http.newCall(request).execute().use { it.body.string() })
        assertFalse(status.isShowingSavedData.value)

        server.enqueue(MockResponse.Builder().code(503).build())
        val saved = http.newCall(request).execute().use { it.code to it.body.string() }
        assertEquals(200 to """[{"id":"1"}]""", saved)
        assertTrue(status.isShowingSavedData.value)
    }

    @Test
    fun `상대 경로 파일 주소를 절대 주소로 바꾼다`() {
        val config = ApiConfig("https://api-travelmate.fit-plate.com/api")
        assertEquals("https://api-travelmate.fit-plate.com/api/files/a.jpg", config.resolve("/files/a.jpg"))
        assertEquals("https://x.com/a.jpg", config.resolve("https://x.com/a.jpg"))
        assertEquals("wss://api-travelmate.fit-plate.com/api/chat/ws", config.chatSocketUrl)
        assertNull(config.resolve(""))
    }
}
