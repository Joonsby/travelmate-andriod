package com.joonsby.travelmate.core.data

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 로그인 상태와 앱 앞/뒤 전환에 맞춰 실시간 연결을 켜고 끈다. Application 에서 한 번 [install] 한다.
 * - 로그인하면 연결, 로그아웃하면 끊기
 * - 앱이 다시 앞으로 오면 끊겨 있던 연결을 바로 잇고, 복원에 실패했던 로그인을 다시 시도한다
 */
@Singleton
class AppLifecycle @Inject constructor(
    private val session: SessionManager,
    private val chatSocket: ChatSocket,
    @Named(APP_SCOPE) private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    fun install() {
        session.onSignOut = { chatSocket.stop() }
        scope.launch {
            session.state.distinctUntilChangedBy { (it as? SessionState.SignedIn)?.user?.id }.collect { state ->
                if (state is SessionState.SignedIn) chatSocket.start(state.user.id) else chatSocket.stop()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        chatSocket.reconnect()
        scope.launch { session.onForeground() }
    }
}
