package host.stjin.anonaddy.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy_shared.ui.theme.AppTheme

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun ErrorScreen(context: Context, text: String, leadingText: String? = null) {
    AppTheme {
        Scaffold(
            modifier = Modifier,
            timeText = {
                CustomTimeText(
                    visible = true,
                    showLeadingText = true,
                    leadingText = leadingText ?: context.resources.getString(R.string.app_name)
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(context.resources.getString(R.string.whoops), fontSize = 30.sp, textAlign = TextAlign.Center)
                    Text(text, color = MaterialTheme.colors.error, textAlign = TextAlign.Center)
                }
            }
        }


    }
}