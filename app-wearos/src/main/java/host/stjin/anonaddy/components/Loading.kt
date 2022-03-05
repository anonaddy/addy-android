package host.stjin.anonaddy.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import host.stjin.anonaddy.R

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun Loading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        androidx.wear.compose.material.CircularProgressIndicator(
            indicatorColor = colorResource(id = R.color.md_theme_primaryContainer),
            trackColor = colorResource(id = R.color.md_theme_onPrimaryContainer)
        )
    }
}