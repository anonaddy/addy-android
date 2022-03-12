package host.stjin.anonaddy_shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.ButtonDefaults.buttonColors
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults.chipColors
import androidx.wear.compose.material.InlineSliderColors
import androidx.wear.compose.material.InlineSliderDefaults.colors
import androidx.wear.compose.material.ToggleChipColors
import androidx.wear.compose.material.ToggleChipDefaults.toggleChipColors
import host.stjin.anonaddy_shared.R

/**
 * Why maintain 2 color palettes if you can use 1? ;D
 */

@Composable
private fun getLightThemeColors(): ColorScheme {
    return lightColorScheme(
        primary = colorResource(id = R.color.md_theme_primary),
        onPrimary = colorResource(id = R.color.md_theme_onPrimary),
        primaryContainer = colorResource(id = R.color.md_theme_primaryContainer),
        onPrimaryContainer = colorResource(id = R.color.md_theme_onPrimaryContainer),
        secondary = colorResource(id = R.color.md_theme_secondary),
        onSecondary = colorResource(id = R.color.md_theme_onSecondary),
        secondaryContainer = colorResource(id = R.color.md_theme_secondaryContainer),
        onSecondaryContainer = colorResource(id = R.color.md_theme_onSecondaryContainer),
        tertiary = colorResource(id = R.color.md_theme_tertiary),
        onTertiary = colorResource(id = R.color.md_theme_onTertiary),
        tertiaryContainer = colorResource(id = R.color.md_theme_tertiaryContainer),
        onTertiaryContainer = colorResource(id = R.color.md_theme_onTertiaryContainer),
        error = colorResource(id = R.color.md_theme_error),
        errorContainer = colorResource(id = R.color.md_theme_errorContainer),
        onError = colorResource(id = R.color.md_theme_onError),
        onErrorContainer = colorResource(id = R.color.md_theme_onErrorContainer),
        background = colorResource(id = R.color.md_theme_background),
        onBackground = colorResource(id = R.color.md_theme_onBackground),
        surface = colorResource(id = R.color.md_theme_surface),
        onSurface = colorResource(id = R.color.md_theme_onSurface),
        surfaceVariant = colorResource(id = R.color.md_theme_surfaceVariant),
        onSurfaceVariant = colorResource(id = R.color.md_theme_onSurfaceVariant),
        outline = colorResource(id = R.color.md_theme_outline),
        inverseOnSurface = colorResource(id = R.color.md_theme_inverseOnSurface),
        inverseSurface = colorResource(id = R.color.md_theme_inverseSurface),
        inversePrimary = colorResource(id = R.color.md_theme_primaryInverse),
        //shadow = md_theme_light_shadow,
    )
}

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getLightThemeColors(),
        typography = AppTypography,
        content = content
    )
}

@Composable
fun getAnonAddyChipColors(): ChipColors {
    return chipColors(
        backgroundColor = colorResource(id = R.color.md_theme_secondaryContainer),
        contentColor = colorResource(id = R.color.md_theme_onSecondaryContainer),
        secondaryContentColor = colorResource(id = R.color.md_theme_onSecondaryContainer),
        iconTintColor = colorResource(id = R.color.md_theme_onSecondaryContainer)
    )
}

@Composable
fun getAnonAddyDangerChipColors(): ChipColors {
    return chipColors(
        backgroundColor = colorResource(id = R.color.md_theme_errorContainer),
        contentColor = colorResource(id = R.color.md_theme_onErrorContainer),
        secondaryContentColor = colorResource(id = R.color.md_theme_onErrorContainer),
        iconTintColor = colorResource(id = R.color.md_theme_onErrorContainer)
    )
}

@Composable
fun getAnonAddyInlineSliderColors(): InlineSliderColors {
    return colors(
        selectedBarColor = colorResource(id = R.color.md_theme_secondaryContainer),
    )
}

@Composable
fun getAnonAddyButtonColors(): ButtonColors {
    return buttonColors(
        backgroundColor = colorResource(id = R.color.md_theme_secondaryContainer),
        contentColor = colorResource(id = R.color.md_theme_onSecondaryContainer)
    )
}

@Composable
fun getAnonAddyToggleChipColors(): ToggleChipColors {
    return toggleChipColors(
        checkedEndBackgroundColor = colorResource(id = R.color.md_theme_primaryInverse).copy(alpha = 0.30f),
        checkedToggleControlTintColor = colorResource(id = R.color.md_theme_onPrimary)
    )
}
