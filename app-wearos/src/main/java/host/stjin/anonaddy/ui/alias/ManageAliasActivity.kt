package host.stjin.anonaddy.ui.alias

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.components.Loading
import host.stjin.anonaddy.components.ShowOnDeviceComposeContent
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyChipColors
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyToggleChipColors
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

class ManageAliasActivity : ComponentActivity() {

    private var alias: Aliases? = null
    private lateinit var networkHelper: NetworkHelper
    private lateinit var favoriteAliasHelper: FavoriteAliasHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkHelper = NetworkHelper(this)
        favoriteAliasHelper = FavoriteAliasHelper(this)

        val aliasId: String? = intent.getStringExtra("alias")
        val aliasList = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(this)
        val favoriteAliasList = CacheHelper.getBackgroundServiceCacheFavoriteAliasesData(this)
        // If there are favorite aliases, add them to local list
        favoriteAliasList?.let { aliasList?.addAll(it) }

        if (aliasId == null || aliasList == null) {
            finish()
            return
        }

        // Show this alias on paired device(s)
        if (intent.getBooleanExtra("showOnPairedDevice", false)) {
            showAliasOnDevice(aliasId)
        } else {
            // Check if the alias exists in the local storage
            this.alias = aliasList.firstOrNull { it.id == aliasId }
            if (this.alias != null) {
                setContent {
                    ComposeContent()
                }
                // Favorite this alias by default
                if (intent.getBooleanExtra("favorite", false)) {
                    favoriteAlias(true)
                }
            } else {
                setContent {
                    Loading()
                }
                // The alias does not exist in local storage, the alias could be sent from the paired device
                // Try to obtain the alias from web
                lifecycleScope.launch {
                    NetworkHelper(this@ManageAliasActivity).getSpecificAlias({ result, error ->
                        if (result != null) {
                            this@ManageAliasActivity.alias = result
                            setContent {
                                ComposeContent()
                            }
                        } else {
                            setContent {
                                ErrorScreen(
                                    this@ManageAliasActivity,
                                    this@ManageAliasActivity.resources.getString(R.string.error_adding_alias) + "\n" + error,
                                    this@ManageAliasActivity.resources.getString(R.string.edit_alias)
                                )
                            }
                        }
                    }, aliasId)
                }
            }
        }
    }


    private fun noNodesFound(aliasId: String) {
        hasPairedDevices = false
        // No nodes found, let's check again in 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            showAliasOnDevice(aliasId)
        }, 5000)
    }

    private var hasPairedDevices by mutableStateOf(false)
    private fun showAliasOnDevice(aliasId: String) {
        setContent {
            val haptic = LocalHapticFeedback.current
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            ShowOnDeviceComposeContent(this, hasPairedDevices)
        }

        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnCompleteListener { nodes ->
            if (nodes.result.any()) {
                nodeClient.localNode.addOnCompleteListener {
                    hasPairedDevices = true
                    // Send a message to all connected nodes basically broadcasting itself.
                    // Nodes with the app installed will receive this message and open the setup sheet
                    for (node in nodes.result) {
                        Wearable.getMessageClient(this).sendMessage(node.id, "/showAlias", aliasId.toByteArray())
                    }

                    // Close the app after the command has been send
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 5000)
                }
            } else {
                noNodesFound(aliasId)
            }
        }.addOnFailureListener {
            noNodesFound(aliasId)
        }.addOnCanceledListener {
            noNodesFound(aliasId)
        }

    }

    private var isAliasActive by mutableStateOf(false)
    private var isChangingActivationStatus by mutableStateOf(false)
    private var isAliasFavorite by mutableStateOf(false)

    @OptIn(ExperimentalWearMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Composable
    private fun ComposeContent() {
        if (alias != null) {
            isAliasActive = alias!!.active

            // Creates a CoroutineScope bound to the lifecycle
            val scope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            val favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
            isAliasFavorite = favoriteAliases?.contains(this@ManageAliasActivity.alias!!.id) == true

            AppTheme {
                val lazyListState: LazyListState = rememberLazyListState()
                Scaffold(
                    modifier = Modifier,
                    timeText = {
                        CustomTimeText(
                            visible = lazyListState.firstVisibleItemScrollOffset == 0,
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
                            top = 40.dp,
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
                                colors = getAnonAddyToggleChipColors(),
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
                                    if (!lazyListState.isScrollInProgress) {
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
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                    if (!lazyListState.isScrollInProgress) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        favoriteAlias(it)
                                    }
                                },
                                colors = getAnonAddyToggleChipColors(),
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
                                modifier = Modifier
                                    .padding(top = 2.dp, bottom = 2.dp),
                                onClick = {
                                    if (!lazyListState.isScrollInProgress) {
                                        // Happens in method
                                        //haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showAliasOnDevice(alias!!.id)
                                    }
                                },
                                colors = getAnonAddyChipColors(),
                                enabled = true,
                                label = { Text(text = resources.getString(R.string.show_on_paired_device)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_devices),
                                        contentDescription = resources.getString(R.string.show_on_paired_device),
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

    private fun favoriteAlias(boolean: Boolean) {
        if (boolean) {
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
    }


    private suspend fun deactivateAlias() {
        networkHelper.deactivateSpecificAlias({ result ->
            isChangingActivationStatus = false
            if (result == "204") {
                isAliasActive = false
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_edit_active) + "\n" + result, Toast.LENGTH_SHORT).show()
            }

            // Since an alias was deactivated , call scheduleBackgroundWorker. This method will schedule the service if its required
            BackgroundWorkerHelper(this).scheduleBackgroundWorker()
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

            // Since an alias was activated , call scheduleBackgroundWorker. This method will schedule the service if its required
            BackgroundWorkerHelper(this).scheduleBackgroundWorker()
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

        if (alias!!.emails_forwarded > 0) {
            val section1 = DonutSection(
                color = colorResource(id = R.color.portalOrange),
                amount = alias!!.emails_forwarded.toFloat()
            )
            // Always show section 1
            listOfDonutSection.add(section1)
        }

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

        if (listOfDonutSection.sumOf { it.amount.toInt() } > 0) {
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
        } else {
            // There is not data to fill the donut, so don't compose anything
        }

    }

}