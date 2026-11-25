package com.joonsby.travelmate.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.joonsby.travelmate.core.data.SocialLoginException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 앱 모듈이 BuildConfig 로 채워 넣는 로그인 설정 */
data class AuthConfig(
    /** GCP 의 「웹 애플리케이션」 OAuth 클라이언트 ID. idToken 의 수신자(aud)가 되고 서버도 이 값을 허용한다. */
    val googleWebClientId: String,
)

/**
 * 카카오 로그인. 카카오톡이 있으면 카카오톡으로, 없으면 카카오계정(웹)으로 로그인해 카카오 액세스 토큰을 돌려준다.
 * 카카오톡에 로그인돼 있지 않은 등 카카오톡 로그인이 실패하면 카카오계정 로그인으로 한 번 더 시도한다.
 */
suspend fun kakaoAccessToken(context: Context): String {
    val client = UserApiClient.instance
    if (client.isKakaoTalkLoginAvailable(context)) {
        try {
            return kakaoLogin { callback -> client.loginWithKakaoTalk(context, callback = callback) }
        } catch (e: SocialLoginException.Cancelled) {
            throw e
        } catch (e: SocialLoginException) {
            // 카카오톡 로그인이 안 되면 카카오계정으로 넘어간다.
        }
    }
    return kakaoLogin { callback -> client.loginWithKakaoAccount(context, callback = callback) }
}

private suspend fun kakaoLogin(start: ((OAuthToken?, Throwable?) -> Unit) -> Unit): String =
    suspendCancellableCoroutine { continuation ->
        start { token, error ->
            if (!continuation.isActive) return@start
            when {
                error != null -> continuation.resumeWithException(mapKakaoError(error))
                token != null -> continuation.resume(token.accessToken)
                else -> continuation.resumeWithException(SocialLoginException.Failed("토큰이 비어 있습니다."))
            }
        }
    }

/** 사용자 취소(화면 닫기, 동의 화면에서 취소)와 실제 실패를 구분한다. */
internal fun mapKakaoError(error: Throwable): SocialLoginException = when {
    error is ClientError && error.reason == ClientErrorCause.Cancelled -> SocialLoginException.Cancelled()
    error is AuthError && error.reason == AuthErrorCause.AccessDenied -> SocialLoginException.Cancelled()
    else -> SocialLoginException.Failed(error.toString())
}

/** 카카오 계정과 앱의 연결을 끊는다 (회원 탈퇴). 실패해도 탈퇴는 끝난 것으로 본다. */
suspend fun kakaoUnlink() = suspendCancellableCoroutine { continuation ->
    UserApiClient.instance.unlink { if (continuation.isActive) continuation.resume(Unit) }
}

/** Google 로 로그인해 서버가 검증할 idToken 을 돌려준다. 계정 고르는 창은 Android Credential Manager 가 띄운다. */
suspend fun googleIdToken(context: Context, config: AuthConfig): String {
    val option = GetSignInWithGoogleOption.Builder(config.googleWebClientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val result = try {
        CredentialManager.create(context).getCredential(context, request)
    } catch (e: GetCredentialCancellationException) {
        throw SocialLoginException.Cancelled()
    } catch (e: GetCredentialException) {
        throw SocialLoginException.Failed("${e.type}: ${e.message}")
    }
    val credential = result.credential
    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw SocialLoginException.Failed("Google 로그인 결과가 아닙니다: ${credential.type}")
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
