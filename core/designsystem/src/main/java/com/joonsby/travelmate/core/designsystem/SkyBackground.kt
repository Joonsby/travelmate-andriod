package com.joonsby.travelmate.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 앱 아이콘처럼 노을 진 바다 배경: 깊은 바다색 하늘이 아래로 갈수록 노을빛으로 물들고, 오른쪽 아래에 지는 해, 맨 아래 산 능선 두 겹.
 * 시작(복원) 화면과 로그인 화면에 깐다. 다크 모드에서도 같은 배경을 쓴다.
 */
@Composable
fun TMSkyBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                0f to SkyColors[0], 0.45f to SkyColors[1], 0.72f to SkyColors[2], 0.9f to SkyColors[3],
            ),
        )
        val sun = Offset(w * 0.78f, h * 0.86f)
        val glowRadius = 220.dp.toPx()
        drawCircle(
            Brush.radialGradient(
                listOf(tmColor(0xFFC48A, 0.6f), Color.Transparent), center = sun, radius = glowRadius,
            ),
            radius = glowRadius, center = sun,
        )
        drawCircle(
            Brush.verticalGradient(listOf(tmColor(0xFFE7A8), tmColor(0xF7A35C)), sun.y - 48.dp.toPx(), sun.y + 48.dp.toPx()),
            radius = 48.dp.toPx(), center = sun,
        )
        ridge(listOf(0f, 0.35f, 0.15f, 0.55f, 0.3f, 0.6f, 0.2f), centerY = h * 0.9f, height = h * 0.12f, color = tmColor(0x2B7A8E, 0.85f))
        ridge(listOf(0.3f, 0.1f, 0.45f, 0.2f, 0.5f, 0.15f, 0.35f), centerY = h * 0.96f, height = h * 0.1f, color = tmColor(0x0E5A70))
    }
}

/** 시스템 시작 화면 배경과 같은 맨 위 하늘색 */
val SkyColors = listOf(tmColor(0x06566F), tmColor(0x1A7A94), tmColor(0x4E9FB2), tmColor(0xF0A588))

/** peaks: 왼쪽부터 같은 간격의 봉우리 높이 (0 = 맨 위, 1 = 맨 아래). iOS RidgeShape 와 같은 곡선 */
private fun DrawScope.ridge(peaks: List<Float>, centerY: Float, height: Float, color: Color) {
    val top = centerY - height / 2
    val bottom = top + height
    val step = size.width / (peaks.size - 1).coerceAtLeast(1)
    val path = Path().apply {
        moveTo(0f, bottom)
        var previous = Offset(0f, top + peaks[0] * height)
        lineTo(previous.x, previous.y)
        peaks.drop(1).forEachIndexed { i, peak ->
            val point = Offset((i + 1) * step, top + peak * height)
            quadraticTo((previous.x + point.x) / 2, minOf(previous.y, point.y) - height * 0.08f, point.x, point.y)
            previous = point
        }
        lineTo(size.width, bottom)
        // iOS 는 능선 아래를 화면 끝까지 채운다(안전 영역 무시). 아래 여백이 비지 않게 바닥까지 늘린다.
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color)
}

/** 앱 마크 (앱 아이콘 그림, 둥근 사각형 + 그림자) */
@Composable
fun TMAppMark(size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.225f)
    Image(
        painterResource(R.drawable.app_mark),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .shadow(18.dp, shape, ambientColor = tmColor(0x03384A, 0.35f), spotColor = tmColor(0x03384A, 0.35f))
            .clip(shape),
    )
}
