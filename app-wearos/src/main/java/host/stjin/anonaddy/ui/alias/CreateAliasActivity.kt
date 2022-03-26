package host.stjin.anonaddy.ui.alias

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.components.Loading
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyButtonColors
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyChipColors
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

class CreateAliasActivity : ComponentActivity() {

    private lateinit var favoriteAliasHelper: FavoriteAliasHelper
    private lateinit var settingsManager: SettingsManager
    private lateinit var networkHelper: NetworkHelper
    private var userResource: UserResource? = null
    private val SPACING_ALIAS_BUTTONS = Dp(24f)
    private val SPACING_GUIDE_BUTTONS = Dp(18f)
    private val SPACING_BUTTONS = Dp(8f)

    private var skipAliasCreateGuide by mutableStateOf(true)

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteAliasHelper = FavoriteAliasHelper(this)
        networkHelper = NetworkHelper(this)
        settingsManager = SettingsManager(false, this)
        skipAliasCreateGuide = settingsManager.getSettingsBool(SettingsManager.PREFS.WEAROS_SKIP_ALIAS_CREATE_GUIDE)

        setComposeContent()


        userResource = CacheHelper.getBackgroundServiceCacheUserResource(this)
        if (userResource != null) {
            if (skipAliasCreateGuide) {
                createAlias(userResource!!)
            } // If this value is false the guide will be shown in composeContent
        } else {
            // App not setup, open splash
            val intent = Intent(this@CreateAliasActivity, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun createAlias(userResource: UserResource) {
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
                    // Replace custom with random characters because typing on a watch is... meh
                },
                domain = userResource.default_alias_domain,
                "",
                if (userResource.default_alias_format == "custom") "random_characters" else userResource.default_alias_format,
                local_part = "",
                null
            )
        }
    }


    private var alias by mutableStateOf<Aliases?>(null)

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun setComposeContent() {
        setContent {
            ComposeContent()
        }
    }

    private var isAliasFavorite by mutableStateOf(false)

    @Composable
    private fun AddFavoriteLayout() {
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = getString(R.string.tile_favorite_aliases_favorite)
                }
        ) {
            Button(
                colors = getAnonAddyButtonColors(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isAliasFavorite) {
                        favoriteAliasHelper.removeAliasAsFavorite(this@CreateAliasActivity.alias!!.id)
                        isAliasFavorite = false
                    } else {
                        if (!favoriteAliasHelper.addAliasAsFavorite(this@CreateAliasActivity.alias!!.id)) {
                            Toast.makeText(
                                this@CreateAliasActivity,
                                resources.getString(R.string.max_favorites_reached),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            isAliasFavorite = true
                        }
                    }

                },
                enabled = true,
            ) {
                Icon(
                    painter = if (isAliasFavorite) painterResource(id = R.drawable.ic_starred) else painterResource(id = R.drawable.ic_star),
                    contentDescription = getString(R.string.tile_favorite_aliases_favorite),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Center),
                )
            }
        }
    }

    @Composable
    private fun ShowOnDeviceLayout() {
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = getString(R.string.show_on_paired_device)
                }
        ) {
            Button(
                colors = getAnonAddyButtonColors(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val intent = Intent(this@CreateAliasActivity, ManageAliasActivity::class.java)
                    intent.putExtra("alias", alias!!.id)
                    intent.putExtra("showOnPairedDevice", true)
                    startActivity(intent)
                    finish()
                },
                enabled = true,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_devices),
                    contentDescription = getString(R.string.show_on_paired_device),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Center),
                )
            }
        }
    }


    @Composable
    private fun NewAliasCreated() {
        Box(
            contentAlignment = Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
                Text(alias!!.email, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
                Row {
                    AddFavoriteLayout()
                    Spacer(modifier = Modifier.width(SPACING_BUTTONS))
                    ShowOnDeviceLayout()
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalWearMaterialApi::class)
    @Composable
    private fun ComposeContent() {
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        var currentScrollPosition = 0
        val lazyListState: LazyListState = rememberLazyListState()
        Scaffold(
            modifier = Modifier,
            timeText = {
                CustomTimeText(
                    visible = !lazyListState.isScrollInProgress && lazyListState.firstVisibleItemScrollOffset == 0,
                    showLeadingText = true,
                    leadingText = resources.getString(R.string.add_alias)
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            /* positionIndicator = {
                 Log.e("ANONDEBUG12", "positionIndicator")

                 PositionIndicator(
                     lazyListState = lazyListState
                 )
             }*/
        ) {
            Log.e("ANONDEBUG12", "Scaffold")

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreRotaryScrollEvent {
                        if (currentScrollPosition != lazyListState.firstVisibleItemScrollOffset) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        currentScrollPosition = lazyListState.firstVisibleItemScrollOffset
                        // return false to ignore this event and continue propagation to the child.
                        false
                    }
                    .onRotaryScrollEvent {
                        scope.launch {
                            lazyListState.animateScrollBy(it.verticalScrollPixels)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
                contentPadding = PaddingValues(
                    top = 40.dp,
                    start = 10.dp,
                    end = 10.dp,
                    bottom = 40.dp
                ),
                horizontalAlignment = CenterHorizontally,
                state = lazyListState,
            ) {
                item {
                    if (!skipAliasCreateGuide) {
                        AliasCreateGuide()
                    } else if (alias == null) {
                        Loading()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        NewAliasCreated()
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    @Composable
    private fun AliasCreateGuide() {
        Text(text = resources.getString(R.string.wearos_create_alias_guide), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(SPACING_GUIDE_BUTTONS))
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp),
            onClick = {
                settingsManager.putSettingsBool(SettingsManager.PREFS.WEAROS_SKIP_ALIAS_CREATE_GUIDE, true)
                skipAliasCreateGuide = true
                createAlias(userResource!!)
            },
            colors = getAnonAddyChipColors(),
            enabled = true,
            label = {
                Text(
                    text = resources.getString(R.string.i_understand)
                )
            },
        )

    }
}
