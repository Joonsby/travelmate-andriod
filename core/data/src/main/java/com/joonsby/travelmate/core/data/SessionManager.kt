package com.joonsby.travelmate.core.data

import com.joonsby.travelmate.core.model.AuthSession
import com.joonsby.travelmate.core.model.AuthUser
import com.joonsby.travelmate.core.model.SocialProvider
import com.joonsby.travelmate.core.network.AccessTokenBody
import com.joonsby.travelmate.core.network.ApiError
import com.joonsby.travelmate.core.network.AuthApi
import com.joonsby.travelmate.core.network.IdTokenBody
import com.joonsby.travelmate.core.network.RefreshTokenBody
import com.joonsby.travelmate.core.network.ResponseCache
import com.joonsby.travelmate.core.network.ReviewLoginBody
import com.joonsby.travelmate.core.network.TokenRefresher
import com.joonsby.travelmate.core.network.TokenStorage
import com.joonsby.travelmate.core.network.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** 앱 전체의 로그인 상태. 최상위 화면이 이 상태에 따라 로그인 화면과 탭 화면을 바꿔 보여 준다. */
sealed interface SessionState {
    /** 앱 시작 직후 저장된 토큰을 확인하는 중 */
    data object Checking : SessionState
    data class RestoreFailed(val message: String) : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

/** 소셜 SDK 로그인 실패. 사용자가 직접 취소한 경우는 화면에 오류를 띄우지 않는다. */
sealed class SocialLoginException(message: String) : Exception(message) {
    class Cancelled : SocialLoginException("cancelled")
    class Failed(detail: String) : SocialLoginException(detail)
}

/** 로그인 화면이 보는 진행 상태 */
data class LoginUiState(
    val loggingInProvider: SocialProvider? = null,
    val isReviewLoggingIn: Boolean = false,
    /** 로그인 화면에 보여 줄 오류. 사용자가 취소한 경우에는 null */
    val errorMessage: String? = null,
    /** 로그인 화면 위쪽에 잠깐 띄울 안내 (예: 회원 탈퇴 완료) */
    val noticeMessage: String? = null,
) {
    val isLoggingIn: Boolean get() = loggingInProvider != null || isReviewLoggingIn
}

@Singleton
class SessionManager @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val responseCache: ResponseCache,
    refresher: TokenRefresher,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    /** 로그아웃·계정 전환마다 올린다. 그 전에 시작한 요청의 결과가 새 상태를 덮어쓰지 않게 한다. */
    private val generation = AtomicInteger()
    @Volatile private var isRestoring = false

    /** 로그아웃·탈퇴로 나갈 때 (실시간 연결 끊기, 푸시 기기 등록 지우기 등) */
    @Volatile var onSignOut: (suspend () -> Unit)? = null

    init {
        // 갱신 토큰까지 만료되면 (어느 요청에서든) 로그인 화면으로 보낸다.
        refresher.onExpired = {
            generation.incrementAndGet()
            responseCache.clear()
            if (_state.value is SessionState.SignedIn || _state.value is SessionState.Checking) {
                _state.value = SessionState.SignedOut
                _login.update { it.copy(errorMessage = EXPIRED_MESSAGE) }
            }
        }
    }

    /** 앱 시작 때 저장된 세션으로 복원한다. 소셜 로그인 화면은 다시 띄우지 않는다. */
    suspend fun restore() {
        if (isRestoring || _login.value.isLoggingIn) return
        if (_state.value is SessionState.SignedIn || _state.value is SessionState.SignedOut) return
        if (tokenStorage.load() == null) {
            _state.value = SessionState.SignedOut
            return
        }
        isRestoring = true
        _state.value = SessionState.Checking
        val started = generation.get()
        try {
            // 액세스 토큰이 만료됐으면 OkHttp 갱신기가 한 번 갱신해 다시 보낸다.
            val user = apiCall { authApi.me() }
            if (started == generation.get()) _state.value = SessionState.SignedIn(user)
        } catch (e: CancellationException) {
            if (started == generation.get() && _state.value is SessionState.Checking) {
                _state.value = SessionState.RestoreFailed(RESTORE_FAILED_MESSAGE)
            }
            throw e
        } catch (e: ApiError.Unauthorized) {
            if (started == generation.get()) {
                tokenStorage.clear()
                _state.value = SessionState.SignedOut
                _login.update { it.copy(errorMessage = EXPIRED_MESSAGE) }
            }
        } catch (e: Exception) {
            // 네트워크 오류·서버 장애는 로그아웃 사유가 아니다.
            if (started == generation.get()) _state.value = SessionState.RestoreFailed(RESTORE_FAILED_MESSAGE)
        } finally {
            isRestoring = false
        }
    }

