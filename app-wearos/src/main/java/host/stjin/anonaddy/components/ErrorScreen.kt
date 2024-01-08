package host.stjin.anonaddy.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.ui.components.ScalingLazyColumnWithRSB
import host.stjin.anonaddy_shared.ui.theme.AppTheme

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun ErrorScreen(context: Context, text: String, leadingText: String? = null) {
    val haptic = LocalHapticFeedback.current
    // 2 vibrations for error
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

    AppTheme {
        val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
        Scaffold(
            modifier = Modifier,
            timeText = {
                CustomTimeText(
                    visible = (remember { derivedStateOf { scalingLazyListState.centerItemIndex } }).value < 1,
                    showLeadingText = true,
                    leadingText = leadingText ?: context.resources.getString(R.string.app_name)
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = scalingLazyListState
                )
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                ScalingLazyColumnWithRSB(
                    modifier = Modifier.fillMaxWidth(),
                    state = scalingLazyListState,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(text, color = MaterialTheme.colors.error, textAlign = TextAlign.Center)
                    }
                }
            }
        }


    }
}