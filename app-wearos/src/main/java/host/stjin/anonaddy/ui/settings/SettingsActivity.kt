package host.stjin.anonaddy.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ShowOnDeviceComposeContent
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.ui.theme.*
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private lateinit var favoriteAliasHelper: FavoriteAliasHelper
    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private val SPACING_ALIAS_BUTTONS = Dp(8f)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteAliasHelper = FavoriteAliasHelper(this)
        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        setComposeContent()
    }

    private fun getStoreLogsFromSettings(): Boolean {
        return settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS)
    }

    private fun getBackgroundServiceIntervalFromSetting(): Int {
        return when (getBackgroundServiceIntervalValueFromSettings()) {
            15 -> {
                1
            }
            30 -> {
                2
            }
            60 -> {
                3
            }
            120 -> {
                4
            }
            else -> {
                2
            }
        }
    }

    private fun getBackgroundServiceIntervalValueFromSettings(): Int {
        return settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30)
    }

    private fun setComposeContent() {
        setContent {
            ComposeContent()
        }
    }

    private var backgroundServiceInterval by mutableStateOf(2)
    private var backgroundServiceIntervalValue by mutableStateOf(30)
    private var storeLogs by mutableStateOf(false)

    @OptIn(ExperimentalWearMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Composable
    private fun ComposeContent() {
        Log.e("ANONDEBUG12", "ComposeContent")
        AppTheme {
            val scope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current
            var currentScrollPosition = 0
            backgroundServiceInterval = getBackgroundServiceIntervalFromSetting()
            backgroundServiceIntervalValue = getBackgroundServiceIntervalValueFromSettings()
            storeLogs = getStoreLogsFromSettings()
            val focusRequester = remember { FocusRequester() }
            val lazyListState: LazyListState = rememberLazyListState()
            Scaffold(
                modifier = Modifier,
                timeText = {
                    CustomTimeText(
                        visible = !lazyListState.isScrollInProgress && lazyListState.firstVisibleItemScrollOffset == 0,
                        showLeadingText = true,
                        leadingText = resources.getString(R.string.settings)
                    )
                },
                vignette = {
                    Vignette(vignettePosition = VignettePosition.TopAndBottom)
                },
                positionIndicator = {
                    Log.e("ANONDEBUG12", "positionIndicator")
                    PositionIndicator(
                        lazyListState = lazyListState
                    )
                },
            ) {
                Log.e("ANONDEBUG12", "LazyColumn")
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
                        top = 60.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 40.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = lazyListState,
                ) {
                    item { Text(text = resources.getString(R.string.background_service_interval)) }
                    item {
                        Text(
                            text = resources.getString(
                                R.string.background_service_interval_value,
                                backgroundServiceIntervalValue
                            ), Modifier.alpha(0.4f)
                        )
                    }
                    item { InlineSlider(backgroundServiceInterval, haptic) }
                    item {
                        Text(modifier = Modifier.padding(top = 12.dp), text = resources.getString(R.string.tile_favorite_aliases_label))
                    }
                    item { ClearFavoritesChip(lazyListState, haptic) }
                    item { Text(modifier = Modifier.padding(top = 12.dp), text = resources.getString(R.string.logs)) }
                    item { StoreLogsSwitch(lazyListState, haptic) }
                    item { SendLogsToDeviceChip(lazyListState, haptic) }
                    item { Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS)) }
                    item { ClearAllDataChip(lazyListState, haptic) }
                    item { Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS * 2)) }
                    item {
                        Text(
                            text = resources.getString(R.string.crafted_with_love_and_privacy),
                            Modifier.alpha(0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                    item { Text(text = BuildConfig.VERSION_NAME, Modifier.alpha(0.5f), textAlign = TextAlign.Center) }
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }

        }

    }

    @Composable
    private fun ClearAllDataChip(lazyListState: LazyListState, hapticFeedback: HapticFeedback) {
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp),
            onClick = {
                if (!lazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    resetApp()
                }
            },
            colors = getAnonAddyDangerChipColors(),
            enabled = true,
            label = { Text(text = resources.getString(R.string.reset_app)) },
            secondaryLabel = { Text(text = resources.getString(R.string.reset_app_desc)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_loader),
                    contentDescription = resources.getString(R.string.reset_app),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        )
    }

    @Composable
    private fun SendLogsToDeviceChip(lazyListState: LazyListState, hapticFeedback: HapticFeedback) {
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp),
            onClick = {
                if (!lazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    sendLogsToPairedDevice()
                }
            },
            colors = getAnonAddyChipColors(),
            enabled = true,
            label = { Text(text = resources.getString(R.string.send_logs_to_device)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_devices),
                    contentDescription = resources.getString(R.string.send_logs_to_device),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        )
    }

    @Composable
    private fun StoreLogsSwitch(lazyListState: LazyListState, hapticFeedback: HapticFeedback) {
        ToggleChip(
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            label = {
                Text(
                    resources.getString(R.string.store_logs), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            checked = this.storeLogs,
            colors = getAnonAddyToggleChipColors(),
            toggleControl = {
                ToggleChipDefaults.switchIcon(checked = this.storeLogs)
            },
            onCheckedChange = {
                if (!lazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    this.storeLogs = it
                    settingsManager.putSettingsBool(SettingsManager.PREFS.STORE_LOGS, it)
                }
            },
            enabled = true
        )
    }

    @Composable
    private fun ClearFavoritesChip(lazyListState: LazyListState, hapticFeedback: HapticFeedback) {
        Chip(
            modifier = Modifier
                .padding(top = 12.dp),
            onClick = {
                if (!lazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    FavoriteAliasHelper(this@SettingsActivity).clearFavoriteAliases()
                    Toast.makeText(this@SettingsActivity, resources.getString(R.string.favorite_aliases_cleared), Toast.LENGTH_SHORT)
                        .show()
                    // Since the favorite list was modified, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@SettingsActivity).scheduleBackgroundWorker()
                }
            },
            colors = getAnonAddyChipColors(),
            enabled = true,
            label = { Text(text = resources.getString(R.string.clear_favorites)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = resources.getString(R.string.clear_favorites),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        )
    }

    @Composable
    private fun InlineSlider(backgroundServiceInterval: Int, hapticFeedback: HapticFeedback) {
        InlineSlider(
            colors = getAnonAddyInlineSliderColors(),
            modifier = Modifier.padding(top = 12.dp),
            value = backgroundServiceInterval.toFloat(),
            steps = 3,
            onValueChange = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                when (it) {
                    1f -> {
                        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 15)
                        this.backgroundServiceInterval = 1
                        backgroundServiceIntervalValue = 15
                    }
                    2f -> {
                        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30)
                        this.backgroundServiceInterval = 2
                        backgroundServiceIntervalValue = 30
                    }
                    3f -> {
                        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 60)
                        this.backgroundServiceInterval = 3
                        backgroundServiceIntervalValue = 60
                    }
                    4f -> {
                        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 120)
                        this.backgroundServiceInterval = 4
                        backgroundServiceIntervalValue = 120
                    }
                }

                // Since the favorite list was modified, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this).scheduleBackgroundWorker()
            },
            increaseIcon = { Icon(InlineSliderDefaults.Increase, resources.getString(R.string.increase)) },
            decreaseIcon = { Icon(InlineSliderDefaults.Decrease, resources.getString(R.string.decrease)) },
        )
    }


    private var hasPairedDevices by mutableStateOf(false)
    private fun noNodesFound() {
        hasPairedDevices = false
        // No nodes found, let's check again in 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            sendLogsToPairedDevice()
        }, 5000)
    }

    private fun sendLogsToPairedDevice() {
        setContent {
            ShowOnDeviceComposeContent(this, hasPairedDevices)
        }

        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.any()) {
                nodeClient.localNode.addOnSuccessListener {
                    hasPairedDevices = true

                    val logs = Gson().toJson(LoggingHelper(this).getLogs())

                    // Send a message to all connected nodes basically broadcasting itself.
                    // Nodes with the app installed will receive this message and open the setup sheet
                    for (node in nodes) {
                        Wearable.getMessageClient(this).sendMessage(node.id, "/showLogs", logs.toByteArray())
                    }

                    // Close the app after the command has been send
                    Handler(Looper.getMainLooper()).postDelayed({
                        setComposeContent()
                    }, 5000)
                }
            } else {
                noNodesFound()
            }
        }.addOnFailureListener {
            noNodesFound()
        }.addOnCanceledListener {
            noNodesFound()
        }

    }

    private fun resetApp() {
        encryptedSettingsManager.clearSettingsAndCloseApp()
    }
}