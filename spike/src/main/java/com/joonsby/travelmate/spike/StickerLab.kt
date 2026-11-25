package com.joonsby.travelmate.spike

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaCodecList
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.nio.ByteBuffer

private data class Sticker(val id: String, val pack: String, val format: String)

private data class DecodeResult(
    val total: Int,
    val imageDecoderOk: Int,
    val bitmapFactoryOk: Int,
    val decoderAlpha: Int,
    val factoryAlpha: Int,
    val failures: List<String>,
)

/**
 * 실험 1. 서버 스티커(HEIC 3,161 + Lottie 860)가 이 기기에서 열리는지.
 * - 그리드: Coil(앱에서 실제로 쓸 경로)로 HEIC 를, lottie-compose 로 Lottie 를 그려 본다
 * - 디코딩 검사: 같은 바이트를 ImageDecoder 와 BitmapFactory 로 각각 풀어 성공 수를 센다
 */
@Composable
fun StickerLab() {
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf(DEFAULT_API_BASE) }
    var stickers by remember { mutableStateOf<List<Sticker>>(emptyList()) }
    var status by remember { mutableStateOf("카탈로그를 불러오는 중…") }
    var result by remember { mutableStateOf<DecodeResult?>(null) }
    var running by remember { mutableStateOf(false) }

    fun loadCatalog() = scope.launch {
        status = "카탈로그를 불러오는 중…"
        status = runCatching {
            stickers = withContext(Dispatchers.IO) { fetchCatalog(baseUrl) }
            stickers.groupBy { "${it.pack}(${it.format})" }.entries.joinToString(" · ") { "${it.key} ${it.value.size}" }
        }.getOrElse { "카탈로그 실패: ${it.message}" }
    }

    fun runDecode(limit: Int) = scope.launch {
        running = true
        result = null
        val heic = stickers.filter { it.format == "heic" }
        val targets = if (limit >= heic.size) heic else heic.shuffled().take(limit)
        result = withContext(Dispatchers.IO) { decodeAll(baseUrl, targets) }
        running = false
    }

    LaunchedEffect(Unit) { loadCatalog() }

    val heic = stickers.filter { it.format == "heic" }
    val lottie = stickers.filter { it.format == "lottie" }

    LazyVerticalGrid(GridCells.Adaptive(64.dp), Modifier.padding(horizontal = 12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. 스티커 실험", style = MaterialTheme.typography.titleLarge)
                Text(hevcDecoderInfo(), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(baseUrl, { baseUrl = it }, Modifier.fillMaxWidth(), label = { Text("API 주소") }, singleLine = true)
                Text(status, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Switch(checked = useWebp, onCheckedChange = { useWebp = it; result = null })
                    Text("  정지 스티커를 WebP 로 받기")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { loadCatalog() }) { Text("다시 불러오기") }
                    Button(onClick = { runDecode(60) }, enabled = heic.isNotEmpty() && !running) { Text("HEIC 60개 검사") }
                    Button(onClick = { runDecode(Int.MAX_VALUE) }, enabled = heic.isNotEmpty() && !running) { Text("전수") }
                }
                if (running) Text("검사 중…")
                result?.let { r ->
                    Text(
                        "검사 ${r.total}개\nImageDecoder 성공 ${r.imageDecoderOk} (투명 유지 ${r.decoderAlpha})" +
                            "\nBitmapFactory 성공 ${r.bitmapFactoryOk} (투명 유지 ${r.factoryAlpha})" +
                            if (r.failures.isEmpty()) "" else "\n실패 예: " + r.failures.take(10).joinToString(),
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text("${if (useWebp) "WebP" else "HEIC"} (Coil) — 앞 48개", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(heic.take(48), key = { it.id + useWebp }) { sticker ->
            AsyncImage(model = fileUrl(baseUrl, sticker), contentDescription = sticker.id, modifier = Modifier.size(64.dp).padding(4.dp))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Lottie (Noto) — 앞 16개", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        }
        items(lottie.take(16), key = { it.id }) { sticker ->
            val composition by rememberLottieComposition(LottieCompositionSpec.Url(fileUrl(baseUrl, sticker)))
            LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(64.dp).padding(4.dp))
        }
    }
}

/** 정지 스티커를 WebP 로 받을지. 서버에 WebP 변환본이 생기면 앱은 이쪽을 쓴다 (D3 A안) */
private var useWebp by mutableStateOf(false)

private fun fileUrl(base: String, s: Sticker) =
    "${base.trimEnd('/')}/stickers/${s.id}.${if (s.format == "lottie") "json" else if (useWebp) "webp" else "heic"}"

/** catalog.json → 피부색 변형까지 펼친 스티커 목록. 형식이 없으면 heic (iOS 와 같은 규칙) */
private fun fetchCatalog(base: String): List<Sticker> {
    val body = Http.client.newCall(Request.Builder().url("${base.trimEnd('/')}/stickers/catalog.json").build()).execute().use {
        check(it.isSuccessful) { "HTTP ${it.code}" }
        it.body.string()
    }
    val packs = JSONObject(body).getJSONArray("packs")
    return buildList {
        for (p in 0 until packs.length()) {
            val pack = packs.getJSONObject(p)
            val format = pack.optString("format", "heic").ifEmpty { "heic" }
            val groups = pack.getJSONArray("groups")
            for (g in 0 until groups.length()) {
                val items = groups.getJSONObject(g).getJSONArray("stickers")
                for (i in 0 until items.length()) {
                    val s = items.getJSONObject(i)
                    val id = s.getString("id")
                    add(Sticker(id, id.substringBefore('/'), format))
                    val tones = s.optJSONArray("tones") ?: continue
                    for (t in 0 until tones.length()) add(Sticker(tones.getJSONObject(t).getString("id"), id.substringBefore('/'), format))
                }
            }
        }
    }
}

private suspend fun decodeAll(base: String, targets: List<Sticker>): DecodeResult {
    val gate = Semaphore(6)
    val outcomes = kotlinx.coroutines.coroutineScope {
        targets.map { s ->
            async {
                gate.withPermit {
                    val bytes = runCatching {
                        Http.client.newCall(Request.Builder().url(fileUrl(base, s)).build()).execute().use { it.body.bytes() }
                    }.getOrNull() ?: return@withPermit Outcome(s.id, false, false, false, false)
                    val viaDecoder = runCatching {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { d, _, _ ->
                            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    }.getOrNull()
                    val viaFactory = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Outcome(s.id, viaDecoder != null, viaFactory != null, viaDecoder?.cornerIsTransparent() == true,
                        viaFactory?.cornerIsTransparent() == true)
                }
            }
        }.awaitAll()
    }
    return DecodeResult(
        total = outcomes.size,
        imageDecoderOk = outcomes.count { it.decoderOk },
        bitmapFactoryOk = outcomes.count { it.factoryOk },
        decoderAlpha = outcomes.count { it.decoderAlpha },
        factoryAlpha = outcomes.count { it.factoryAlpha },
        failures = outcomes.filter { !it.decoderOk }.map { it.id },
    )
}

private class Outcome(val id: String, val decoderOk: Boolean, val factoryOk: Boolean, val decoderAlpha: Boolean, val factoryAlpha: Boolean)

/** 스티커 원본은 모서리가 투명하다. 모서리 알파가 0 이 아니면 투명 정보를 잃은 것이다. */
private fun android.graphics.Bitmap.cornerIsTransparent(): Boolean = hasAlpha() && android.graphics.Color.alpha(getPixel(0, 0)) == 0

/** HEIC 는 HEVC 코덱으로 풀린다. 하드웨어 디코더가 있는지가 기기별 차이의 원인이 된다. */
private fun hevcDecoderInfo(): String {
    val decoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        .filter { !it.isEncoder && it.supportedTypes.any { t -> t.equals("video/hevc", ignoreCase = true) } }
        .map { info ->
            val hw = if (Build.VERSION.SDK_INT >= 29) (if (info.isHardwareAccelerated) "HW" else "SW") else "?"
            "${info.name}($hw)"
        }
    return "HEVC 디코더: " + decoders.ifEmpty { listOf("없음") }.joinToString()
}
