package host.stjin.anonaddy.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.utils.ColorUtils
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.DateTimeUtils


@ExperimentalWearMaterialApi
@Composable
fun AliasList(aliases: List<Aliases>, scalingLazyListState: ScalingLazyListState, context: Context) {
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxWidth(),
        state = scalingLazyListState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { AliasActionRow(context = context) }
        items(aliases) { alias ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Chip(
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        secondaryContentColor = Color(ColorUtils.getMostPopularColor(context, alias))
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            text = alias.email
                        )
                    },
                    secondaryLabel = {
                        Text(
                            modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            text = (if (alias.description != null) {
                                alias.description
                            } else {
                                context.resources.getString(
                                    R.string.aliases_recyclerview_list_item_date_time,
                                    DateTimeUtils.convertStringToLocalTimeZoneString(
                                        alias.created_at,
                                        DateTimeUtils.DatetimeFormat.SHORT_DATE
                                    ),
                                    DateTimeUtils.convertStringToLocalTimeZoneString(
                                        alias.created_at,
                                        DateTimeUtils.DatetimeFormat.TIME
                                    )
                                )
                            }).toString()
                        )
                    },
                    onClick = {
                        if (!scalingLazyListState.isScrollInProgress) {
                            //haptic.performHapticFeedback(HapticFeedbackType.LongPress) VIBRATES IN ACTIVITY

                            val intent = Intent(context, ManageAliasActivity::class.java)
                            intent.putExtra("alias", alias.id)
                            context.startActivity(intent)
                        }
                    },
                )
                if (alias.pinned) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pinned),
                        contentDescription = context.resources.getString(R.string.pin),
                        modifier = Modifier
                            .padding(end = 12.dp, top = 8.dp)
                            .size(12.dp)
                            .alpha(0.6f),
                    )
                }
            }
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
fun PreviewAliasList() {
    val aliases = listOf(
        Aliases(
            id = "1",
            user_id = "1",
            aliasable_id = null,
            aliasable_type = null,
            local_part = "sample1",
            extension = null,
            domain = "anonaddy.me",
            email = "sample1@anonaddy.me",
            active = true,
            pinned = false,
            description = "Sample Description 1",
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
        ),
        Aliases(
            id = "2",
            user_id = "1",
            aliasable_id = null,
            aliasable_type = null,
            local_part = "sample2",
            extension = null,
            domain = "anonaddy.me",
            email = "sample2@anonaddy.me",
            active = true,
            pinned = true,
            description = null,
            from_name = null,
            attached_recipients_only = false,
            emails_forwarded = 5,
            emails_blocked = 1,
            emails_replied = 0,
            emails_sent = 0,
            recipients = null,
            last_forwarded = null,
            last_blocked = null,
            last_replied = null,
            last_sent = null,
            created_at = "2023-02-01 12:00:00",
            updated_at = "2023-02-01 12:00:00",
            deleted_at = null
        )
    )
    AppTheme {
        AliasList(
            aliases = aliases,
            scalingLazyListState = rememberScalingLazyListState(),
            context = LocalContext.current
        )
    }
}
