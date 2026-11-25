package com.joonsby.travelmate.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 앱 테마. Material 3 는 기본 동작(리플·접근성·시트)을 위한 바탕으로만 쓰고,
 * 모양은 TM 토큰과 TM 컴포넌트로 그린다.
 */
@Composable
fun TravelMateTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) TMColors.Dark else TMColors.Light
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.brand, onPrimary = colors.onBrand, secondary = colors.brandSecondary,
            background = colors.background, onBackground = colors.textPrimary,
            surface = colors.surface, onSurface = colors.textPrimary, surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary, outline = colors.border, error = colors.danger,
            surfaceContainerLow = colors.surface, surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
        )
    } else {
        lightColorScheme(
            primary = colors.brand, onPrimary = colors.onBrand, secondary = colors.brandSecondary,
            background = colors.background, onBackground = colors.textPrimary,
            surface = colors.surface, onSurface = colors.textPrimary, surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary, outline = colors.border, error = colors.danger,
            surfaceContainerLow = colors.surface, surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
        )
    }
    val typography = TMTypography()
    CompositionLocalProvider(
        LocalTMColors provides colors,
        LocalTMTypography provides typography,
        LocalTextSelectionColors provides TextSelectionColors(colors.brand, colors.brand.copy(alpha = 0.3f)),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** 화면에서 토큰을 읽는 곳: TMTheme.colors.brand, TMTheme.type.headline */
object TMTheme {
    val colors: TMColors
        @Composable @ReadOnlyComposable get() = LocalTMColors.current
    val type: TMTypography
        @Composable @ReadOnlyComposable get() = LocalTMTypography.current
}

/** 그림자 토큰. 다크 모드에서는 그림자 대신 테두리로 구분하므로 그리지 않는다. */
@Composable
fun Modifier.tmShadow(shape: Shape): Modifier {
    val colors = TMTheme.colors
    if (colors.isDark) return this
    val tint = tmColor(0x0B1650, 0.08f)
    return this.shadow(elevation = 12.dp, shape = shape, ambientColor = tint, spotColor = tint)
}
