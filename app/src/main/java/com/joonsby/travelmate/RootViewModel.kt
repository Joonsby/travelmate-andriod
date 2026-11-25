package com.joonsby.travelmate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joonsby.travelmate.core.data.AppSettings
import com.joonsby.travelmate.core.data.Appearance
import com.joonsby.travelmate.core.data.SessionManager
import com.joonsby.travelmate.core.data.SessionState
import com.joonsby.travelmate.core.network.OfflineStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val session: SessionManager,
    settings: AppSettings,
    offlineStatus: OfflineStatus,
) : ViewModel() {
    val state: StateFlow<SessionState> = session.state
    val isShowingSavedData: StateFlow<Boolean> = offlineStatus.isShowingSavedData
    val appearance: StateFlow<Appearance> = settings.appearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, Appearance.System)

    init {
        // 앱을 켜면 저장된 토큰으로 자동 로그인한다.
        viewModelScope.launch { session.restore() }
    }

    fun retryRestore() = viewModelScope.launch { session.restore() }

    fun logout() = viewModelScope.launch { session.logout() }
}
