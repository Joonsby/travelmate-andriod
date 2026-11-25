package com.joonsby.travelmate.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** 인터넷이 안 돼 마지막으로 불러온 내용을 보여 주는 동안 위쪽 가운데에 뜨는 작은 안내 */
@Composable
fun OfflineBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible, modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        Row(
            Modifier
                .padding(top = 2.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = TMSpacing.sm, vertical = 6.dp)
                .semantics(mergeDescendants = true) { contentDescription = "오프라인입니다. 마지막으로 불러온 내용을 보여 주고 있어요." },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.WifiOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text("오프라인 · 마지막으로 불러온 내용이에요", style = TMTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
        }
    }
}

/** 화면 위쪽에 잠깐 띄우는 안내 (예: 회원 탈퇴 완료). duration 이 지나면 onDismiss */
@Composable
fun TMToast(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier, durationMillis: Long = 4_000) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(durationMillis)
            onDismiss()
        }
    }
    AnimatedVisibility(message != null, modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        Text(
            message.orEmpty(),
            style = TMTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = TMSpacing.screen)
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(TMRadius.md))
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = TMSpacing.md, vertical = TMSpacing.sm),
        )
    }
}

/** 하단 탭 하나 */
data class TMTabItem(val title: String, val icon: ImageVector, val badge: Int = 0)

/**
 * 하단 탭: 내 여행 · 채팅 · (+) 새 여행 · 친구 · 내 정보.
 * 가운데 + 는 화면이 아니라 버튼이라 선택되지 않고 onCreate 만 부른다.
 * 모양은 iOS 탭 막대(아이콘 + 작은 글자, 선택하면 브랜드색, 숫자 배지)를 따른다.
 */
@Composable
fun TMBottomBar(
    items: List<TMTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TMTheme.colors
    Column(modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val middle = items.size / 2
            items.forEachIndexed { index, item ->
                if (index == middle) CreateTab(onCreate, Modifier.weight(1f))
                TabCell(item, selected = index == selectedIndex, onClick = { onSelect(index) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TabCell(item: TMTabItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = TMTheme.colors
    val tint = if (selected) colors.brand else colors.textTertiary
    Column(
        modifier
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            if (item.badge > 0) {
                Text(
                    if (item.badge > 99) "99+" else "${item.badge}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Pretendard,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(tmColor(0xFF3B30))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Text(item.title, style = TMTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 12.sp), color = tint)
    }
}

/** 가운데 "새 여행": #1A8CF0 동그라미 안의 흰 + (iOS CreateTabIcon 과 같은 색) */
@Composable
private fun CreateTab(onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tmColor(0x1A8CF0)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text("새 여행", style = TMTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 12.sp), color = TMTheme.colors.textTertiary)
    }
}
