package host.stjin.anonaddy.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.ui.theme.getAddyIoChipColors

private val SPACING_GUIDE_BUTTONS = Dp(18f)

@ExperimentalWearMaterialApi
@Composable
fun AliasCreateGuide(scalingLazyListState: ScalingLazyListState, settingsManager: SettingsManager, context: Context, onIUnderstandClick: () -> Unit) {
    // Creates a CoroutineScope bound to the lifecycle
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        ScalingLazyColumnWithRSB(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            snap = false,
            state = scalingLazyListState
        ) {
            item {
                Spacer(modifier = Modifier.height(Dp(36f)))
            }
            item {
                Text(
                    text = context.resources.getString(
                        R.string.wearos_create_alias_guide,
                        settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30)
                    ), textAlign = TextAlign.Center
                )
            }
            item {
                Spacer(modifier = Modifier.height(SPACING_GUIDE_BUTTONS))
            }
            item {
                Chip(
                    modifier = Modifier
                        .padding(top = 2.dp, bottom = 2.dp),
                    onClick = {
                        settingsManager.putSettingsBool(SettingsManager.PREFS.WEAROS_SKIP_ALIAS_CREATE_GUIDE, true)
                        onIUnderstandClick()
                    },
                    colors = getAddyIoChipColors(),
                    enabled = true,
                    label = {
                        Text(
                            text = context.resources.getString(R.string.i_understand)
                        )
                    })
            }
        }
    }
}