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
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.ColorUtils
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.CacheHelper
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import kotlinx.coroutines.launch

class AliasActivity : ComponentActivity() {

    private lateinit var favoriteAliasHelper: FavoriteAliasHelper

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteAliasHelper = FavoriteAliasHelper(this)

        setComposeContent()
    }

    private var aliases: ArrayList<Aliases>? = null
    private var favoriteAliases: MutableSet<String>? = null
    private var isLoading by mutableStateOf(true)
    private fun loadAliases() {
        val favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
        // Get all aliases, then remove all the favorite aliases
        val aliases = CacheHelper.getBackgroundServiceCacheMostActiveAliasesData(this)

        if (!favoriteAliases.isNullOrEmpty()) {
            aliases?.removeAll { favoriteAliases.contains(it.id) }
            // Insert the favorite aliases in the first few positions
            CacheHelper.getBackgroundServiceCacheFavoriteAliasesData(this)?.let { aliases?.addAll(0, it) }
        }

        this.favoriteAliases = favoriteAliases
        this.aliases = aliases

        // Triggers recomposition
        isLoading = false
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    private fun setComposeContent() {
        val userResource = CacheHelper.getBackgroundServiceCacheUserResource(this)
        if (userResource != null) {
            // Cache contains 15 most popular aliases
            if (aliases != null) {
                setContent {
                    AliasList()
                }
            } else {
                lifecycleScope.launch {
                    NetworkHelper(this@AliasActivity).cacheMostPopularAliasesDataForWidget({ result ->
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
                    })
                }
            }
        } else {
            // App not setup, open splash
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
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
        isLoading = true
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
                            itemContent = { index ->
                                Chip(
                                    colors = ChipDefaults.chipColors(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        secondaryContentColor = Color(ColorUtils.getMostPopularColor(this@AliasActivity, aliases!![index]))
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = getStarIcon(aliases!![index])),
                                            tint = if (favoriteAliases
                                                    ?.contains(aliases!![index].id) == true
                                            ) Color(ColorUtils.getMostPopularColor(this@AliasActivity, aliases!![index])) else Color.White,
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
                                        intent.putExtra("alias", aliases!![index].id)
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


    private fun getStarIcon(aliases: Aliases): Int {
        return if (favoriteAliases?.contains(aliases.id) == true) {
            R.drawable.ic_starred
        } else {
            R.drawable.ic_star
        }
    }

}
