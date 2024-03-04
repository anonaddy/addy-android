package host.stjin.anonaddy.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.ui.components.ScalingLazyColumnWithRSB
import host.stjin.anonaddy_shared.ui.theme.AppTheme

class SetupActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSetup()
        setContent {
            SetComposeView()
        }
    }

    private var hasPairedDevices by mutableStateOf(false)

    @OptIn(ExperimentalWearMaterialApi::class, androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
    @Composable
    private fun SetComposeView() {
        AppTheme {
            val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
            Scaffold(
                modifier = Modifier,
                timeText = {
                    CustomTimeText(
                        visible = true,
                        showLeadingText = true,
                        leadingText = resources.getString(R.string.app_name)
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
                    val image = AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_watch_setup_notification_anim)
                    var atEnd by remember { mutableStateOf(false) }

                    ScalingLazyColumnWithRSB(
                        modifier = Modifier.fillMaxWidth(),
                        state = scalingLazyListState,
                        horizontalAlignment = CenterHorizontally
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
                                Text(this@SetupActivity.resources.getString(R.string.setup_wearos_check_paired_device), textAlign = TextAlign.Center)
                            }
                        } else {
                            item {
                                Text(this@SetupActivity.resources.getString(R.string.setup_wearos_no_paired_device), textAlign = TextAlign.Center)
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
    }

    private fun requestSetup() {
        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.any()) {
                nodeClient.localNode.addOnSuccessListener { localNode ->
                    hasPairedDevices = true
                    // Send a message to all connected nodes basically broadcasting itself.
                    // Nodes with the app installed will receive this message and open the setup sheet
                    for (node in nodes) {
                        Wearable.getMessageClient(this).sendMessage(node.id, "/requestsetup", localNode.displayName.toByteArray())
                    }
                }
            } else {
                noNodesFound()
            }
        }.addOnFailureListener {
            noNodesFound()
        }.addOnCanceledListener {
            noNodesFound()
        }
    }

    private fun noNodesFound() {
        hasPairedDevices = false
        // No nodes found, let's check again in 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            requestSetup()
        }, 5000)
    }

    public override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(p0: DataEventBuffer) {
        finish()
    }

}