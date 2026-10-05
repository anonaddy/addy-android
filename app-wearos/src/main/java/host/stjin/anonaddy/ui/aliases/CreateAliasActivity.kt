package host.stjin.anonaddy.ui.aliases

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.components.AliasCreateGuide
import host.stjin.anonaddy.ui.components.CreatedAliasDetails
import host.stjin.anonaddy.ui.components.ErrorScreen
import host.stjin.anonaddy.ui.components.Loading
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy.ui.theme.AppTheme
import host.stjin.anonaddy.ui.base.BaseComponentActivity

class CreateAliasActivity : BaseComponentActivity() {

    private val viewModel: CreateAliasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent()
    }

    private fun setComposeContent() {
        setContent {
            AppTheme {
                AddyIoScaffold(ServiceLocator.settingsManager)
            }
        }
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    private fun AddyIoScaffold(settingsManager: SettingsManager) {
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
            var skipAliasCreateGuide by remember { mutableStateOf(settingsManager.getSettingsBool(SettingsManager.PREFS.WEAROS_SKIP_ALIAS_CREATE_GUIDE)) }

            androidx.compose.runtime.LaunchedEffect(skipAliasCreateGuide) {
                if (skipAliasCreateGuide && viewModel.alias == null && viewModel.errorMessage == null) {
                    viewModel.createAlias()
                }
            }

            val error = viewModel.errorMessage
            val createdAlias = viewModel.alias
            if (error != null) {
                ErrorScreen(
                    this@CreateAliasActivity,
                    error,
                    resources.getString(R.string.add_alias)
                )
            } else if (createdAlias != null) {
                CreatedAliasDetails(scalingLazyListState, createdAlias, this@CreateAliasActivity, this@CreateAliasActivity)
            } else if (skipAliasCreateGuide) {
                Loading()
            } else {
                val onIUnderstandClick = {
                    skipAliasCreateGuide = true
                }
                // show Guide
                AliasCreateGuide(scalingLazyListState, settingsManager, this@CreateAliasActivity, onIUnderstandClick)
            }
        }
    }
}
