package host.stjin.anonaddy.utils

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.ColorInt

object ThemeUtils {
    @ColorInt
    fun getDeviceAccentColor(context: Context): Int {
        val value = TypedValue()
        val ctx = ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault)
        ctx.theme.resolveAttribute(android.R.attr.colorAccent, value, true)
        return value.data
    }
}