package host.stjin.anonaddy.utils

import android.content.Context
import android.content.res.Resources.Theme
import android.util.TypedValue


object AttributeHelper {
    fun getValueByAttr(context: Context, resId: Int): Int {
        val typedValue = TypedValue()
        val theme: Theme = context.theme
        theme.resolveAttribute(resId, typedValue, true)
        return typedValue.data
    }
}