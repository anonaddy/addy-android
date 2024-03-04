package host.stjin.anonaddy.components

import android.content.Context
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.ScalingLazyColumnWithRSB

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun ShowOnDeviceComposeContent(context: Context, hasPairedDevices: Boolean) {
    val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
    Scaffold(
        modifier = Modifier,
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
            val image = AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_watch_setup_notification_anim)
            var atEnd by remember { mutableStateOf(false) }
            ScalingLazyColumnWithRSB(
                modifier = Modifier.fillMaxWidth(),
                state = scalingLazyListState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Icon(
                        painter = rememberAnimatedVectorPainter(image, atEnd),
                        contentDescription = null, // decorative element
                        modifier = Modifier
                            .size(96.dp)
                            .align(Alignment.Center)
                    )
                }
                if (hasPairedDevices) {
                    item {
                        Text(context.resources.getString(R.string.wearos_check_paired_device), textAlign = TextAlign.Center)

                    }
                } else {
                    item {
                        Text(context.resources.getString(R.string.setup_wearos_no_paired_device), textAlign = TextAlign.Center)

                    }
                }

            }
            DisposableEffect(Unit) {
                atEnd = !atEnd
                onDispose { }
            }

        }
    }
}