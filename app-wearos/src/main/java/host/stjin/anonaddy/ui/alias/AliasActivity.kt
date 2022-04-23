package host.stjin.anonaddy.ui.alias

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.components.Loading
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.AliasList
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.CacheHelper
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
        }
    }


    private fun setComposeContent() {
        getAliases()?.let { aliasesList = it }
        setContent {
            Log.e("ANONDEBUG12", "setContent()")
            AppTheme {
                Log.e("ANONDEBUG12", "AppTheme()")
                AnonAddyScaffold()
            }
        }
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    private fun AnonAddyScaffold() {
        Log.e("ANONDEBUG12", "AnonAddyScaffold()")

        val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
        Scaffold(
            modifier = Modifier,
            timeText = {
                CustomTimeText(
                    visible = !scalingLazyListState.isScrollInProgress && scalingLazyListState.centerItemIndex == 0,
                    showLeadingText = true,
                    leadingText = resources.getString(R.string.aliases)
                )
                // TODO remove all Logs
                Log.e("ANONDEBUG12", "timeText")

            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                Log.e("ANONDEBUG12", "positionIndicator")
                PositionIndicator(
                    scalingLazyListState = scalingLazyListState
                )
            },
        ) {
            if (aliasesList.isNullOrEmpty()) {
                Log.e("ANONDEBUG12", "isNullOrEmpty")
                Loading()
            } else {
                Log.e("ANONDEBUG12", "ScalingLazyColumn")
                val favoriteAliases by remember { mutableStateOf(getFavoriteAliases()) }
                AliasList(aliases = aliasesList, favoriteAliases = favoriteAliases, scalingLazyListState = scalingLazyListState, context = this)
            }
        }
    }

    private fun getAliases(): List<Aliases>? {
        val aliases = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(this)
        if (aliases.isNullOrEmpty()) {
            downloadAliases()
        } else {
            // Remove the fav aliases from the aliases
            val favoriteAliases = getFavoriteAliases()
            if (!favoriteAliases.isNullOrEmpty()) {
                aliases.removeAll { favoriteAliases.contains(it.id) }
                // Insert the favorite aliases in the first few positions
                CacheHelper.getBackgroundServiceCacheFavoriteAliasesData(this)?.let { aliases.addAll(0, it) }
            }
        }
        return aliases
    }

    private fun getFavoriteAliases(): List<String>? {
        return FavoriteAliasHelper(this).getFavoriteAliases()?.toList()
    }

    private fun downloadAliases() {
        lifecycleScope.launch {
            NetworkHelper(this@AliasActivity).cacheLastUpdatedAliasesData({ result ->
                if (result) {
                    setComposeContent()
                } else {
                    setContent {
                        ErrorScreen(
                            this@AliasActivity,
                            this@AliasActivity.resources.getString(R.string.could_not_refresh_data),
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
