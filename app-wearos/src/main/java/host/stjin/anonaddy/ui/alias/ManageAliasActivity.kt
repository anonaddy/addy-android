package host.stjin.anonaddy.ui.alias

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutModel
import app.futured.donut.compose.data.DonutSection
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.components.Loading
import host.stjin.anonaddy.components.ShowOnDeviceComposeContent
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.ui.components.ScalingLazyColumnWithRSB
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.ui.theme.getAddyIoChipColors
import host.stjin.anonaddy_shared.ui.theme.getAddyIoToggleChipColors
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

class ManageAliasActivity : ComponentActivity() {

    private var alias: Aliases? = null
    private lateinit var networkHelper: NetworkHelper
    private lateinit var favoriteAliasHelper: FavoriteAliasHelper

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
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.any()) {
                nodeClient.localNode.addOnSuccessListener {
                    hasPairedDevices = true
                    // Send a message to all connected nodes basically broadcasting itself.
                    // Nodes with the app installed will receive this message and open the setup sheet
                    for (node in nodes) {
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
            AppTheme {
                // Creates a CoroutineScope bound to the lifecycle
                val haptic = LocalHapticFeedback.current
                val favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
                isAliasFavorite = favoriteAliases?.contains(this@ManageAliasActivity.alias!!.id) == true


                val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
                Scaffold(
                    modifier = Modifier,
                    timeText = {
                        CustomTimeText(
                            visible = (remember { derivedStateOf { scalingLazyListState.centerItemIndex } }).value < 2,
                            showLeadingText = true,
                            leadingText = resources.getString(R.string.edit_alias)
                        )
                    },
                    vignette = {
                        Vignette(vignettePosition = VignettePosition.TopAndBottom)
                    },
                    positionIndicator = {
                        PositionIndicator(
                            scalingLazyListState = scalingLazyListState,
                            modifier = Modifier
                        )
                    }
                ) {
                    ScalingLazyColumnWithRSB(
                        modifier = Modifier.fillMaxWidth(),
                        state = scalingLazyListState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item { GetDonut() }
                        item {
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_forwarded, alias!!.emails_forwarded),
                                icon = R.drawable.ic_inbox,
                                colorResource(
                                    id = R.color.portalOrange
                                )
                            )
                        }
                        item {
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_replied, alias!!.emails_replied),
                                icon = R.drawable.ic_arrow_back_up,
                                colorResource(
                                    id = R.color.portalBlue
                                )
                            )
                        }
                        item {
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_sent, alias!!.emails_sent),
                                icon = R.drawable.ic_mail_forward,
                                colorResource(
                                    id = R.color.easternBlue
                                )
                            )
                        }
                        item {
                            StatTextView(
                                string = this@ManageAliasActivity.resources.getString(R.string.d_blocked, alias!!.emails_blocked),
                                icon = R.drawable.ic_forbid,
                                colorResource(
                                    id = R.color.softRed
                                )
                            )
                        }
                        item { AliasActiveToggle(scalingLazyListState, haptic) }
                        item { AliasFavoriteToggle(scalingLazyListState, haptic) }
                        item { ShowOnDeviceChip(scalingLazyListState) }
                    }
                }

            }
        }
    }

    @Composable
    private fun ShowOnDeviceChip(scalingLazyListState: ScalingLazyListState) {
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            onClick = {
                if (!scalingLazyListState.isScrollInProgress) {
                    // Happens in method
                    //haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAliasOnDevice(alias!!.id)
                }
            },
            colors = getAddyIoChipColors(),
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

    @Composable
    private fun AliasFavoriteToggle(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        ToggleChip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            label = {
                Text(
                    resources.getString(R.string.favorite), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            checked = isAliasFavorite,
            onCheckedChange = {
                if (!scalingLazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    favoriteAlias(it)
                }
            },
            colors = getAddyIoToggleChipColors(),
            toggleControl = {
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
    }

    @Composable
    private fun AliasActiveToggle(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        ToggleChip(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 2.dp)
                .fillMaxWidth(),
            label = {
                Text(
                    if (isAliasActive) resources.getString(R.string.activated) else resources.getString(
                        R.string.deactivated
                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            checked = isAliasActive,
            colors = getAddyIoToggleChipColors(),
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked = isAliasActive),
                    contentDescription = if (isAliasActive) resources.getString(R.string.activated) else resources.getString(R.string.deactivated),
                )
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
                if (!scalingLazyListState.isScrollInProgress) {
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
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

            },
            enabled = true
        )
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

        // If there are no statistics, sent the emptyDonut value to 1 so that a donut can be drawn
        val emptyDonut = if (alias!!.emails_forwarded == 0 &&
            alias!!.emails_replied == 0 &&
            alias!!.emails_sent == 0 &&
            alias!!.emails_blocked == 0
        ) 1 else 0

        val section1 = DonutSection(
            color = colorResource(id = R.color.portalOrange),
            amount = alias!!.emails_forwarded.toFloat() + emptyDonut
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
                    sections = listOfDonutSection.sortedBy { it.amount },
                ), modifier = Modifier
                    .height(56.dp)
                    .width(56.dp)
            )
        } else {
            // There is not data to fill the donut, so don't compose anything
        }

    }

}