package com.joonsby.travelmate.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joonsby.travelmate.core.data.LoginUiState
import com.joonsby.travelmate.core.data.SessionManager
import com.joonsby.travelmate.core.model.SocialProvider
import com.joonsby.travelmate.core.network.ApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionManager,
    private val authConfig: AuthConfig,
    apiConfig: ApiConfig,
) : ViewModel() {
    val state: StateFlow<LoginUiState> = session.login
    val termsUrl = apiConfig.termsUrl
    val privacyUrl = apiConfig.privacyUrl

    /** context 는 SDK 가 로그인 창을 띄우는 데만 쓴다 (Activity). */
    fun loginKakao(context: Context) = viewModelScope.launch {
        session.login(SocialProvider.Kakao) { kakaoAccessToken(context) }
    }

    fun loginGoogle(context: Context) = viewModelScope.launch {
        session.login(SocialProvider.Google) { googleIdToken(context, authConfig) }
    }

    /** 성공하면 null, 실패하면 시트에 보여 줄 문구 */
    suspend fun reviewLogin(email: String, password: String): String? = session.reviewLogin(email, password)

    fun clearNotice() = session.clearNotice()
}
