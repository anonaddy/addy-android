package host.stjin.anonaddy.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.CreateAliasActivity
import host.stjin.anonaddy.ui.settings.SettingsActivity
import host.stjin.anonaddy_shared.ui.theme.getAnonAddyButtonColors

private val SPACING_BUTTONS = Dp(8f)
private val SPACING_ALIAS_BUTTONS = Dp(8f)

@Composable
fun AliasActionRow(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Dp(48f)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            ShowOnNewAliasLayout(context)
            Spacer(modifier = Modifier.width(SPACING_BUTTONS))
            ShowOnSettingsLayout(context)
        }
        Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
    }

}


@Composable
private fun ShowOnNewAliasLayout(context: Context) {
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.add_alias)
            }
    ) {
        Button(
            colors = getAnonAddyButtonColors(),
            onClick = {
                val intent = Intent(context, CreateAliasActivity::class.java)
                context.startActivity(intent)
            },
            enabled = true,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = context.getString(R.string.add_alias),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    }
}

@Composable
private fun ShowOnSettingsLayout(context: Context) {
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.settings)
            }
    ) {
        Button(
            colors = getAnonAddyButtonColors(),
            onClick = {
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            },
            enabled = true,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = context.getString(R.string.settings),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    }
}