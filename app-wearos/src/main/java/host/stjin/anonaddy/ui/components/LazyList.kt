package host.stjin.anonaddy.ui.components

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.rotaryWithScroll


@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ScalingLazyColumnWithRSB(
    modifier: Modifier = Modifier,
    state: ScalingLazyListState = rememberScalingLazyListState(),
    scalingParams: ScalingParams = ScalingLazyColumnDefaults.scalingParams(),
    reverseLayout: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(
        space = 2.dp,
        alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom
    ),
    autoCentering: AutoCenteringParams = AutoCenteringParams(),
    content: ScalingLazyListScope.() -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    ScalingLazyColumn(
        modifier = modifier.rotaryWithScroll(
            scrollableState = state,
            flingBehavior = ScrollableDefaults.flingBehavior(),
            focusRequester = focusRequester,
        ),
        state = state,
        reverseLayout = reverseLayout,
        scalingParams = scalingParams,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        autoCentering = autoCentering,
        content = content
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}