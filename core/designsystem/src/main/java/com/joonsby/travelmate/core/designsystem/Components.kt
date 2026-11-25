package com.joonsby.travelmate.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle

// iOS Components.swift 의 TM 컴포넌트를 같은 이름·수치로 옮긴 것

/** primary: 핵심 행동 / secondary: 보조 행동 / subtle: 목록 안 작은 행동 / destructive: 삭제·거절 */
enum class TMButtonKind { Primary, Secondary, Subtle, Destructive }

/** 높이 54(compact 40), 누르면 0.98배, 비활성 0.45 */
@Composable
fun TMButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: TMButtonKind = TMButtonKind.Primary,
    fullWidth: Boolean = true,
    compact: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = TMTheme.colors
    val type = TMTheme.type
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(150), label = "press")
    val radius = if (compact) TMRadius.sm else TMRadius.md
    val shape = RoundedCornerShape(radius)
    val foreground = when (kind) {
        TMButtonKind.Primary -> colors.onBrand
        TMButtonKind.Secondary -> colors.textPrimary
        TMButtonKind.Subtle -> colors.brand
        TMButtonKind.Destructive -> colors.danger
    }
    val background = when (kind) {
        TMButtonKind.Primary -> colors.brand
        TMButtonKind.Secondary -> colors.surface
        TMButtonKind.Subtle -> colors.brand.copy(alpha = 0.12f)
        TMButtonKind.Destructive -> colors.danger.copy(alpha = 0.12f)
    }
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .defaultMinSize(minHeight = if (compact) 40.dp else 54.dp)
            .clip(shape)
            .background(background)
            .then(if (kind == TMButtonKind.Secondary) Modifier.border(1.dp, colors.border, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) TMSpacing.md else TMSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TMSpacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = if (compact) type.subheadline.copy(fontWeight = FontWeight.SemiBold) else type.headline
        CompositionLocalProvider(LocalContentColor provides foreground, LocalTextStyle provides style) {
            content()
        }
    }
}

/** 글자만 있는 TMButton */
@Composable
fun TMButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: TMButtonKind = TMButtonKind.Primary,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    compact: Boolean = false,
) {
    TMButton(onClick, modifier, kind, fullWidth, compact, enabled && !loading) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        } else {
            if (icon != null) Icon(icon, contentDescription = null, Modifier.size(20.dp))
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 기본 카드 표면: 흰(다크: 남색) 배경, 큰 모서리, 은은한 그림자, 다크 모드 테두리 */
@Composable
fun Modifier.tmCard(padding: Dp = TMSpacing.md, radius: Dp = TMRadius.lg): Modifier {
    val colors = TMTheme.colors
    val shape = RoundedCornerShape(radius)
    return this
        .fillMaxWidth()
        .tmShadow(shape)
        .clip(shape)
        .background(colors.surface)
        .border(0.5.dp, colors.border.copy(alpha = 0.6f), shape)
        .padding(padding)
}

/** 화면 전체 배경 */
@Composable
fun Modifier.tmScreenBackground(): Modifier = background(TMTheme.colors.background)

@Composable
fun TMSectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, count: Int? = null) {
    val colors = TMTheme.colors
    val type = TMTheme.type
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.Bottom) {
        Text(title, style = type.titleSmall, color = colors.textPrimary)
        if (count != null) {
            Spacer(Modifier.size(TMSpacing.xs))
            Text("$count", style = type.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        if (subtitle != null) Text(subtitle, style = type.footnote, color = colors.textTertiary)
    }
}

/** 아이콘, 라벨, 도움말을 가진 입력 칸. 입력하는 중 글자가 있으면 x 버튼으로 지울 수 있다. */
@Composable
fun TMTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    icon: ImageVector? = null,
    helper: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showsClearAlways: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val colors = TMTheme.colors
    val type = TMTheme.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(TMRadius.md)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TMSpacing.xs)) {
        Text(label, style = type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(shape)
                .background(colors.surfaceMuted)
                .border(1.5.dp, if (focused) colors.brand else Color.Transparent, shape)
                .padding(horizontal = TMSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TMSpacing.sm),
        ) {
            if (icon != null) Icon(icon, null, tint = if (focused) colors.brand else colors.textTertiary)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = type.body, color = colors.textTertiary)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = type.body.copy(color = colors.textPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.brand),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    interactionSource = interaction,
                )
            }
            if (value.isNotEmpty() && (focused || showsClearAlways)) {
                TMClearTextButton(onClick = {
                    onValueChange("")
                    focusRequester.requestFocus()
                })
            }
        }
        if (helper != null) Text(helper, style = type.caption, color = colors.textTertiary)
    }
}

