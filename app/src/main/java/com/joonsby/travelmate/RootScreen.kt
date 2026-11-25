package com.joonsby.travelmate

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joonsby.travelmate.core.data.SessionState
import com.joonsby.travelmate.core.designsystem.OfflineBadge
import com.joonsby.travelmate.core.designsystem.TMAppMark
import com.joonsby.travelmate.core.designsystem.TMAvatar
import com.joonsby.travelmate.core.designsystem.TMBottomBar
import com.joonsby.travelmate.core.designsystem.TMButton
import com.joonsby.travelmate.core.designsystem.TMButtonKind
import com.joonsby.travelmate.core.designsystem.TMEmptyState
import com.joonsby.travelmate.core.designsystem.TMSkyBackground
import com.joonsby.travelmate.core.designsystem.TMSpacing
import com.joonsby.travelmate.core.designsystem.TMTabItem
import com.joonsby.travelmate.core.designsystem.TMTheme
import com.joonsby.travelmate.core.designsystem.TMToast
import com.joonsby.travelmate.core.designsystem.tmColor
import com.joonsby.travelmate.core.designsystem.tmScreenBackground
import com.joonsby.travelmate.core.model.AuthUser
import com.joonsby.travelmate.feature.auth.LoginScreen

/** 로그인 상태에 따라 시작(복원) 화면, 복원 실패, 로그인, 탭 화면을 바꿔 보여 준다. */
@Composable
fun RootScreen(viewModel: RootViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        SessionState.Checking -> {
            SystemBarIcons(dark = false)
            SplashScreen()
        }
        is SessionState.RestoreFailed -> {
            SystemBarIcons(dark = !TMTheme.colors.isDark)
            Box(Modifier.fillMaxSize().tmScreenBackground(), contentAlignment = Alignment.Center) {
                TMEmptyState(
                    icon = Icons.Rounded.WifiOff,
                    title = "로그인 연결을 확인해 주세요",
                    message = current.message,
                    tint = TMTheme.colors.highlight,
                ) {
                    TMButton("다시 시도", onClick = { viewModel.retryRestore() }, icon = Icons.Rounded.Refresh)
                    TMButton("다른 계정으로 로그인", onClick = { viewModel.logout() }, kind = TMButtonKind.Secondary)
                }
            }
        }
        SessionState.SignedOut -> {
            SystemBarIcons(dark = false)
            LoginScreen()
        }
        is SessionState.SignedIn -> {
            SystemBarIcons(dark = !TMTheme.colors.isDark)
            val offline by viewModel.isShowingSavedData.collectAsStateWithLifecycle()
            SignedInShell(current.user, offline, onLogout = viewModel::logout)
        }
    }
}

/** 자동 로그인을 확인하는 동안의 화면: 노을 진 바다 위에 앱 아이콘과 이름 */
@Composable
private fun SplashScreen() {
    val shadow = Shadow(tmColor(0x03384A, 0.3f), Offset(0f, 4f), 12f)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TMSkyBackground()
        Column(
            Modifier.padding(bottom = TMSpacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TMSpacing.md),
        ) {
            TMAppMark(132.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TMSpacing.xxs)) {
                Text("여행짜자", style = TMTheme.type.display.copy(shadow = shadow), color = Color.White)
                Text(
                    "설레는 여행, 함께 떠나요",
                    style = TMTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold, shadow = shadow),
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            CircularProgressIndicator(Modifier.padding(top = TMSpacing.md).size(28.dp), color = Color.White, strokeWidth = 2.5.dp)
        }
    }
}

/**
 * 로그인한 뒤의 탭: 내 여행 | 채팅 | (+) | 친구 | 내 정보.
 * 각 탭의 실제 화면은 Phase 2 부터 채운다. 지금은 자리만 두고, 내 정보에서 로그아웃할 수 있게 한다.
 */
@Composable
private fun SignedInShell(user: AuthUser, offline: Boolean, onLogout: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val tabs = listOf(
        TMTabItem("내 여행", Icons.Rounded.Luggage),
        TMTabItem("채팅", Icons.Rounded.Forum),
        TMTabItem("친구", Icons.Rounded.People),
        TMTabItem("내 정보", Icons.Rounded.AccountCircle),
    )
    Scaffold(
        containerColor = TMTheme.colors.background,
        bottomBar = {
            TMBottomBar(tabs, selected, onSelect = { selected = it }, onCreate = { notice = "새 여행 만들기는 다음 단계에서 연결됩니다." })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (selected == 3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TMSpacing.md)) {
                    TMAvatar(user.nickname, size = 72.dp)
                    Text(user.nickname, style = TMTheme.type.title, color = TMTheme.colors.textPrimary)
                    user.email?.let { Text(it, style = TMTheme.type.subheadline, color = TMTheme.colors.textSecondary) }
                    TMButton("로그아웃", onClick = onLogout, kind = TMButtonKind.Secondary, fullWidth = false)
                }
            } else {
                TMEmptyState(
                    icon = Icons.Rounded.Construction,
                    title = "${tabs[selected].title} 준비 중",
                    message = "이 화면은 다음 개발 단계에서 채워집니다.",
                )
            }
            OfflineBadge(offline, Modifier.align(Alignment.TopCenter).statusBarsPadding())
            TMToast(notice, onDismiss = { notice = null }, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(), durationMillis = 2_500)
        }
    }
}

/** 상태 표시줄·내비게이션 바 아이콘 색. 하늘 배경 위에서는 흰 아이콘을 쓴다. */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(dark) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = dark
                isAppearanceLightNavigationBars = dark
            }
        }
        onDispose {}
    }
}
