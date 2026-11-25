package com.joonsby.travelmate.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * 오프라인 보기: 성공한 조회(GET) 응답을 저장해 두고, 인터넷이 안 되거나 서버가 잠시 멈췄을 때 대신 돌려준다.
 * 로그인한 사람마다 내용이 다르므로 로그인·로그아웃·탈퇴할 때 [clear] 한다.
 */
class ResponseCache(private val directory: File) {
    fun store(url: String, body: ByteArray) {
        if (body.isEmpty()) return
        runCatching {
            directory.mkdirs()
            val target = file(url)
            val temp = File(directory, target.name + ".tmp")
            temp.writeBytes(body)
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
        }
    }

    fun load(url: String): ByteArray? = runCatching { file(url).takeIf(File::exists)?.readBytes() }.getOrNull()

    fun clear() {
        directory.deleteRecursively()
    }

    private fun file(url: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.json")
    }

    companion object {
        /** 조회 요청만 저장한다. 파일(사진·동영상)은 크고 주소가 매번 바뀌어 저장하지 않는다. */
        fun isCacheable(request: Request): Boolean =
            request.method == "GET" && !request.url.encodedPath.contains("/files/")
    }
}

/** 지금 화면이 저장해 둔 내용(오프라인)을 보여 주고 있는지. 위쪽 안내에 쓴다. */
class OfflineStatus {
    private val state = MutableStateFlow(false)
    val isShowingSavedData: StateFlow<Boolean> = state.asStateFlow()

    fun update(showingSavedData: Boolean) {
        state.value = showingSavedData
    }
}

class OfflineCacheInterceptor(
    private val cache: ResponseCache,
    private val status: OfflineStatus,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!ResponseCache.isCacheable(request)) return chain.proceed(request)
        val url = request.url.toString()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            return saved(request, url) ?: throw e
        }
        if (response.code in 502..504) {
            val saved = saved(request, url) ?: return response
            response.close()
            return saved
        }
        if (!response.isSuccessful) return response
        val contentType = response.body.contentType()
        val bytes = response.body.bytes()
        cache.store(url, bytes)
        status.update(false)
        return response.newBuilder().body(bytes.toResponseBody(contentType)).build()
    }

    private fun saved(request: Request, url: String): Response? {
        val bytes = cache.load(url) ?: return null
        status.update(true)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("Saved")
            .header(SAVED_HEADER, "1")
            .body(bytes.toResponseBody("application/json".toMediaType()))
            .build()
    }

    companion object {
        const val SAVED_HEADER = "X-TM-Saved-Response"
    }
}
