package com.joonsby.travelmate.core.network

import com.joonsby.travelmate.core.model.AuthTokens
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** OkHttp·Retrofit 조립. 앱은 DI 모듈에서, 테스트는 직접 부른다. */
object NetworkFactory {
    /** 토큰 없이 쓰는 기본 클라이언트 (갱신 요청, 실시간 연결의 바탕) */
    fun baseClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** 앱 API 용: 오프라인 저장 → 토큰 붙이기 → (401 이면) 갱신 후 재시도 */
    fun apiClient(
        base: OkHttpClient,
        storage: TokenStorage,
        refresher: TokenRefresher,
        cache: ResponseCache,
        offlineStatus: OfflineStatus,
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(OfflineCacheInterceptor(cache, offlineStatus))
        .addInterceptor(AuthInterceptor(storage))
        .authenticator(TokenAuthenticator(refresher))
        .build()

    fun retrofit(config: ApiConfig, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(config.retrofitBaseUrl)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()

    /** 토큰 갱신 요청. 이 경로에는 갱신기를 달지 않는다(갱신이 401 이면 그대로 실패). */
    fun refreshCall(config: ApiConfig, base: OkHttpClient): suspend (String) -> AuthTokens {
        val api = retrofit(config, base.newBuilder().addInterceptor(AuthInterceptor(null)).build())
            .create(AuthApi::class.java)
        return { refreshToken -> apiCall { api.refresh(RefreshTokenBody(refreshToken)) } }
    }
}
