package com.ecoruta.dispositivo.presentation.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.EcoCard
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PhotoTitle
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.EcoTheme
import com.ecoruta.dispositivo.presentation.theme.OnPhoto
import com.ecoruta.dispositivo.presentation.theme.PhotoTextShadow

@Composable
fun PairingRoute(viewModel: PairingViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PairingScreen(state, viewModel::onCodeChange, viewModel::onSubmit)
}

@Composable
fun PairingScreen(
    state: PairingUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    ScreenScaffold(
        photo = painterResource(R.drawable.welcome_photo),
        showThemeSelector = true,
        bottomBar = {
            PrimaryButton(
                stringResource(R.string.pairing_submit),
                onSubmit,
                Modifier.weight(1f),
                enabled = state.canSubmit,
                loading = state.isLoading,
            )
        },
    ) {
        PhotoTitle(stringResource(R.string.pairing_title_1), stringResource(R.string.pairing_title_2))
        Text(
            stringResource(R.string.pairing_text),
            color = OnPhoto,
            style = EcoTheme.type.body.copy(shadow = PhotoTextShadow),
        )
        // Right under the instructions it explains, and well clear of the keyboard.
        EcoCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pairing_code_label), color = EcoTheme.colors.inkMuted, style = EcoTheme.type.label)
            CodeField(state.code, onCodeChange, onSubmit, isError = state.errorRes != null)
        }
        if (state.sessionEnded) NoticeBanner(stringResource(R.string.pairing_session_ended), Tone.INFO)
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
    }
}

/**
 * One box per character, so the driver sees at once how many are missing. A single text field
 * underneath takes the input; the boxes only draw it.
 */
@Composable
private fun CodeField(code: String, onCodeChange: (String) -> Unit, onSubmit: () -> Unit, isError: Boolean) {
    val colors = EcoTheme.colors
    val border = EcoTheme.border
    val shape = EcoTheme.radius.lg
    val label = stringResource(R.string.pairing_code_label)
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs),
            ) {
                repeat(PAIRING_CODE_LENGTH) { index ->
                    val isNext = index == code.length
                    val (outlineWidth, outline) = when {
                        isError -> border.focus to colors.error
                        isNext -> border.focus to colors.brand
                        else -> border.field to colors.borderField
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(EcoTheme.size.codeBoxAspect)
                            .clip(shape)
                            .background(colors.surface)
                            .border(outlineWidth, outline, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(code.getOrNull(index)?.toString().orEmpty(), color = colors.ink, style = EcoTheme.type.code)
                    }
                }
            }
        },
    )
}
