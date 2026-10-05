package host.stjin.anonaddy.ui.base

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.RoundedCorner
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable


open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    open fun dpToPx(dp: Int): Int {
        // https://developer.android.com/guide/practices/screens_support.html#dips-pels
        val density: Float = Resources.getSystem().displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This makes sure the textfields move above the keyboard and matches device corner radius on supported devices
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyDeviceCornerRadius(insets)
            }

            // Here, you might only want to apply the bottom inset to avoid extra padding on top or sides
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottomInset.bottom)
            //view.setOnApplyWindowInsetsListener(null)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun applyDeviceCornerRadius(insetsCompat: WindowInsetsCompat) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val windowInsets = insetsCompat.toWindowInsets() ?: return
        val topLeftCorner = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
        val topRightCorner = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)

        val topLeftRadius = topLeftCorner?.radius?.toFloat() ?: 0f
        val topRightRadius = topRightCorner?.radius?.toFloat() ?: 0f

        val effectiveTopLeft = if (topLeftRadius > 0f) topLeftRadius else topRightRadius
        val effectiveTopRight = if (topRightRadius > 0f) topRightRadius else topLeftRadius

        if (effectiveTopLeft > 0f || effectiveTopRight > 0f) {
            val bottomSheet = dialog?.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return

            when (val background = bottomSheet.background) {
                is MaterialShapeDrawable -> {
                    background.shapeAppearanceModel = background.shapeAppearanceModel
                        .toBuilder()
                        .setTopLeftCornerSize(effectiveTopLeft)
                        .setTopRightCornerSize(effectiveTopRight)
                        .build()
                }
                is GradientDrawable -> {
                    background.mutate()
                    background.cornerRadii = floatArrayOf(
                        effectiveTopLeft, effectiveTopLeft,
                        effectiveTopRight, effectiveTopRight,
                        0f, 0f,
                        0f, 0f
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val configuration: Configuration = requireActivity().resources.configuration
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            configuration.screenWidthDp > 450
        ) {
            // you can go more fancy and vary the bottom sheet width depending on the screen width
            // see recommendations on https://material.io/components/sheets-bottom#specs
            dialog!!.window!!.setLayout(dpToPx(600), -1)
        }
    }

}
