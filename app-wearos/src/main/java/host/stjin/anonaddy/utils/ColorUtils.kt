package host.stjin.anonaddy.utils

import android.content.Context
import androidx.core.content.ContextCompat
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.Aliases

object ColorUtils {
    fun getMostPopularColor(context: Context, aliases: Aliases): Int {
        val color1 = if (aliases.active) R.color.portalOrange else R.color.md_grey_500
        val color2 = if (aliases.active) R.color.portalBlue else R.color.md_grey_600
        val color3 = if (aliases.active) R.color.easternBlue else R.color.md_grey_700
        val color4 = if (aliases.active) R.color.softRed else R.color.md_grey_800

        val colorArray = arrayOf(
            arrayOf(aliases.emails_forwarded, ContextCompat.getColor(context, color1)),
            arrayOf(aliases.emails_replied, ContextCompat.getColor(context, color2)),
            arrayOf(aliases.emails_sent, ContextCompat.getColor(context, color3)),
            arrayOf(aliases.emails_blocked, ContextCompat.getColor(context, color4))
        ).maxByOrNull { it[0].toFloat() }

        return colorArray?.get(1) as Int
    }
}