package host.stjin.anonaddy

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
        val callback = @RequiresApi(Build.VERSION_CODES.R)
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

}