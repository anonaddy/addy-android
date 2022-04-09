package host.stjin.anonaddy.ui.alias

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.components.Loading
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.ui.settings.SettingsActivity
import host.stjin.anonaddy.utils.ColorUtils
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyButtonColors
import host.stjin.anonaddy_shared.utils.CacheHelper
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import kotlinx.coroutines.launch

class AliasActivity : ComponentActivity() {

    private lateinit var favoriteAliasHelper: FavoriteAliasHelper
    private val SPACING_BUTTONS = Dp(8f)
    private val SPACING_ALIAS_BUTTONS = Dp(8f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteAliasHelper = FavoriteAliasHelper(this)

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

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun setComposeContent() {
        setContent {
            AppTheme {
                AliasList()
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

    private fun getFavoriteAliases(): MutableSet<String>? {
        return favoriteAliasHelper.getFavoriteAliases()
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


    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalWearMaterialApi
    @Composable
    private fun AliasList() {
        Log.e("ANONDEBUG12", "AliasList")

        // Creates a CoroutineScope bound to the lifecycle
        val scope = rememberCoroutineScope()
        val aliases = getAliases()
        val favoriteAliases = getFavoriteAliases()
        val haptic = LocalHapticFeedback.current
        val focusRequester = remember { FocusRequester() }
        var currentScrollPosition = 0

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
            if (aliases.isNullOrEmpty()) {
                Log.e("ANONDEBUG12", "isNullOrEmpty")
                Loading()
            } else {
                Log.e("ANONDEBUG12", "ScalingLazyColumn")
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreRotaryScrollEvent {
                            if (currentScrollPosition != scalingLazyListState.centerItemScrollOffset) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            currentScrollPosition = scalingLazyListState.centerItemScrollOffset

                            // return false to ignore this event and continue propagation to the child.
                            false
                        }
                        .onRotaryScrollEvent {
                            scope.launch {
                                scalingLazyListState.animateScrollBy(it.verticalScrollPixels)
                            }
                            true
                        }

                        .focusRequester(focusRequester)
                        .focusable(),
                    contentPadding = PaddingValues(
                        top = 28.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 40.dp
                    ),
                    verticalArrangement = Arrangement.Center,
                    state = scalingLazyListState,
                ) {
                    Log.e("ANONDEBUG12", "scalinglazylistscope")
                    item { AliasActionRow() }
                    items(aliases) { alias ->
                        Chip(
                            colors = ChipDefaults.chipColors(
                                backgroundColor = MaterialTheme.colors.surface,
                                secondaryContentColor = Color(ColorUtils.getMostPopularColor(this@AliasActivity, alias))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            icon = {
                                Icon(
                                    painter = painterResource(id = getStarIcon(alias, favoriteAliases)),
                                    tint = if (favoriteAliases
                                            ?.contains(alias.id) == true
                                    ) Color(ColorUtils.getMostPopularColor(this@AliasActivity, alias)) else Color.White,
                                    contentDescription = resources.getString(R.string.favorite),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .wrapContentSize(align = Alignment.Center),
                                )
                            },
                            label = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    text = alias.email
                                )
                            },
                            secondaryLabel = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    text = (if (alias.description != null) {
                                        alias.description
                                    } else {
                                        resources.getString(
                                            R.string.aliases_recyclerview_list_item_date_time,
                                            DateTimeUtils.turnStringIntoLocalString(
                                                alias.created_at,
                                                DateTimeUtils.DATETIMEUTILS.SHORT_DATE
                                            ),
                                            DateTimeUtils.turnStringIntoLocalString(
                                                alias.created_at,
                                                DateTimeUtils.DATETIMEUTILS.TIME
                                            )
                                        )
                                    }).toString()
                                )
                            },
                            onClick = {
                                if (!scalingLazyListState.isScrollInProgress) {
                                    //haptic.performHapticFeedback(HapticFeedbackType.LongPress) VIBRATES IN ACTIVITY

                                    val intent = Intent(this@AliasActivity, ManageAliasActivity::class.java)
                                    intent.putExtra("alias", alias.id)
                                    startActivity(intent)
                                }
                            },
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

            }


        }

    }

    @Composable
    private fun AliasActionRow() {
        Log.e("ANONDEBUG12", "aliasActionRow")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Dp(48f)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                ShowOnNewAliasLayout()
                Spacer(modifier = Modifier.width(SPACING_BUTTONS))
                ShowOnSettingsLayout()
            }
            Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
        }

    }


    @Composable
    private fun ShowOnNewAliasLayout() {
        Log.e("ANONDEBUG12", "ShowOnNewAliasLayout")

        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = getString(R.string.add_alias)
                }
        ) {
            Button(
                colors = getAnonAddyButtonColors(),
                onClick = {
                    val intent = Intent(this@AliasActivity, CreateAliasActivity::class.java)
                    startActivity(intent)
                },
                enabled = true,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = getString(R.string.add_alias),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        }
    }

    @Composable
    private fun ShowOnSettingsLayout() {
        Log.e("ANONDEBUG12", "ShowOnSettingsLayout")

        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = getString(R.string.settings)
                }
        ) {
            Button(
                colors = getAnonAddyButtonColors(),
                onClick = {
                    val intent = Intent(this@AliasActivity, SettingsActivity::class.java)
                    startActivity(intent)
                },
                enabled = true,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = getString(R.string.settings),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        }
    }

    private fun getStarIcon(aliases: Aliases, favoriteAliases: MutableSet<String>?): Int {
        Log.e("ANONDEBUG12", "getStarIcon")

        return if (favoriteAliases?.contains(aliases.id) == true) {
            R.drawable.ic_starred
        } else {
            R.drawable.ic_star
        }
    }

}
