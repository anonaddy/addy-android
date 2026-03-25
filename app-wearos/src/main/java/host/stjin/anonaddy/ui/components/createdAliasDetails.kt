package host.stjin.anonaddy.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.CreateAliasActivity
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.ui.theme.getAddyIoButtonColors

private val SPACING_ALIAS_BUTTONS = Dp(24f)
private val SPACING_BUTTONS = Dp(8f)

@ExperimentalWearMaterialApi
@Composable
fun CreatedAliasDetails(scalingLazyListState: ScalingLazyListState, alias: Aliases, context: Context, activity: CreateAliasActivity) {


    // Creates a CoroutineScope bound to the lifecycle
    val haptic = LocalHapticFeedback.current
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxWidth(),
        state = scalingLazyListState,
        horizontalAlignment = CenterHorizontally
    ) {
        item {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
                    Text(alias.email, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
                    Row {
                        PinLayout(alias = alias, context = context, activity = activity)
                        Spacer(modifier = Modifier.width(SPACING_BUTTONS))
                        ShowOnDeviceLayout(alias = alias, context = context, activity = activity)
                    }
                }
            }
        }
    }
}


@Composable
private fun PinLayout(alias: Aliases, context: Context, activity: CreateAliasActivity) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.tile_pinned_aliases_pin)
            }
    ) {

        Button(
            colors = getAddyIoButtonColors(),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                val intent = Intent(context, ManageAliasActivity::class.java)
                intent.putExtra("alias", alias.id)
                intent.putExtra("pinAlias", true)
                context.startActivity(intent)
                activity.finish()

            },
            enabled = true,
        ) {
            Icon(
                painter = if (alias.pinned) painterResource(id = R.drawable.ic_pinned) else painterResource(id = R.drawable.ic_pinned_off),
                contentDescription = context.getString(R.string.tile_pinned_aliases_pin),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    }
}

@Composable
private fun ShowOnDeviceLayout(alias: Aliases, context: Context, activity: CreateAliasActivity) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.show_on_paired_device)
            }
    ) {
        Button(
            colors = getAddyIoButtonColors(),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                val intent = Intent(context, ManageAliasActivity::class.java)
                intent.putExtra("alias", alias.id)
                intent.putExtra("showOnPairedDevice", true)
                context.startActivity(intent)
                activity.finish()
            },
            enabled = true,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_devices),
                contentDescription = context.getString(R.string.show_on_paired_device),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    }
}

@ExperimentalWearMaterialApi
@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun PreviewCreatedAliasDetails() {
    val context = LocalContext.current
    val alias = Aliases(
        id = "1",
        user_id = "1",
        aliasable_id = null,
        aliasable_type = null,
        local_part = "newalias",
        extension = null,
        domain = "anonaddy.me",
        email = "newalias@anonaddy.me",
        active = true,
        pinned = false,
        description = null,
        from_name = null,
        attached_recipients_only = false,
        emails_forwarded = 0,
        emails_blocked = 0,
        emails_replied = 0,
        emails_sent = 0,
        recipients = null,
        last_forwarded = null,
        last_blocked = null,
        last_replied = null,
        last_sent = null,
        created_at = "2023-01-01 00:00:00",
        updated_at = "2023-01-01 00:00:00",
        deleted_at = null
    )
    AppTheme {
        CreatedAliasDetails(
            scalingLazyListState = rememberScalingLazyListState(),
            alias = alias,
            context = context,
            activity = CreateAliasActivity()
        )
    }
}
