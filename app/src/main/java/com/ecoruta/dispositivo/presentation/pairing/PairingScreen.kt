package com.ecoruta.dispositivo.presentation.pairing

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.GlassPanel
import com.ecoruta.dispositivo.presentation.components.HighlightTitle
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.RouteIllustration
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.components.glass
import com.ecoruta.dispositivo.presentation.theme.SourceSans
import com.ecoruta.dispositivo.presentation.theme.ecoColors

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
        bottomBar = {
            PrimaryButton(
                stringResource(R.string.pairing_submit),
                onSubmit,
                Modifier.weight(1f),
                enabled = state.canSubmit,
                loading = state.isLoading,
                height = 80.dp,
            )
        },
    ) {
        HighlightTitle(stringResource(R.string.pairing_title_1), stringResource(R.string.pairing_title_2))
        Text(stringResource(R.string.pairing_text), color = ecoColors.inkMuted, style = MaterialTheme.typography.bodyLarge)
        if (state.sessionEnded) NoticeBanner(stringResource(R.string.pairing_session_ended), Tone.INFO)
        // The drawing gives up its room to messages, so none of them ends up below the fold.
        if (!state.sessionEnded && state.errorRes == null) RouteIllustration(Modifier.fillMaxWidth().height(150.dp))
        GlassPanel(strong = true) {
            Text(stringResource(R.string.pairing_code_label), color = ecoColors.inkMuted, style = MaterialTheme.typography.labelMedium)
            CodeField(state.code, onCodeChange, onSubmit, isError = state.errorRes != null)
        }
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
    }
}

/**
 * One box per character, so the driver sees at once how many are missing. A single text field
 * underneath takes the input; the boxes only draw it.
 */
@Composable
private fun CodeField(code: String, onCodeChange: (String) -> Unit, onSubmit: () -> Unit, isError: Boolean) {
    val colors = ecoColors
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(PAIRING_CODE_LENGTH) { index ->
                    val char = code.getOrNull(index)
                    val isNext = index == code.length
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.82f)
                            .glass(shape = shape)
                            .then(
                                if (isNext || isError) {
                                    Modifier.border(2.dp, if (isError) colors.error else colors.brand, shape)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            char?.toString().orEmpty(),
                            color = colors.ink,
                            fontFamily = SourceSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                        )
                    }
                }
            }
        },
    )
}
