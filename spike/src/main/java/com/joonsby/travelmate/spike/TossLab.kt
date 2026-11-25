package com.joonsby.travelmate.spike

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val TOSS_PACKAGE = "viva.republica.toss"

/**
 * 실험 2. iOS 와 같은 supertoss://send?... 링크가 Android 토스에서 열리는지, 값이 채워지는지.
 * 링크는 토스 화면을 열기만 한다. 송금은 토스에서 직접 승인해야 일어난다.
 */
@Composable
fun TossLab() {
    val context = LocalContext.current
    var bank by remember { mutableStateOf("국민은행") }
    var account by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("1000") }
    var memo by remember { mutableStateOf("정산시험") }
    var log by remember { mutableStateOf(tossInstallInfo(context)) }

    val uri = buildTossUri(bank, account, amount.toIntOrNull() ?: 0, memo)

    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("2. 토스 송금 링크", style = MaterialTheme.typography.titleLarge)
        Text("송금은 토스 화면에서 직접 승인해야만 일어납니다. 계좌는 본인 계좌로 시험하세요.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(bank, { bank = it }, Modifier.fillMaxWidth(), label = { Text("은행 이름") }, singleLine = true)
        OutlinedTextField(account, { account = it }, Modifier.fillMaxWidth(), label = { Text("계좌번호 (숫자만)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("금액") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(memo, { memo = it.take(10) }, Modifier.fillMaxWidth(), label = { Text("메모 (10자)") }, singleLine = true)
        Text(uri.toString().replace(Regex("accountNo=\\d+"), "accountNo=…"), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        Button(onClick = { log = tossInstallInfo(context) }, Modifier.fillMaxWidth()) { Text("설치·처리 앱 다시 확인") }
        Button(onClick = { log = openToss(context, uri) }, Modifier.fillMaxWidth(), enabled = account.length in 8..16) { Text("supertoss 링크 열기") }
        Text(log, fontFamily = FontFamily.Monospace)
    }
}

/** iOS TossTransferLink.url() 과 같은 형식. Uri.Builder 는 공백을 %20, + 를 %2B 로 인코딩한다. */
private fun buildTossUri(bank: String, account: String, amount: Int, memo: String): Uri =
    Uri.Builder().scheme("supertoss").authority("send")
        .appendQueryParameter("bank", bank.trim())
        .appendQueryParameter("accountNo", account.filter { it.isDigit() })
        .appendQueryParameter("amount", amount.toString())
        .apply { if (memo.isNotBlank()) appendQueryParameter("message", memo.trim()) }
        .build()

private fun tossInstallInfo(context: Context): String {
    val pm = context.packageManager
    val installed = runCatching { pm.getPackageInfo(TOSS_PACKAGE, 0).versionName }.getOrNull()
    val handlers = pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("supertoss://send")), PackageManager.MATCH_DEFAULT_ONLY)
        .map { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
    return "토스 설치: ${installed ?: "없음"}\nsupertoss 처리 앱: ${handlers.ifEmpty { listOf("없음") }.joinToString()}"
}

private fun openToss(context: Context, uri: Uri): String = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    "링크를 열었습니다. 토스에서 송금 화면이 열렸는지, 은행·계좌·금액·메모가 채워졌는지 확인하세요."
} catch (e: ActivityNotFoundException) {
    "열 수 있는 앱이 없습니다 (ActivityNotFoundException). 앱에서는 계좌 복사로 안내해야 합니다."
}
