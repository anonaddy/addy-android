package host.stjin.anonaddy.utils

import android.content.Context


object ScreenSizeUtils {
    fun calculateNoOfColumns(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        // Where 360 is the width of your grid item. You can change it as per your convention.
        var columns = (dpWidth / 360).toInt()
        if (columns == 0) columns = 1
        return columns
    }
}