    /** 앱이 다시 앞으로 왔을 때: 복원에 실패한 상태면 다시 시도한다. */
    suspend fun onForeground() {
        if (_state.value is SessionState.RestoreFailed) restore()
    }

    /** 소셜 로그인. obtainToken 은 SDK 로그인을 띄워 서버에 보낼 토큰(카카오 액세스 토큰, 구글 idToken)을 돌려준다. */
    suspend fun login(provider: SocialProvider, obtainToken: suspend () -> String) {
        if (_login.value.isLoggingIn) return
        _login.update { it.copy(loggingInProvider = provider, errorMessage = null, noticeMessage = null) }
        try {
            val token = obtainToken()
            val session = apiCall {
                when (provider) {
                    SocialProvider.Kakao -> authApi.loginKakao(AccessTokenBody(token))
                    SocialProvider.Google -> authApi.loginGoogle(IdTokenBody(token))
                    SocialProvider.Apple -> throw SocialLoginException.Failed("Android 에서는 Apple 로그인을 지원하지 않습니다.")
                }
            }
            signIn(session)
        } catch (e: SocialLoginException.Cancelled) {
            // 사용자가 직접 취소했으므로 오류를 보여 주지 않는다.
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _login.update { it.copy(errorMessage = message(e, provider)) }
        } finally {
            _login.update { it.copy(loggingInProvider = null) }
        }
    }

    /** 스토어 심사용 로그인. 성공하면 null, 실패하면 화면에 보여 줄 문구 */
    suspend fun reviewLogin(email: String, password: String): String? {
        if (_login.value.isLoggingIn) return null
        _login.update { it.copy(isReviewLoggingIn = true, errorMessage = null, noticeMessage = null) }
        return try {
            signIn(apiCall { authApi.reviewLogin(ReviewLoginBody(email.trim(), password)) })
            null
        } catch (e: ApiError.Unauthorized) {
            "Incorrect email or password. (이메일 또는 비밀번호가 맞지 않아요.)"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message(e, null)
        } finally {
            _login.update { it.copy(isReviewLoggingIn = false) }
        }
    }

    private fun signIn(session: AuthSession) {
        tokenStorage.save(session.tokens)
        // 다른 계정의 오프라인 보기 내용이 보이지 않게 지운다.
        responseCache.clear()
        generation.incrementAndGet()
        _state.value = SessionState.SignedIn(session.user)
    }

    suspend fun logout() {
        generation.incrementAndGet()
        val tokens = tokenStorage.load()
        // 기기에서는 바로 로그아웃해, 서버 응답을 기다리는 동안 다시 복원되지 않게 한다.
        tokenStorage.clear()
        responseCache.clear()
        _state.value = SessionState.SignedOut
        _login.update { it.copy(errorMessage = null) }
        onSignOut?.invoke()
        if (tokens != null) runCatching { apiCall { authApi.logout(RefreshTokenBody(tokens.refreshToken)) } }
    }

    fun clearNotice() = _login.update { it.copy(noticeMessage = null) }

    companion object {
        const val EXPIRED_MESSAGE = "로그인이 만료되었습니다. 다시 로그인해 주세요."
        const val RESTORE_FAILED_MESSAGE = "자동 로그인을 완료하지 못했습니다. 연결을 확인하고 다시 시도해 주세요."

        fun message(error: Throwable, provider: SocialProvider?): String {
            val name = provider?.let { "${it.displayName} " }.orEmpty()
            return when (error) {
                is SocialLoginException.Failed -> "${name}로그인에 실패했습니다. 잠시 후 다시 시도해 주세요."
                is ApiError.Unauthorized -> "${name}인증을 확인하지 못했습니다. 다시 로그인해 주세요."
                is ApiError -> error.userMessage
                else -> "로그인 중 문제가 발생했습니다."
            }
        }
    }
}
