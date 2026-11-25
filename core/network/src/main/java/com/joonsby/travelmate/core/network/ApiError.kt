package com.joonsby.travelmate.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** 서버와 주고받는 JSON 설정. 모르는 키는 무시하고(서버가 필드를 늘려도 깨지지 않게), null 은 보내지 않는다. */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/** 네트워크 계층의 모든 실패. 화면에는 [userMessage] 를 그대로 보여 줄 수 있다. */
sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** 인터넷 연결 실패, 타임아웃 */
    class Transport(cause: IOException) : ApiError("네트워크 연결을 확인해 주세요.", cause)

    /** 401: 로그인이 필요하거나 만료됨 (토큰 갱신까지 실패한 경우) */
    class Unauthorized : ApiError("로그인이 필요합니다.")

    /** 그 밖의 2xx 가 아닌 응답. 서버가 보낸 `{ error: { message, details } }` 를 담는다. */
    class Server(
        val statusCode: Int,
        val serverMessage: String?,
        val fieldErrors: Map<String, String>,
    ) : ApiError(serverMessage ?: "서버 오류가 발생했습니다. ($statusCode)")

    /** 응답 JSON 을 읽지 못함 */
    class Decoding(cause: Throwable) : ApiError("요청을 처리하는 중 문제가 발생했습니다.", cause)

    val userMessage: String get() = message ?: "요청을 처리하는 중 문제가 발생했습니다."

    companion object {
        /** 서버 오류 본문 `{ "error": { "message": "...", "details": { "필드": "..." } } }` 를 읽는다. */
        fun fromResponse(statusCode: Int, body: String?): ApiError {
            if (statusCode == 401) return Unauthorized()
            val error = runCatching { ApiJson.parseToJsonElement(body.orEmpty()) as? JsonObject }.getOrNull()
                ?.get("error") as? JsonObject
            val message = (error?.get("message") as? JsonPrimitive)?.takeIf { it.isString }?.content
            val details = (error?.get("details") as? JsonObject)
                ?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.takeIf { it.isString }?.let { key to it.content } }
                ?.toMap()
                .orEmpty()
            return Server(statusCode, message, details)
        }
    }
}

/** Retrofit 호출을 감싸 실패를 [ApiError] 로 바꾼다. 작업 취소는 그대로 던진다. */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: ApiError) {
    throw e
} catch (e: HttpException) {
    throw ApiError.fromResponse(e.code(), e.response()?.errorBody()?.string())
} catch (e: SerializationException) {
    throw ApiError.Decoding(e)
} catch (e: IOException) {
    throw ApiError.Transport(e)
}
