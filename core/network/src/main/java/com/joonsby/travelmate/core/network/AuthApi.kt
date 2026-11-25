package com.joonsby.travelmate.core.network

import com.joonsby.travelmate.core.model.AuthSession
import com.joonsby.travelmate.core.model.AuthTokens
import com.joonsby.travelmate.core.model.AuthUser
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/** 서버 인증(`/api/auth/...`)과 내 정보(`/api/me`) */
interface AuthApi {
    @Headers(NO_AUTH_HEADER)
    @POST("auth/kakao")
    suspend fun loginKakao(@Body body: AccessTokenBody): AuthSession

    @Headers(NO_AUTH_HEADER)
    @POST("auth/google")
    suspend fun loginGoogle(@Body body: IdTokenBody): AuthSession

    /** 스토어 심사용 로그인. 서버에 정해 둔 심사 계정만 들어올 수 있다. */
    @Headers(NO_AUTH_HEADER)
    @POST("auth/review")
    suspend fun reviewLogin(@Body body: ReviewLoginBody): AuthSession

    @Headers(NO_AUTH_HEADER)
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshTokenBody): AuthTokens

    @Headers(NO_AUTH_HEADER)
    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshTokenBody): Response<Unit>

    @GET("me")
    suspend fun me(): AuthUser
}

@Serializable data class AccessTokenBody(val accessToken: String)
@Serializable data class IdTokenBody(val idToken: String)
@Serializable data class RefreshTokenBody(val refreshToken: String)
@Serializable data class ReviewLoginBody(val email: String, val password: String)
