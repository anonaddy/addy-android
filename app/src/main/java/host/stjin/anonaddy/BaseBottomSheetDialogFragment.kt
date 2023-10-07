package host.stjin.anonaddy

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    @RequiresApi(Build.VERSION_CODES.R)
    fun setIMEAnimation(linearLayout: LinearLayout) {
        val inputLayoutMarginBottom = linearLayout.marginBottom
        val callback =
            object : WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(insets: WindowInsets, animations: MutableList<WindowInsetsAnimation>): WindowInsets {
                    linearLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        updateMargins(
                            bottom = inputLayoutMarginBottom +
                                    insets.getInsets(WindowInsets.Type.ime()).bottom
                        )
                    }
                    return insets
                }
            }
        linearLayout.setWindowInsetsAnimationCallback(callback)
    }

    open fun dpToPx(dp: Int): Int {
        // https://developer.android.com/guide/practices/screens_support.html#dips-pels
        val density: Float = Resources.getSystem().displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    override fun onResume() {
        super.onResume()
        val configuration: Configuration = requireActivity().resources.configuration
        if (configuration.orientation === Configuration.ORIENTATION_LANDSCAPE &&
            configuration.screenWidthDp > 450
        ) {
            // you can go more fancy and vary the bottom sheet width depending on the screen width
            // see recommendations on https://material.io/components/sheets-bottom#specs
            dialog!!.window!!.setLayout(dpToPx(600), -1)
        }
    }

}