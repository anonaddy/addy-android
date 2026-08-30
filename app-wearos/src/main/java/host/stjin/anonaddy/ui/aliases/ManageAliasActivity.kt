package host.stjin.anonaddy.ui.aliases

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.activity.viewModels
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.ui.components.ErrorScreen
import host.stjin.anonaddy.ui.components.Loading
import host.stjin.anonaddy.ui.components.ScalingLazyColumnWithRSB
import host.stjin.anonaddy.ui.components.ShowOnDeviceComposeContent
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy.ui.theme.AppTheme
import host.stjin.anonaddy.ui.theme.getAddyIoChipColors
import host.stjin.anonaddy.ui.theme.getAddyIoToggleChipColors
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

import host.stjin.anonaddy.ui.base.BaseComponentActivity

class ManageAliasActivity : BaseComponentActivity() {

    private val viewModel: ManageAliasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aliasId: String? = intent.getStringExtra("alias")
        val aliasList = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(this)
        val pinnedAliasList = CacheHelper.getBackgroundServiceCachePinnedAliasesData(this)
        // If there are favorite aliases, add them to local list
        pinnedAliasList?.let { aliasList?.addAll(it) }

        if (aliasId == null || aliasList == null) {
            finish()
            return
        }

        // Show this alias on paired device(s)
        if (intent.getBooleanExtra("showOnPairedDevice", false)) {
            showAliasOnDevice(aliasId)
        } else {
            // Check if the alias exists in the local storage
            val foundAlias = aliasList.firstOrNull { it.id == aliasId }
            if (foundAlias != null) {
                viewModel.setInitialAlias(foundAlias)
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
                    when (val result = viewModel.getSpecificAlias(aliasId)) {
                        is NetworkResult.Success<Aliases> -> {
                            setContent {
                                ComposeContent()
                            }
                        }
                        is NetworkResult.Error -> {
                            setContent {
                                ErrorScreen(
                                    this@ManageAliasActivity,
                                    this@ManageAliasActivity.resources.getString(R.string.error_adding_alias) + "\n" + result.error,
                                    this@ManageAliasActivity.resources.getString(R.string.edit_alias)
                                )
                            }
                        }
                    }
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

    @OptIn(ExperimentalWearMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Composable
    private fun ComposeContent() {
        val currentAlias = viewModel.alias ?: return // Exit if null
        LaunchedEffect(Unit) {
            if (intent.getBooleanExtra("pinAlias", false)) {
                viewModel.isChangingPinnedStatus = true
                val (_, errorMsg) = viewModel.pinAlias()
                if (errorMsg != null) {
                    Toast.makeText(this@ManageAliasActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        AppTheme {
            // Creates a CoroutineScope bound to the lifecycle
            val haptic = LocalHapticFeedback.current

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
                    item { DonutChart() }
                    item {
                        Text(
                            text = currentAlias.email,
                            modifier = Modifier
                                .fillMaxWidth() // Ensures textAlign works for multiple lines
                                .padding(vertical = 8.dp, horizontal = 4.dp), // Horizontal padding for round screens
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 3, // Allow wrapping for long emails
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                // color = Color.White // Or use a theme color
                            )
                        )
                    }
                    item {
                        val forwardedText = remember(currentAlias.emails_forwarded) {
                            this@ManageAliasActivity.resources.getQuantityString(R.plurals.d_forwarded, currentAlias.emails_forwarded, currentAlias.emails_forwarded)
                        }
                        StatTextView(
                            string = forwardedText,
                            icon = R.drawable.ic_inbox,
                            colorResource(
                                id = R.color.portalOrange
                            )
                        )
                    }
                    if (currentAlias.emails_replied > 0) {
                        item {
                            val repliedText = remember(currentAlias.emails_replied) {
                                this@ManageAliasActivity.resources.getQuantityString(R.plurals.d_replied, currentAlias.emails_replied, currentAlias.emails_replied)
                            }
                            StatTextView(
                                string = repliedText,
                                icon = R.drawable.ic_arrow_back_up,
                                colorResource(
                                    id = R.color.portalBlue
                                )
                            )
                        }
                    }
                    if (currentAlias.emails_sent > 0) {
                        item {
                            val sentText = remember(currentAlias.emails_sent) {
                                this@ManageAliasActivity.resources.getQuantityString(R.plurals.d_sent, currentAlias.emails_sent, currentAlias.emails_sent)
                            }
                            StatTextView(
                                string = sentText,
                                icon = R.drawable.ic_mail_forward,
                                colorResource(
                                    id = R.color.easternBlue
                                )
                            )
                        }
                    }
                    if (currentAlias.emails_blocked > 0) {
                        item {
                            val blockedText = remember(currentAlias.emails_blocked) {
                                this@ManageAliasActivity.resources.getQuantityString(R.plurals.d_blocked, currentAlias.emails_blocked, currentAlias.emails_blocked)
                            }
                            StatTextView(
                                string = blockedText,
                                icon = R.drawable.ic_forbid,
                                colorResource(
                                    id = R.color.softRed
                                )
                            )
                        }
                    }
                    item {
                        AliasActiveToggle(scalingLazyListState, haptic)
                    }
                    item {
                        AliasPinnedToggle(scalingLazyListState, haptic)
                    }
                    item {
                        ShowOnDeviceButton(scalingLazyListState, currentAlias)
                    }
                }
            }

        }

    }

    @Composable
    private fun ShowOnDeviceButton(
        scalingLazyListState: ScalingLazyListState,
        currentAlias: Aliases
    ) {
        Chip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            label = {
                Text(
                    resources.getString(
                        R.string.show_on_paired_device
                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_devices),
                    contentDescription = resources.getString(R.string.show_on_paired_device),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            },
            colors = getAddyIoChipColors(),
            onClick = {
                if (!scalingLazyListState.isScrollInProgress) {
                    showAliasOnDevice(currentAlias.id)
                }
            }
        )
    }

    @Composable
    private fun AliasPinnedToggle(scalingLazyListState: ScalingLazyListState, hapticFeedback: HapticFeedback) {
        ToggleChip(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            label = {
                Text(
                    if (viewModel.isAliasPinned) resources.getString(R.string.pinned) else resources.getString(
                        R.string.pin
                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            checked = viewModel.isAliasPinned,
            onCheckedChange = {
                if (!scalingLazyListState.isScrollInProgress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (viewModel.isAliasPinned) {
                        lifecycleScope.launch {
                            viewModel.isChangingPinnedStatus = true
                            val (_, errorMsg) = viewModel.unpinAlias()
                            if (errorMsg != null) {
                                Toast.makeText(this@ManageAliasActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        lifecycleScope.launch {
                            viewModel.isChangingPinnedStatus = true
                            val (_, errorMsg) = viewModel.pinAlias()
                            if (errorMsg != null) {
                                Toast.makeText(this@ManageAliasActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            colors = getAddyIoToggleChipColors(),
            toggleControl = {
            },
            secondaryLabel = {
                Text(
                    if (viewModel.isChangingPinnedStatus) {
                        resources.getString(
                            R.string.changing_status
                        )
                    } else resources.getString(
                        R.string.pin_status_desc
                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            appIcon = {
                Icon(
                    painter = if (viewModel.isAliasPinned) painterResource(id = R.drawable.ic_pinned) else painterResource(
                        id = R.drawable.ic_pinned_off
                    ),
                    contentDescription = resources.getString(R.string.pin_alias),
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
                    if (viewModel.isAliasActive) resources.getString(R.string.activated) else resources.getString(
                        R.string.deactivated
                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            checked = viewModel.isAliasActive,
            colors = getAddyIoToggleChipColors(),
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked = viewModel.isAliasActive),
                    contentDescription = if (viewModel.isAliasActive) resources.getString(R.string.activated) else resources.getString(R.string.deactivated),
                )
            },
            secondaryLabel = {
                Text(
                    if (viewModel.isChangingActivationStatus) {
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
                    viewModel.isAliasActive = it
                    if (!viewModel.isChangingActivationStatus) {
                        if (viewModel.isAliasActive) {
                            lifecycleScope.launch {
                                viewModel.isChangingActivationStatus = true
                                val (_, errorMsg) = viewModel.activateAlias()
                                if (errorMsg != null) {
                                    Toast.makeText(this@ManageAliasActivity, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            lifecycleScope.launch {
                                viewModel.isChangingActivationStatus = true
                                val (_, errorMsg) = viewModel.deactivateAlias()
                                if (errorMsg != null) {
                                    Toast.makeText(this@ManageAliasActivity, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

            },
            enabled = true
        )
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
    fun DonutChart() {
        val currentAlias = viewModel.alias ?: return // Exit if null
        val portalOrange = colorResource(id = R.color.portalOrange)
        val portalBlue = colorResource(id = R.color.portalBlue)
        val easternBlue = colorResource(id = R.color.easternBlue)
        val softRed = colorResource(id = R.color.softRed)

        val donutModel = remember(
            currentAlias.emails_forwarded,
            currentAlias.emails_replied,
            currentAlias.emails_sent,
            currentAlias.emails_blocked,
            portalOrange, portalBlue, easternBlue, softRed
        ) {
            val emptyDonut = if (currentAlias.emails_forwarded == 0 &&
                currentAlias.emails_replied == 0 &&
                currentAlias.emails_sent == 0 &&
                currentAlias.emails_blocked == 0
            ) 1 else 0

            val sections = ArrayList<DonutSection>(4)
            sections.add(
                DonutSection(
                    color = portalOrange,
                    amount = currentAlias.emails_forwarded.toFloat() + emptyDonut
                )
            )

            if (currentAlias.emails_replied > 0) {
                sections.add(
                    DonutSection(
                        color = portalBlue,
                        amount = currentAlias.emails_replied.toFloat()
                    )
                )
            }

            if (currentAlias.emails_sent > 0) {
                sections.add(
                    DonutSection(
                        color = easternBlue,
                        amount = currentAlias.emails_sent.toFloat()
                    )
                )
            }

            if (currentAlias.emails_blocked > 0) {
                sections.add(
                    DonutSection(
                        color = softRed,
                        amount = currentAlias.emails_blocked.toFloat()
                    )
                )
            }

            val cap = sections.sumOf { it.amount.toInt() }.toFloat()
            if (cap > 0) {
                DonutModel(
                    cap = cap,
                    masterProgress = 1f,
                    gapWidthDegrees = 0f,
                    gapAngleDegrees = 270f,
                    strokeWidth = 16f,
                    backgroundLineColor = Color.Transparent,
                    sections = sections.sortedBy { it.amount }
                )
            } else {
                null
            }
        }

        if (donutModel != null) {
            DonutProgress(
                model = donutModel,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp)
            )
        }
    }

}