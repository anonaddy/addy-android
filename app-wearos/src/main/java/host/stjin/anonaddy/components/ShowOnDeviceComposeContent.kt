package host.stjin.anonaddy.components

import android.content.Context
import android.util.Log
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R

@OptIn(ExperimentalWearMaterialApi::class, androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
@Composable
fun ShowOnDeviceComposeContent(context: Context, hasPairedDevices: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Log.e("ANONDEBUG12", "Box")

        Column {
            val image = AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_watch_setup_notification_anim)
            var atEnd by remember { mutableStateOf(false) }
            Icon(
                painter = rememberAnimatedVectorPainter(image, atEnd),
                contentDescription = null, // decorative element
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.CenterHorizontally)
            )
            if (hasPairedDevices) {
                Text(context.resources.getString(R.string.wearos_check_paired_device), textAlign = TextAlign.Center)
            } else {
                Text(context.resources.getString(R.string.setup_wearos_no_paired_device), textAlign = TextAlign.Center)
            }


            DisposableEffect(Unit) {
                atEnd = !atEnd
                onDispose { }
            }
        }
    }
}