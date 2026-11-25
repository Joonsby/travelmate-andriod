package com.joonsby.travelmate.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joonsby.travelmate.core.designsystem.R as DesignR
import com.joonsby.travelmate.core.designsystem.TMAppMark
import com.joonsby.travelmate.core.designsystem.TMBanner
import com.joonsby.travelmate.core.designsystem.TMBannerStyle
import com.joonsby.travelmate.core.designsystem.TMRadius
import com.joonsby.travelmate.core.designsystem.TMSkyBackground
import com.joonsby.travelmate.core.designsystem.TMSpacing
import com.joonsby.travelmate.core.designsystem.TMTheme
import com.joonsby.travelmate.core.designsystem.TMToast
import com.joonsby.travelmate.core.designsystem.tmColor

/**
 * 로그인 화면. 위쪽은 앱 아이콘 같은 하늘 위에 앱 마크와 소개 문구, 아래쪽은 카드 위에 로그인 버튼.
 * Android 1차에서는 Apple 로그인을 숨긴다(D1). 앱 마크를 1.5초 안에 5번 누르면 심사용 로그인이 열린다.
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = TMTheme.colors
    val type = TMTheme.type
    var showsReviewLogin by remember { mutableStateOf(false) }
    var markTaps by remember { mutableIntStateOf(0) }
    var lastMarkTap by remember { mutableLongStateOf(0L) }
    val textShadow = Shadow(tmColor(0x03384A, 0.3f), blurRadius = 12f, offset = androidx.compose.ui.geometry.Offset(0f, 4f))

    Box(Modifier.fillMaxSize()) {
        TMSkyBackground()
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = TMSpacing.screen)
                    .padding(top = TMSpacing.xxxl * 1.5f, bottom = TMSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TMSpacing.lg),
            ) {
                TMAppMark(
                    96.dp,
                    Modifier
                        .clearAndSetSemantics {}
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            val now = System.currentTimeMillis()
                            markTaps = if (now - lastMarkTap < 1_500) markTaps + 1 else 1
                            lastMarkTap = now
                            if (markTaps >= 5 && !state.isLoggingIn) {
                                markTaps = 0
                                showsReviewLogin = true
                            }
                        },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TMSpacing.xs)) {
                    Text("여행짜자", style = type.display.copy(shadow = textShadow), color = Color.White)
                    Text(
                        "일정부터 동행까지,\n여행 계획을 한곳에서",
                        style = type.titleSmall.copy(shadow = textShadow),
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
                FeatureChips()
            }

            // 아래쪽 카드: 오류 배너, 로그인 버튼, 약관 동의 안내
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = TMRadius.xl, topEnd = TMRadius.xl))
                    .background(colors.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = TMSpacing.screen)
                    .padding(top = TMSpacing.xl, bottom = TMSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TMSpacing.sm),
            ) {
                Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(TMSpacing.sm)) {
                    state.errorMessage?.let { TMBanner(TMBannerStyle.Error, it) }
                    // 하나가 진행 중이면 모든 버튼을 막아 중복 요청을 막는다.
                    KakaoLoginButton(loading = state.loggingInProvider?.serverName == "kakao", enabled = !state.isLoggingIn) {
                        viewModel.loginKakao(context)
                    }
                    GoogleLoginButton(loading = state.loggingInProvider?.serverName == "google", enabled = !state.isLoggingIn) {
                        viewModel.loginGoogle(context)
                    }
                    ConsentText(viewModel.termsUrl, viewModel.privacyUrl)
                }
            }
        }
        // 탈퇴 완료 같은 안내는 로그인 버튼을 가리지 않도록 위쪽에 잠깐 띄운다.
        TMToast(state.noticeMessage, onDismiss = viewModel::clearNotice, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }

    if (showsReviewLogin) {
        ReviewLoginSheet(
            isLoggingIn = state.isReviewLoggingIn,
            onSubmit = viewModel::reviewLogin,
            onDismiss = { showsReviewLogin = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeatureChips() {
    // 한 줄에 들어가지 않으면(큰 글자 설정) 다음 줄로 넘긴다.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TMSpacing.xs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(TMSpacing.xs),
    ) {
        FeatureChip(Icons.Rounded.CalendarMonth, "일정 관리")
        FeatureChip(Icons.Rounded.People, "친구와 함께")
        FeatureChip(Icons.Rounded.Map, "여행지 기록")
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = TMSpacing.sm, vertical = TMSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TMSpacing.xxs + 2.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(text, style = TMTheme.type.caption, color = Color.White)
    }
}

/** 카카오 디자인 가이드: 컨테이너 #FEE500, 레이블 85% 검정 */
@Composable
private fun KakaoLoginButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = Color.Black.copy(alpha = 0.85f)
    LoginButtonBase(tmColor(0xFEE500), null, enabled, onClick) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Rounded.ChatBubble, null, tint = label, modifier = Modifier.size(20.dp))
        }
        Text(if (loading) "로그인 중…" else "카카오 로그인", style = TMTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = label)
    }
}

/** 구글 브랜드 가이드의 밝은 버튼: 흰 배경, #747775 테두리, #1F1F1F 글자, 4색 G 로고 */
@Composable
private fun GoogleLoginButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    LoginButtonBase(Color.White, tmColor(0x747775), enabled, onClick) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = tmColor(0x1F1F1F), strokeWidth = 2.dp)
        } else {
            Image(painterResource(DesignR.drawable.ic_google_g), null, Modifier.size(20.dp))
        }
        Text(if (loading) "로그인 중…" else "Google로 로그인", style = TMTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = tmColor(0x1F1F1F))
    }
}

@Composable
private fun LoginButtonBase(
    container: Color,
    border: Color?,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(TMRadius.md)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .clip(shape)
            .background(container)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** 로그인 버튼 아래 약관 동의 안내. 링크를 누르면 약관 페이지가 브라우저로 열린다. */
@Composable
private fun ConsentText(termsUrl: String, privacyUrl: String) {
    val colors = TMTheme.colors
    val linkStyle = TextLinkStyles(SpanStyle(color = colors.brand))
    val text = buildAnnotatedString {
        append("로그인하면 ")
        withLink(LinkAnnotation.Url(termsUrl, linkStyle)) { append("이용약관") }
        append("과 ")
        withLink(LinkAnnotation.Url(privacyUrl, linkStyle)) { append("개인정보 처리방침") }
        append("에 동의하게 됩니다. 불쾌한 콘텐츠나 다른 사람을 괴롭히는 행동은 허용되지 않아요.")
    }
    Text(
        text,
        style = TMTheme.type.caption,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = TMSpacing.xxs),
    )
}
