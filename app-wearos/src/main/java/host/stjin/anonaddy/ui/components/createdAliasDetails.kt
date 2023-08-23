package host.stjin.anonaddy.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.CreateAliasActivity
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.getAddyIoButtonColors

private val SPACING_ALIAS_BUTTONS = Dp(24f)
private val SPACING_BUTTONS = Dp(8f)

@ExperimentalWearMaterialApi
@Composable
fun CreatedAliasDetails(scalingLazyListState: ScalingLazyListState, alias: Aliases, context: Context, activity: CreateAliasActivity) {


    // Creates a CoroutineScope bound to the lifecycle
    val haptic = LocalHapticFeedback.current
    ScalingLazyColumnWithRSB(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        snap = false,
        state = scalingLazyListState
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
                        AddFavoriteLayout(alias = alias, context = context)
                        Spacer(modifier = Modifier.width(SPACING_BUTTONS))
                        ShowOnDeviceLayout(alias = alias, context = context, activity = activity)
                    }
                }
            }
        }
    }
}


@Composable
private fun AddFavoriteLayout(alias: Aliases, context: Context) {
    val haptic = LocalHapticFeedback.current
    var isAliasFavorite by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.tile_favorite_aliases_favorite)
            }
    ) {
        val favoriteAliasHelper by remember {
            mutableStateOf(FavoriteAliasHelper(context))
        }
        Button(
            colors = getAddyIoButtonColors(),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (isAliasFavorite) {
                    favoriteAliasHelper.removeAliasAsFavorite(alias.id)
                    isAliasFavorite = false
                } else {
                    if (!favoriteAliasHelper.addAliasAsFavorite(alias.id)) {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.max_favorites_reached),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        isAliasFavorite = true
                    }
                }

            },
            enabled = true,
        ) {
            Icon(
                painter = if (isAliasFavorite) painterResource(id = R.drawable.ic_starred) else painterResource(id = R.drawable.ic_star),
                contentDescription = context.getString(R.string.tile_favorite_aliases_favorite),
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