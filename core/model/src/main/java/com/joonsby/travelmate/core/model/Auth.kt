package com.joonsby.travelmate.core.model

import kotlinx.serialization.Serializable

/** 서비스 자체 토큰 쌍. 액세스 토큰(JWT)은 API 호출에, 리프레시 토큰은 재발급에 쓴다. */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/** 로그인 API 응답: 서비스 토큰과 회원 정보 */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser,
    val isNewUser: Boolean = false,
) {
    val tokens: AuthTokens get() = AuthTokens(accessToken, refreshToken)
}

@Serializable
data class AuthUser(
    val id: String,
    val nickname: String,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val statusMessage: String? = null,
    /** 연결된 로그인 제공자 ("kakao", "google", "apple") */
    val providers: List<String>? = null,
    /** 가입일 (ISO 8601) */
    val createdAt: String? = null,
) {
    val linkedProviders: List<SocialProvider>
        get() = providers.orEmpty().mapNotNull(SocialProvider::fromServerName)
}

/** 소셜 로그인 제공자. serverName 은 서버 경로 `/auth/{serverName}` 과 같다. */
enum class SocialProvider(val serverName: String, val displayName: String) {
    Kakao("kakao", "카카오"),
    Google("google", "구글"),
    Apple("apple", "Apple");

    companion object {
        fun fromServerName(name: String): SocialProvider? = entries.firstOrNull { it.serverName == name }
    }
}
