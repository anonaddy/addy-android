package host.stjin.anonaddy.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.GsonTools
import kotlinx.coroutines.launch

class AliasActivity : ComponentActivity() {

    private lateinit var favoriteAliasHelper: FavoriteAliasHelper
    private lateinit var settingsManager: SettingsManager

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteAliasHelper = FavoriteAliasHelper(this)
        settingsManager = SettingsManager(encrypt = true, this)

        setComposeContent()
    }

    var aliases by mutableStateOf<ArrayList<Aliases>?>(null)
    var favoriteAliases by mutableStateOf<MutableSet<String>?>(null)
    var isLoading by mutableStateOf(true)
    private fun loadAliases() {
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_15_MOST_ACTIVE_ALIASES_DATA)
        aliases = aliasesJson?.let { GsonTools.jsonToAliasObject(this, it) }
        favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
        isLoading = false
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    private fun setComposeContent() {
        // Cache contains 15 most popular aliases
        if (aliases != null) {
            setContent {
                AliasList()
            }
        } else {
            lifecycleScope.launch {
                NetworkHelper(this@AliasActivity).cache15MostPopularAliasesDataForWidget { result ->
                    if (result) {
                        setContent {
                            AliasList()
                            loadAliases()
                        }
                    } else {
                        setContent {
                            ErrorScreen(
                                this@AliasActivity,
                                this@AliasActivity.resources.getString(R.string.could_not_refresh_data),
                                this@AliasActivity.resources.getString(R.string.aliases)
                            )
                        }
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    private fun Loading() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            CircularProgressIndicator()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAliases()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalWearMaterialApi
    @Composable
    private fun AliasList() {

        // Creates a CoroutineScope bound to the lifecycle
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        AppTheme {
            val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
            Scaffold(
                modifier = Modifier,
                timeText = {
                    CustomTimeText(
                        visible = true,
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

                }
            ) {
                if (aliases == null || isLoading) {
                    Loading()
                } else {

                    val focusRequester = remember { FocusRequester() }
                    var currentScrollPosition = 0

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
                        items(count = aliases!!.size,
                            key = {
                                aliases!![it].id
                            },
                            itemContent = { index ->
                                Chip(
                                    colors = ChipDefaults.chipColors(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        secondaryContentColor = Color(getMostPopularColor(aliases!![index]))
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = getStarIcon(aliases!![index])),
                                            tint = if (favoriteAliases
                                                    ?.contains(aliases!![index].id) == true
                                            ) Color(getMostPopularColor(aliases!![index])) else Color.White,
                                            contentDescription = resources.getString(R.string.favorite),
                                            modifier = Modifier
                                                .size(24.dp)
                                                .wrapContentSize(align = Alignment.Center),
                                        )
                                    },
                                    label = {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            text = aliases!![index].email
                                        )
                                    },
                                    secondaryLabel = {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            text = (if (aliases!![index].description != null) {
                                                aliases!![index].description
                                            } else {
                                                resources.getString(
                                                    R.string.aliases_recyclerview_list_item_date_time,
                                                    DateTimeUtils.turnStringIntoLocalString(
                                                        aliases!![index].created_at,
                                                        DateTimeUtils.DATETIMEUTILS.SHORT_DATE
                                                    ),
                                                    DateTimeUtils.turnStringIntoLocalString(
                                                        aliases!![index].created_at,
                                                        DateTimeUtils.DATETIMEUTILS.TIME
                                                    )
                                                )
                                            }).toString()
                                        )
                                    },
                                    onClick = {
                                        val intent = Intent(this@AliasActivity, ManageAliasActivity::class.java)
                                        intent.putExtra("alias", aliases!![index])
                                        startActivity(intent)
                                    },
                                )

                            })
                    }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }


            }


        }
    }

    private fun getMostPopularColor(aliases: Aliases): Int {
        val color1 = if (aliases.active) R.color.portalOrange else R.color.md_grey_500
        val color2 = if (aliases.active) R.color.portalBlue else R.color.md_grey_600
        val color3 = if (aliases.active) R.color.easternBlue else R.color.md_grey_700
        val color4 = if (aliases.active) R.color.softRed else R.color.md_grey_800

        val colorArray = arrayOf(
            arrayOf(aliases.emails_forwarded, ContextCompat.getColor(this, color1)),
            arrayOf(aliases.emails_replied, ContextCompat.getColor(this, color2)),
            arrayOf(aliases.emails_sent, ContextCompat.getColor(this, color3)),
            arrayOf(aliases.emails_blocked, ContextCompat.getColor(this, color4))
        ).maxByOrNull { it[0].toFloat() }

        return colorArray?.get(1) as Int
    }

    private fun getStarIcon(aliases: Aliases): Int {
        return if (favoriteAliases?.contains(aliases.id) == true) {
            R.drawable.ic_starred
        } else {
            R.drawable.ic_star
        }
    }

}
