package host.stjin.anonaddy.ui.components

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.CreateAliasActivity
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyButtonColors
import kotlinx.coroutines.launch

private val SPACING_ALIAS_BUTTONS = Dp(24f)
private val SPACING_BUTTONS = Dp(8f)

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalWearMaterialApi
@Composable
fun createdAliasDetails(lazyListState: LazyListState, alias: Aliases, context: Context, activity: CreateAliasActivity) {


    // Creates a CoroutineScope bound to the lifecycle
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
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
        horizontalAlignment = CenterHorizontally,
        state = lazyListState,
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
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}


@Composable
private fun AddFavoriteLayout(alias: Aliases, context: Context) {
    Log.e("ANONDEBUG12", "AddFavoriteLayout()")

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
            colors = getAnonAddyButtonColors(),
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
    Log.e("ANONDEBUG12", "ShowOnDeviceLayout()")

    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.show_on_paired_device)
            }
    ) {
        Button(
            colors = getAnonAddyButtonColors(),
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