/** 입력 칸 오른쪽 x 버튼 */
@Composable
fun TMClearTextButton(onClick: () -> Unit, height: Dp = 44.dp) {
    Box(
        Modifier
            .widthIn(min = 32.dp)
            .heightIn(min = height)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Cancel, contentDescription = "입력 지우기", tint = TMTheme.colors.textTertiary, modifier = Modifier.size(20.dp))
    }
}

/** 빈 화면·상태 화면: 겹친 원 안의 아이콘, 제목, 설명, 버튼 */
@Composable
fun TMEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tint: Color = TMTheme.colors.brand,
    actions: @Composable () -> Unit = {},
) {
    val colors = TMTheme.colors
    val type = TMTheme.type
    val art = 112.dp
    Column(
        modifier.widthIn(max = 420.dp).padding(horizontal = TMSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TMSpacing.lg),
    ) {
        Box(Modifier.size(art * 1.45f), contentAlignment = Alignment.Center) {
            Box(Modifier.size(art * 1.45f).clip(CircleShape).background(tint.copy(alpha = 0.08f)))
            Box(Modifier.size(art).clip(CircleShape).background(tint.copy(alpha = 0.14f)))
            Icon(icon, null, tint = tint, modifier = Modifier.size(art * 0.38f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TMSpacing.xs)) {
            Text(title, style = type.title, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(message, style = type.callout, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        Column(Modifier.padding(top = TMSpacing.xs), verticalArrangement = Arrangement.spacedBy(TMSpacing.sm)) { actions() }
    }
}

@Composable
fun TMChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = TMTheme.colors.textSecondary, filled: Boolean = false) {
    val colors = TMTheme.colors
    Row(
        modifier
            .clip(CircleShape)
            .background(if (filled) tint else tint.copy(alpha = 0.12f))
            .padding(horizontal = TMSpacing.xs + 2.dp, vertical = TMSpacing.xxs + 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TMSpacing.xxs),
    ) {
        val color = if (filled) colors.onBrand else tint
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = TMTheme.type.caption, color = color)
    }
}

/** 성공·오류 안내 배너 */
enum class TMBannerStyle { Success, Error }

@Composable
fun TMBanner(
    style: TMBannerStyle,
    message: String,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val colors = TMTheme.colors
    val type = TMTheme.type
    val tint = if (style == TMBannerStyle.Success) colors.success else colors.danger
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TMRadius.md))
            .background(tint.copy(alpha = 0.1f))
            .padding(TMSpacing.md)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(TMSpacing.sm),
    ) {
        Icon(
            if (style == TMBannerStyle.Success) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            null, tint = tint, modifier = Modifier.padding(top = 1.dp).size(18.dp),
        )
        Text(message, style = type.subheadline, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (actionTitle != null && onAction != null) {
            Text(
                actionTitle,
                style = type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = tint,
                modifier = Modifier.clickable(role = Role.Button, onClick = onAction),
            )
        }
        if (onClose != null) {
            Icon(
                Icons.Rounded.Close, "닫기", tint = colors.textTertiary,
                modifier = Modifier.size(24.dp).clickable(role = Role.Button, onClick = onClose).padding(4.dp),
            )
        }
    }
}

/** 프로필 사진이 없으면 닉네임 첫 글자로 만든 이니셜 원 (사진은 채팅 단계에서 Coil 로 붙인다) */
@Composable
fun TMAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(CoverGradient.brush(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1),
            style = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp),
            color = Color.White,
        )
    }
}
