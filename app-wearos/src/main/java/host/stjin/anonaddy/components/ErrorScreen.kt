package host.stjin.anonaddy.components

import android.content.Context
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ErrorScreen(context: Context, text: String, leadingText: String? = null) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 2 vibrations for error
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

    AppTheme {
        val lazyListState: LazyListState = rememberLazyListState()
        Scaffold(
            modifier = Modifier,
            timeText = {
                CustomTimeText(
                    visible = lazyListState.firstVisibleItemScrollOffset == 0,
                    showLeadingText = true,
                    leadingText = leadingText ?: context.resources.getString(R.string.app_name)
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    lazyListState = lazyListState
                )
            }
        ) {
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = lazyListState,
                ) {
                    item {
                        Text(context.resources.getString(R.string.whoops), fontSize = 30.sp, textAlign = TextAlign.Center)
                        Text(text, color = MaterialTheme.colors.error, textAlign = TextAlign.Center)
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }


    }
}