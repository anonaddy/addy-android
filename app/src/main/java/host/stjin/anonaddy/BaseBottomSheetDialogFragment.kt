package host.stjin.anonaddy

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    open fun dpToPx(dp: Int): Int {
        // https://developer.android.com/guide/practices/screens_support.html#dips-pels
        val density: Float = Resources.getSystem().displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            // Here, you might only want to apply the bottom inset to avoid extra padding on top or sides
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, v.paddingBottom + bottomInset.bottom)
            view.setOnApplyWindowInsetsListener(null)
            WindowInsetsCompat.CONSUMED
        }
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