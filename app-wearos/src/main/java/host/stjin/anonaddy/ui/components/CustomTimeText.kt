package host.stjin.anonaddy.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.*

/**
 * Custom version of TimeText (Curved Text) that enables leading text (if wanted) and hides while
 * scrolling so user can just focus on the list's items.
 */
@ExperimentalWearMaterialApi
@Composable
fun CustomTimeText(
    visible: Boolean,
    showLeadingText: Boolean,
    leadingText: String
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        TimeText(
            startCurvedContent = if (showLeadingText) {
                {
                    curvedText(
                        text = leadingText,
                        style = CurvedTextStyle()
                    )
                }
            } else null,
            startLinearContent = if (showLeadingText) {
                {
                    Text(
                        text = leadingText,
                        style = TimeTextDefaults.timeTextStyle()
                    )
                }
            } else null,
        )
    }
}

@ExperimentalWearMaterialApi
@Preview(
    apiLevel = 28,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_LARGE_ROUND
)
@Preview(
    apiLevel = 28,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SQUARE
)
@Preview(
    apiLevel = 28,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SMALL_ROUND
)
// This will only be rendered properly in AS Chipmunk and beyond
@Composable
fun PreviewCustomTimeText() {
    CustomTimeText(
        visible = true,
        showLeadingText = true,
        leadingText = "Testing Leading Text..."
    )
}