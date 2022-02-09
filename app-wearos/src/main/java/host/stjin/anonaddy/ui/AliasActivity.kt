package host.stjin.anonaddy.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
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

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent()

    }

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun setComposeContent() {
        favoriteAliasHelper = FavoriteAliasHelper(this)
        val settingsManager = SettingsManager(encrypt = true, this)
        // Cache contains 15 most popular aliases
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_15_MOST_ACTIVE_ALIASES_DATA)
        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(this, it) }

        if (aliasesList != null) {
            setContent {
                AliasList(aliasesList)
            }
        } else {
            setContent {
                Loading()
            }

            lifecycleScope.launch {
                NetworkHelper(this@AliasActivity).cache15MostPopularAliasesDataForWidget { result ->
                    if (result) {
                        setComposeContent()
                    } else {
                        Toast.makeText(this@AliasActivity, "TODO Could not load aliases", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    private fun Loading() {
        AppTheme {
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
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {

                    // TODO Fix
                    //CircularProgressIndicator()
                }
            }


        }
    }

    override fun onResume() {
        super.onResume()
        setComposeContent()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalWearMaterialApi
    @Composable
    private fun AliasList(listWithAliases: ArrayList<Aliases>) {
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
                val focusRequester = remember { FocusRequester() }

                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent {
                            lifecycleScope.launch {
                                scalingLazyListState.scrollBy(it.verticalScrollPixels)
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
                    items(listWithAliases.size) { index ->
                        Chip(
                            colors = ChipDefaults.chipColors(
                                backgroundColor = MaterialTheme.colors.surface,
                                secondaryContentColor = Color(getMostPopularColor(listWithAliases[index]))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            icon = {
                                Icon(
                                    painter = painterResource(id = getStarIcon(listWithAliases[index])),
                                    tint = if (FavoriteAliasHelper(this@AliasActivity).getFavoriteAliases()
                                            ?.contains(listWithAliases[index].id) == true
                                    ) Color(getMostPopularColor(listWithAliases[index])) else Color.White,
                                    contentDescription = resources.getString(R.string.favorite),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .wrapContentSize(align = Alignment.Center),
                                )
                            },
                            label = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    text = listWithAliases[index].email
                                )
                            },
                            secondaryLabel = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    text = (if (listWithAliases[index].description != null) {
                                        listWithAliases[index].description
                                    } else {
                                        resources.getString(
                                            R.string.aliases_recyclerview_list_item_date_time,
                                            DateTimeUtils.turnStringIntoLocalString(
                                                listWithAliases[index].created_at,
                                                DateTimeUtils.DATETIMEUTILS.SHORT_DATE
                                            ),
                                            DateTimeUtils.turnStringIntoLocalString(
                                                listWithAliases[index].created_at,
                                                DateTimeUtils.DATETIMEUTILS.TIME
                                            )
                                        )
                                    }).toString()
                                )
                            },
                            onClick = {
                                val intent = Intent(this@AliasActivity, ManageAliasActivity::class.java)
                                intent.putExtra("alias", listWithAliases[index])
                                startActivity(intent)
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
    fun Greeting(greetingName: String) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = stringResource(R.string.hello_world, greetingName)
        )
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
        return if (FavoriteAliasHelper(this).getFavoriteAliases()?.contains(aliases.id) == true) {
            R.drawable.ic_starred
        } else {
            R.drawable.ic_star
        }
    }

}