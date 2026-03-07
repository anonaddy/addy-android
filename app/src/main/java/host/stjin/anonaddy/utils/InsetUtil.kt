package host.stjin.anonaddy.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

object InsetUtil {

    fun applyBottomInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val typesInset = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, typesInset.bottom)
            insets
        }
    }

    fun applyBottomMarginInset(view: View, originalMargin: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val typesInset = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = originalMargin + typesInset.bottom
            }
            insets
        }
    }
}