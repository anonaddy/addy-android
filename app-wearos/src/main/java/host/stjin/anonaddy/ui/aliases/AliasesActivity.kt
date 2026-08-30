package host.stjin.anonaddy.ui.aliases

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.AliasList
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.ui.components.ErrorScreen
import host.stjin.anonaddy.ui.components.Loading
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.NetworkUtils

import host.stjin.anonaddy.ui.base.BaseComponentActivity

class AliasesActivity : BaseComponentActivity() {

    private val viewModel: AliasesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                AddyIoScaffold()
            }
        }

        downloadAliases()
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    private fun AddyIoScaffold() {
        val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()

        Scaffold(
            modifier = Modifier,
            timeText = {
                CustomTimeText(
                    visible = (remember { derivedStateOf { scalingLazyListState.centerItemIndex } }).value < 1,
                    showLeadingText = true,
                    leadingText = resources.getString(R.string.aliases)
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = scalingLazyListState
                )
            },
        ) {
            val error = viewModel.errorMessage
            if (error != null && viewModel.aliasesList.isEmpty()) {
                ErrorScreen(
                    this@AliasesActivity,
                    error,
                    resources.getString(R.string.aliases)
                )
            } else if (viewModel.aliasesList.isEmpty() || viewModel.isLoading) {
                Loading()
            } else {
                AliasList(aliases = viewModel.aliasesList, scalingLazyListState = scalingLazyListState, context = this@AliasesActivity)
            }
        }
    }

    private fun downloadAliases() {
        val baseError = resources.getString(R.string.could_not_refresh_data)
        val localNetworkPermissionRationale = resources.getString(R.string.local_network_permission_rationale)
        val isLocalAddress = NetworkUtils.isLocalAddress(API_BASE_URL)
        val hasLocalNetworkPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        viewModel.downloadAliases(baseError, localNetworkPermissionRationale, isLocalAddress, hasLocalNetworkPermission)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAliasesFromCache()
        downloadAliases()
    }
}
