package host.stjin.anonaddy.ui.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.AliasCreateGuide
import host.stjin.anonaddy.ui.components.CreatedAliasDetails
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

class CreateAliasActivity : ComponentActivity() {


    private var alias by mutableStateOf<Aliases?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userResource = CacheHelper.getBackgroundServiceCacheUserResource(this)
        if (userResource == null) {
            // App not setup, open splash
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        setComposeContent()
    }

    private fun setComposeContent() {
        setContent {
            AppTheme {
                AddyIoScaffold(SettingsManager(false, this))
            }
        }
    }


    @OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearMaterialApi::class)
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

            if (alias != null) {
                CreatedAliasDetails(scalingLazyListState, alias!!, this, this)
            } else {
                if (skipAliasCreateGuide) {
                    Loading()
                    createAlias()
                } else {
                    val onIUnderstandClick = {
                        skipAliasCreateGuide = true
                    }
                    // show Guide
                    AliasCreateGuide(scalingLazyListState, settingsManager, this, onIUnderstandClick)
                }
            }


        }
    }


    private fun createAlias() {
        val userResource = CacheHelper.getBackgroundServiceCacheUserResource(this)

        lifecycleScope.launch {
            NetworkHelper(this@CreateAliasActivity).addAlias(
                { alias, error ->
                    if (alias != null) {
                        this@CreateAliasActivity.alias = alias
                        // Since an alias was added, call scheduleBackgroundWorker. This method will schedule the service if its required
                        BackgroundWorkerHelper(this@CreateAliasActivity).scheduleBackgroundWorker()
                    } else {
                        setContent {
                            ErrorScreen(
                                this@CreateAliasActivity,
                                this@CreateAliasActivity.resources.getString(R.string.error_adding_alias) + "\n" + error,
                                this@CreateAliasActivity.resources.getString(R.string.add_alias)
                            )
                        }
                    }
                },
                domain = userResource!!.default_alias_domain,
                "",
                // Replace custom with random characters because typing on a watch is... meh
                if (userResource.default_alias_format == "custom") "random_characters" else userResource.default_alias_format,
                aliasLocalPart = "",
                null
            )
        }
    }

}
