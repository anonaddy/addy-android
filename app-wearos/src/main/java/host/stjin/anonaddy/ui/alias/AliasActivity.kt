package host.stjin.anonaddy.ui.alias

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.components.Loading
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.AliasList
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.CacheHelper
import host.stjin.anonaddy_shared.utils.NetworkUtils
import kotlinx.coroutines.launch

class AliasActivity : ComponentActivity() {


    private var aliasesList by mutableStateOf(listOf<Aliases>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userResource = CacheHelper.getBackgroundServiceCacheUserResource(this)
        if (userResource == null) {
            // App not setup, open splash
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            // Always perform a download
            downloadAliases()
        }
    }


    private fun setComposeContent() {
        getAliases()?.let { aliasesList = it }
        setContent {
            AppTheme {
                AddyIoScaffold()
            }
        }
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
            if (aliasesList.isEmpty()) {
                Loading()
            } else {
                AliasList(aliases = aliasesList, scalingLazyListState = scalingLazyListState, context = this)
            }
        }
    }

    private fun getAliases(): List<Aliases>? {
        val aliases = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(this)
        if (aliases.isNullOrEmpty()) {
            downloadAliases()
        }
        return aliases
    }

    private fun downloadAliases() {
        lifecycleScope.launch {
            NetworkHelper(this@AliasActivity).cacheLastUpdatedAliasesData({ result ->
                if (result) {
                    setComposeContent()
                } else {
                    setContent {
                        val baseError = this@AliasActivity.resources.getString(R.string.could_not_refresh_data)
                        val displayError = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN && NetworkUtils.isLocalAddress(API_BASE_URL) &&
                            ContextCompat.checkSelfPermission(this@AliasActivity, Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
                        ) {
                            baseError + "\n\n" + resources.getString(R.string.local_network_permission_rationale)
                        } else {
                            baseError
                        }

                        ErrorScreen(
                            this@AliasActivity,
                            displayError,
                            this@AliasActivity.resources.getString(R.string.aliases)
                        )
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        setComposeContent()
    }


}
