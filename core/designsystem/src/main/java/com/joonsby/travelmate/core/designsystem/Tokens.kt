package com.joonsby.travelmate.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// iOS Tokens.swift 를 같은 값으로 옮긴 것. 화면 코드에서는 Color 리터럴 대신 이 토큰만 쓴다.

/** 0xRRGGBB → Color */
fun tmColor(hex: Long, alpha: Float = 1f): Color = Color(0xFF000000 or hex).copy(alpha = alpha)

@Immutable
data class TMColors(
    val brand: Color,
    val brandSecondary: Color,
    val highlight: Color,
    val ink: Color,
    val onBrand: Color,
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val danger: Color,
    val brandGradientStart: Color,
    val brandGradientEnd: Color,
    val isDark: Boolean,
) {
    /** 대표 그라데이션 (깊은 바다 → 밝은 바다), 왼쪽 위 → 오른쪽 아래 */
    val brandGradient: Brush
        get() = Brush.linearGradient(listOf(brandGradientStart, brandGradientEnd), Offset.Zero, Offset.Infinite)

    companion object {
        val Light = TMColors(
            brand = tmColor(0x0B7194), brandSecondary = tmColor(0xEA6F5A), highlight = tmColor(0xF0893A),
            ink = tmColor(0x0B3B52), onBrand = Color.White,
            background = tmColor(0xF3F8F8), surface = tmColor(0xFFFFFF), surfaceMuted = tmColor(0xE7F1F3),
            border = tmColor(0xD5E6EA),
            textPrimary = tmColor(0x10303D), textSecondary = tmColor(0x4E6872), textTertiary = tmColor(0x87A0A8),
            success = tmColor(0x12A372), danger = tmColor(0xE0393E),
            brandGradientStart = tmColor(0x0B6585), brandGradientEnd = tmColor(0x2493AE),
            isDark = false,
        )
        val Dark = TMColors(
            brand = tmColor(0x5CC6E0), brandSecondary = tmColor(0xFF9A84), highlight = tmColor(0xFFB070),
            ink = tmColor(0x0B3B52), onBrand = Color.White,
            background = tmColor(0x061A22), surface = tmColor(0x0E2732), surfaceMuted = tmColor(0x163642),
            border = tmColor(0x244856),
            textPrimary = tmColor(0xEEF7F9), textSecondary = tmColor(0xA3BCC4), textTertiary = tmColor(0x6F8C96),
            success = tmColor(0x3CD39C), danger = tmColor(0xFF6B6F),
            brandGradientStart = tmColor(0x074E66), brandGradientEnd = tmColor(0x1A7890),
            isDark = true,
        )
    }
}

/** 표지 사진이 없는 여행에 쓸 그라데이션. 같은 문자열은 iOS 와 똑같은 색 조합이 나온다. */
object CoverGradient {
    private val palettes = listOf(
        0x0B6585L to 0x38A2BBL, 0xEA6F5AL to 0xF6B494L, 0x2493AEL to 0x8FD3C8L,
        0xF0893AL to 0xF7C77EL, 0x0B4F6CL to 0xEA7B66L, 0x38A2BBL to 0xF2C9A4L,
    )

    /** iOS: unicodeScalars.reduce(0) { ($0 &* 31 &+ scalar) & 0x7FFFFFFF } % 6 */
    fun index(seed: String): Int {
        var acc = 0L
        var i = 0
        while (i < seed.length) {
            val codePoint = seed.codePointAt(i)
            acc = (acc * 31 + codePoint) and 0x7FFF_FFFFL
            i += Character.charCount(codePoint)
        }
        return (acc % palettes.size).toInt()
    }

    fun colors(seed: String): Pair<Color, Color> = palettes[index(seed)].let { (a, b) -> tmColor(a) to tmColor(b) }

    fun brush(seed: String): Brush = colors(seed).let { (a, b) ->
        Brush.linearGradient(listOf(a, b), Offset.Zero, Offset.Infinite)
    }
}

/** 4dp 단위 간격 */
object TMSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    /** 화면 좌우 기본 여백 */
    val screen = 20.dp
}

object TMRadius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 28.dp
}

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/**
 * 글꼴. 크기와 줄 높이는 iOS 기본 글자 크기(Large)의 텍스트 스타일과 같다.
 * sp 라서 시스템 글자 크기 설정에 따라 함께 커진다. iOS 의 둥근 제목체는 굵기로만 맞춘다.
 */
@Immutable
data class TMTypography(
    val display: TextStyle = style(34, 41, FontWeight.Bold),
    val title: TextStyle = style(22, 28, FontWeight.Bold),
    val titleSmall: TextStyle = style(20, 25, FontWeight.SemiBold),
    val headline: TextStyle = style(17, 22, FontWeight.SemiBold),
    val body: TextStyle = style(17, 22, FontWeight.Normal),
    val callout: TextStyle = style(16, 21, FontWeight.Normal),
    val subheadline: TextStyle = style(15, 20, FontWeight.Normal),
    val footnote: TextStyle = style(13, 18, FontWeight.Normal),
    val caption: TextStyle = style(12, 16, FontWeight.Medium),
) {
    private companion object {
        fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
            fontFamily = Pretendard,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = weight,
        )
    }
}

val LocalTMColors = staticCompositionLocalOf { TMColors.Light }
val LocalTMTypography = staticCompositionLocalOf { TMTypography() }
