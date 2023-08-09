package host.stjin.anonaddy.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import host.stjin.anonaddy.ui.components.ScalingLazyColumnWithRSB
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.ui.theme.*
import host.stjin.anonaddy_shared.utils.LoggingHelper

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
        AppTheme {
            val haptic = LocalHapticFeedback.current
            backgroundServiceInterval = getBackgroundServiceIntervalFromSetting()
            backgroundServiceIntervalValue = getBackgroundServiceIntervalValueFromSettings()
            storeLogs = getStoreLogsFromSettings()
            val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()

            Scaffold(
                modifier = Modifier,
                timeText = {
                    CustomTimeText(
                        visible = (remember { derivedStateOf { scalingLazyListState.centerItemIndex } }).value < 2,
                        showLeadingText = true,
                        leadingText = resources.getString(R.string.settings)
                    )
                },
                vignette = {
                    Vignette(vignettePosition = VignettePosition.TopAndBottom)
                },
                positionIndicator = {
                    PositionIndicator(
                        scalingLazyListState = scalingLazyListState
                    )
                },
            ) {
                ScalingLazyColumnWithRSB(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    snap = false,
                    state = scalingLazyListState
                ) {
                    item { Text(text = resources.getString(R.string.background_service_interval), textAlign = TextAlign.Center) }
                    item {
                        Text(
                            text = resources.getString(
                                R.string.background_service_interval_value,
                                backgroundServiceIntervalValue
                            ), Modifier.alpha(0.4f), textAlign = TextAlign.Center
                        )
                    }
                    item { InlineSlider(backgroundServiceInterval, haptic) }
                    item {
                        Text(modifier = Modifier.padding(top = 12.dp), text = resources.getString(R.string.tile_favorite_aliases_label))
                    }
                    item { ClearFavoritesChip(scalingLazyListState, haptic) }
                    item { Text(modifier = Modifier.padding(top = 12.dp), text = resources.getString(R.string.logs), textAlign = TextAlign.Center) }
                    item { StoreLogsSwitch(scalingLazyListState, haptic) }
                    item { SendLogsToDeviceChip(scalingLazyListState, haptic) }
                    item { Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS)) }
                    item { ClearAllDataChip(scalingLazyListState, haptic) }
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

            }

        }

    }

    @Composable
    private fun ClearAllDataChip(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            onClick = {
                if (!scalingLazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    resetApp()
                }
            },
            colors = getAddyIoDangerChipColors(),
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
    private fun SendLogsToDeviceChip(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            onClick = {
                if (!scalingLazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    sendLogsToPairedDevice()
                }
            },
            colors = getAddyIoChipColors(),
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
    private fun StoreLogsSwitch(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        ToggleChip(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 2.dp)
                .fillMaxWidth(),
            label = {
                Text(
                    resources.getString(R.string.store_logs), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            checked = this.storeLogs,
            colors = getAddyIoToggleChipColors(),
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked = this.storeLogs),
                    contentDescription = if (this.storeLogs) resources.getString(R.string.on) else resources.getString(R.string.off),
                )
            },
            onCheckedChange = {
                if (!scalingLazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    this.storeLogs = it
                    settingsManager.putSettingsBool(SettingsManager.PREFS.STORE_LOGS, it)
                }
            },
            enabled = true
        )
    }

    @Composable
    private fun ClearFavoritesChip(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        Chip(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            onClick = {
                if (!scalingLazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    FavoriteAliasHelper(this@SettingsActivity).clearFavoriteAliases()
                    Toast.makeText(this@SettingsActivity, resources.getString(R.string.favorite_aliases_cleared), Toast.LENGTH_SHORT)
                        .show()
                    // Since the favorite list was modified, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@SettingsActivity).scheduleBackgroundWorker()
                }
            },
            colors = getAddyIoChipColors(),
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
            colors = getAddyIoInlineSliderColors(),
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
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