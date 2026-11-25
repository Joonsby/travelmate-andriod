package com.joonsby.travelmate.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.joonsby.travelmate.core.designsystem.TMBanner
import com.joonsby.travelmate.core.designsystem.TMBannerStyle
import com.joonsby.travelmate.core.designsystem.TMButton
import com.joonsby.travelmate.core.designsystem.TMSpacing
import com.joonsby.travelmate.core.designsystem.TMTextField
import com.joonsby.travelmate.core.designsystem.TMTheme
import kotlinx.coroutines.launch

/**
 * 스토어 심사용 로그인. 심사 환경에서는 Google 이 새 기기 로그인을 막을 수 있어,
 * 서버에 정해 둔 심사 계정으로 이메일·비밀번호만으로 들어오게 한다. 심사관이 읽기 쉽도록 영어를 먼저 쓴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewLoginSheet(
    isLoggingIn: Boolean,
    onSubmit: suspend (email: String, password: String) -> String?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isLoggingIn },
    )
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val passwordFocus = remember { FocusRequester() }
    val canSubmit = email.isNotBlank() && password.isNotEmpty() && !isLoggingIn
    val submit = {
        if (canSubmit) {
            errorMessage = null
            scope.launch {
                val message = onSubmit(email, password)
                if (message == null) onDismiss() else errorMessage = message
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = TMTheme.colors.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = TMSpacing.screen)
                .padding(bottom = TMSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(TMSpacing.md),
        ) {
            Box(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, enabled = !isLoggingIn, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancel", style = TMTheme.type.body, color = TMTheme.colors.brand)
                }
                Text("App Review Sign-In", style = TMTheme.type.headline, color = TMTheme.colors.textPrimary, modifier = Modifier.align(Alignment.Center))
            }
            Text(
                "For App Review only. Sign in with the demo account in the review notes.\n심사용 계정 로그인",
                style = TMTheme.type.footnote,
                color = TMTheme.colors.textSecondary,
            )
            errorMessage?.let { TMBanner(TMBannerStyle.Error, it) }
            TMTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                placeholder = "name@example.com",
                icon = Icons.Rounded.MailOutline,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
            )
            TMTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                icon = Icons.Rounded.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                visualTransformation = PasswordVisualTransformation(),
                focusRequester = passwordFocus,
            )
            TMButton("Sign In", onClick = { submit() }, loading = isLoggingIn, enabled = canSubmit, modifier = Modifier.padding(top = TMSpacing.xs))
        }
    }
}
