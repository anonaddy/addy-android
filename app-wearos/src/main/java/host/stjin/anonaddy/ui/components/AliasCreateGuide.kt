package host.stjin.anonaddy.ui.components

import android.content.Context
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyChipColors
import kotlinx.coroutines.launch

private val SPACING_GUIDE_BUTTONS = Dp(18f)

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalWearMaterialApi
@Composable
fun AliasCreateGuide(lazyListState: LazyListState, settingsManager: SettingsManager, context: Context, onIUnderstandClick: () -> Unit) {
    // Creates a CoroutineScope bound to the lifecycle
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    var currentScrollPosition = 0
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .onPreRotaryScrollEvent {
                    if (currentScrollPosition != lazyListState.firstVisibleItemScrollOffset) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    currentScrollPosition = lazyListState.firstVisibleItemScrollOffset
                    // return false to ignore this event and continue propagation to the child.
                    false
                }
                .onRotaryScrollEvent {
                    scope.launch {
                        lazyListState.animateScrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            contentPadding = PaddingValues(
                top = 40.dp,
                start = 10.dp,
                end = 10.dp,
                bottom = 40.dp
            ),
            horizontalAlignment = CenterHorizontally,
            state = lazyListState,
        ) {
            item {
                Text(
                    text = context.resources.getString(
                        R.string.wearos_create_alias_guide,
                        settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30)
                    ), textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(SPACING_GUIDE_BUTTONS))
                Chip(
                    modifier = Modifier
                        .padding(top = 2.dp, bottom = 2.dp),
                    onClick = {
                        settingsManager.putSettingsBool(SettingsManager.PREFS.WEAROS_SKIP_ALIAS_CREATE_GUIDE, true)
                        onIUnderstandClick()
                    },
                    colors = getAnonAddyChipColors(),
                    enabled = true,
                    label = {
                        Text(
                            text = context.resources.getString(R.string.i_understand)
                        )
                    },
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}