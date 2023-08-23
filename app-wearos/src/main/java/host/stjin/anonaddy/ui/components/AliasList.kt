package host.stjin.anonaddy.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
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
import host.stjin.anonaddy_shared.utils.DateTimeUtils


@ExperimentalWearMaterialApi
@Composable
fun AliasList(aliases: List<Aliases>, favoriteAliases: List<String>?, scalingLazyListState: ScalingLazyListState, context: Context) {
    fun getStarIcon(aliases: Aliases, favoriteAliases: List<String>?): Int {
        return if (favoriteAliases?.contains(aliases.id) == true) {
            R.drawable.ic_starred
        } else {
            R.drawable.ic_star
        }
    }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        state = scalingLazyListState,
        snap = false,
    ) {
        item { AliasActionRow(context = context) }
        items(aliases) { alias ->
            Chip(
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    secondaryContentColor = Color(ColorUtils.getMostPopularColor(context, alias))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                icon = {
                    Icon(
                        painter = painterResource(id = getStarIcon(alias, favoriteAliases)),
                        tint = if (favoriteAliases
                                ?.contains(alias.id) == true
                        ) Color(ColorUtils.getMostPopularColor(context, alias)) else Color.White,
                        contentDescription = context.resources.getString(R.string.favorite),
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center),
                    )
                },
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
                                DateTimeUtils.turnStringIntoLocalString(
                                    alias.created_at,
                                    DateTimeUtils.DATETIMEUTILS.SHORT_DATE
                                ),
                                DateTimeUtils.turnStringIntoLocalString(
                                    alias.created_at,
                                    DateTimeUtils.DATETIMEUTILS.TIME
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
        }
    }


}