package com.joonsby.travelmate.core.network

import com.joonsby.travelmate.core.model.AuthTokens
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/** 이 헤더가 붙은 요청(로그인·갱신)에는 토큰을 붙이지 않는다. 서버로는 보내지 않는다. */
const val NO_AUTH_HEADER = "X-TM-No-Auth: true"
private const val NO_AUTH_NAME = "X-TM-No-Auth"

/** 토큰 보관소. 실제 구현은 Keystore 로 암호화해 파일에 둔다. */
interface TokenStorage {
    fun load(): AuthTokens?
    fun save(tokens: AuthTokens)
    fun clear()
}

/**
 * 액세스 토큰 갱신. 리프레시 토큰은 쓸 때마다 바뀌므로(회전) 동시에 하나만 돌게 한다.
 * 기다리는 동안 다른 요청이 이미 갱신했으면 그 토큰을 그대로 쓴다.
 */
class TokenRefresher(
    private val storage: TokenStorage,
    private val refreshCall: suspend (refreshToken: String) -> AuthTokens,
) {
    private val mutex = Mutex()

    /** 갱신까지 401 이면 부른다 (로그아웃 처리) */
    @Volatile var onExpired: (() -> Unit)? = null

    /**
     * failedAccessToken 으로 401 을 받았을 때 새 액세스 토큰을 돌려준다.
     * 로그아웃됐거나 갱신도 401 이면 null. 연결 오류는 [IOException] 으로 던진다.
     */
    suspend fun refresh(failedAccessToken: String?): String? = mutex.withLock {
        val current = storage.load() ?: return null
        if (failedAccessToken != null && current.accessToken != failedAccessToken) return current.accessToken
        val refreshed = try {
            refreshCall(current.refreshToken)
        } catch (e: ApiError.Unauthorized) {
            // 그사이 다른 계정으로 바뀌지 않았을 때만 지운다.
            if (storage.load()?.refreshToken == current.refreshToken) {
                storage.clear()
                onExpired?.invoke()
            }
            return null
        } catch (e: ApiError.Transport) {
            throw (e.cause as? IOException) ?: IOException(e)
        } catch (e: ApiError) {
            throw IOException(e.userMessage, e)
        }
        // 갱신하는 동안 로그아웃·계정 전환이 있었으면 새 토큰을 저장하지 않는다.
        if (storage.load()?.refreshToken != current.refreshToken) return null
        storage.save(refreshed)
        refreshed.accessToken
    }
}

/** 요청에 `Authorization: Bearer <access>` 를 붙인다. */
class AuthInterceptor(private val storage: TokenStorage?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(NO_AUTH_NAME) != null) {
            return chain.proceed(request.newBuilder().removeHeader(NO_AUTH_NAME).build())
        }
        if (request.header("Authorization") != null) return chain.proceed(request)
        val token = storage?.load()?.accessToken ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/** 401 을 받으면 한 번만 토큰을 갱신해 다시 보낸다. */
class TokenAuthenticator(private val refresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization") ?: return null
        if (response.priorResponse != null) return null
        val failed = sent.removePrefix("Bearer ").trim()
        val token = runBlocking { refresher.refresh(failed) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $token").build()
    }
}
