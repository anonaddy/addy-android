package host.stjin.anonaddy.ui.alias

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutModel
import app.futured.donut.compose.data.DonutSection
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import kotlinx.coroutines.launch

class ManageAliasActivity : ComponentActivity() {

    private var alias: Aliases? = null
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkHelper = NetworkHelper(this)


        val alias: Aliases? = intent.getParcelableExtra("alias")
        if (alias == null) {
            finish()
            return
        }
        this.alias = alias

        setContent {
            ComposeContent()
        }
    }

    var isAliasActive by mutableStateOf(false)
    var isChangingActivationStatus by mutableStateOf(false)
    var isAliasFavorite by mutableStateOf(false)


    @OptIn(ExperimentalWearMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Composable
    private fun ComposeContent() {
        if (alias != null) {
            isAliasActive = alias!!.active


            // Creates a CoroutineScope bound to the lifecycle
            val scope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current

            val favoriteAliasHelper = FavoriteAliasHelper(this)
            val favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
            isAliasFavorite = favoriteAliases?.contains(this@ManageAliasActivity.alias!!.id) == true

            AppTheme {
                val lazyListState: LazyListState = rememberLazyListState()
                Scaffold(
                    modifier = Modifier,
                    timeText = {
                        CustomTimeText(
                            visible = true,
                            showLeadingText = true,
                            leadingText = resources.getString(R.string.edit_alias)
                        )
                    },
                    vignette = {
                        Vignette(vignettePosition = VignettePosition.TopAndBottom)
                    },
                    positionIndicator = {
                        PositionIndicator(
                            lazyListState = lazyListState
                        )

                    }
                ) {
                    val focusRequester = remember { FocusRequester() }
                    var currentScrollPosition = 0
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
                            top = 28.dp,
                            start = 10.dp,
                            end = 10.dp,
                            bottom = 40.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        state = lazyListState,
                    ) {
                        item {
                            GetDonut()
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_forwarded, alias!!.emails_forwarded),
                                icon = R.drawable.ic_inbox,
                                colorResource(
                                    id = R.color.portalOrange
                                )
                            )
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_replied, alias!!.emails_replied),
                                icon = R.drawable.ic_arrow_back_up,
                                colorResource(
                                    id = R.color.portalBlue
                                )
                            )
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_sent, alias!!.emails_sent),
                                icon = R.drawable.ic_mail_forward,
                                colorResource(
                                    id = R.color.easternBlue
                                )
                            )
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_blocked, alias!!.emails_blocked),
                                icon = R.drawable.ic_forbid,
                                colorResource(
                                    id = R.color.softRed
                                )
                            )

                            ToggleChip(
                                modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                                label = {
                                    Text(
                                        if (isAliasActive) resources.getString(R.string.activated) else resources.getString(
                                            R.string.deactivated
                                        ), maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                },
                                checked = isAliasActive,
                                toggleIcon = {
                                    ToggleChipDefaults.SwitchIcon(checked = isAliasActive)
                                },
                                secondaryLabel = {
                                    Text(
                                        if (isChangingActivationStatus) {
                                            resources.getString(
                                                R.string.changing_status
                                            )
                                        } else resources.getString(
                                            R.string.alias_status_desc
                                        ), maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onCheckedChange = {
                                    isAliasActive = it
                                    if (!isChangingActivationStatus) {
                                        if (isAliasActive) {
                                            lifecycleScope.launch {
                                                isChangingActivationStatus = true
                                                activateAlias()
                                            }
                                        } else {
                                            lifecycleScope.launch {
                                                isChangingActivationStatus = true
                                                deactivateAlias()
                                            }
                                        }
                                    }
                                },
                                enabled = true
                            )

                            ToggleChip(
                                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                                label = {
                                    Text(
                                        resources.getString(R.string.favorite), maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                },
                                checked = isAliasFavorite,
                                onCheckedChange = {
                                    if (it) {
                                        if (!favoriteAliasHelper.addAliasAsFavorite(this@ManageAliasActivity.alias!!.id)) {
                                            Toast.makeText(
                                                this@ManageAliasActivity,
                                                resources.getString(R.string.max_favorites_reached),
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        } else {
                                            isAliasFavorite = true
                                        }
                                    } else {
                                        favoriteAliasHelper.removeAliasAsFavorite(this@ManageAliasActivity.alias!!.id)
                                        isAliasFavorite = false
                                    }
                                },
                                toggleIcon = {
                                },
                                appIcon = {
                                    Icon(
                                        painter = if (isAliasFavorite) painterResource(id = R.drawable.ic_starred) else painterResource(
                                            id = R.drawable.ic_star
                                        ),
                                        contentDescription = resources.getString(R.string.alias_status_desc),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .wrapContentSize(align = Alignment.Center),
                                    )
                                },
                                enabled = true
                            )

                            Chip(
                                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                                onClick = { /* Do something */ },
                                enabled = true,
                                label = { Text(text = resources.getString(R.string.set_watchface)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_clock),
                                        contentDescription = resources.getString(R.string.set_watchface),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .wrapContentSize(align = Alignment.Center),
                                    )
                                }
                            )
                        }
                    }

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }

            }
        }
    }


    private suspend fun deactivateAlias() {
        networkHelper.deactivateSpecificAlias({ result ->
            isChangingActivationStatus = false
            if (result == "204") {
                isAliasActive = false
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_edit_active) + "\n" + result, Toast.LENGTH_SHORT).show()
            }
        }, this.alias!!.id)
    }


    private suspend fun activateAlias() {
        networkHelper.activateSpecificAlias({ alias, result ->
            isChangingActivationStatus = false
            if (alias != null) {
                isAliasActive = true
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_edit_active) + "\n" + result, Toast.LENGTH_SHORT).show()
            }
        }, this.alias!!.id)
    }

    @Composable
    fun StatTextView(string: String, icon: Int, color: Color) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        ) {
            Image(painterResource(icon), contentDescription = string, colorFilter = ColorFilter.tint(color = color))
            Text(modifier = Modifier.padding(start = 8.dp), text = string, color = color, style = TextStyle(fontWeight = FontWeight.Bold))
        }
    }

    @Composable
    fun GetDonut() {
        val listOfDonutSection: ArrayList<DonutSection> = arrayListOf()

        val section1 = DonutSection(
            color = colorResource(id = R.color.portalOrange),
            amount = alias!!.emails_forwarded.toFloat()
        )
        // Always show section 1
        listOfDonutSection.add(section1)

        if (alias!!.emails_replied > 0) {
            val section2 = DonutSection(
                color = colorResource(id = R.color.portalBlue),
                amount = alias!!.emails_replied.toFloat()
            )
            listOfDonutSection.add(section2)
        }

        if (alias!!.emails_sent > 0) {
            val section3 = DonutSection(
                color = colorResource(id = R.color.easternBlue),
                amount = alias!!.emails_sent.toFloat()
            )
            listOfDonutSection.add(section3)
        }

        if (alias!!.emails_blocked > 0) {
            val section4 = DonutSection(
                color = colorResource(id = R.color.softRed),
                amount = alias!!.emails_blocked.toFloat()
            )
            listOfDonutSection.add(section4)
        }

        DonutProgress(
            model = DonutModel(
                cap = listOfDonutSection.sumOf { it.amount.toInt() }.toFloat(),
                masterProgress = 1f,
                gapWidthDegrees = 0f,
                gapAngleDegrees = 270f,
                strokeWidth = 16f,
                backgroundLineColor = Color.Transparent,
                // Sort the list by amount so that the biggest number will fill the whole ring
                //TODO For some reason I need to sort by desc on this section while the main app's sections work properly
                sections = listOfDonutSection.sortedByDescending { it.amount },
            ), modifier = Modifier
                .height(56.dp)
                .width(56.dp)
        )
    }

}