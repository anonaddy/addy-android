package host.stjin.anonaddy.utils

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedDispatcher
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.CustomToolbarOneHandedBinding

object ToolbarUtils {

    fun changeTopBarSubTitle(subtitle: TextView, title: TextView, text: String?) {
        // Prevent lagging animation by not setting text multiple times
        if (subtitle.text == text || (subtitle.text.isNullOrEmpty() && text == null)) {
            return
        }

        if (text == null) {
            ObjectAnimator.ofFloat(title, "translationY", 0f).apply {
                duration = 200
                start()
            }

            ObjectAnimator.ofFloat(subtitle, "alpha", 0f).apply {
                duration = 200
                start()
            }
        } else {
            ObjectAnimator.ofFloat(title, "translationY", -subtitle.measuredHeight.toFloat()).apply {
                duration = 200
                start()
            }
            ObjectAnimator.ofFloat(subtitle, "alpha", 0.7f).apply {
                duration = 200
                start()
            }
        }

        subtitle.text = text
    }

    fun shimmerTopBarSubTitle(shimmerFrameLayout: ShimmerFrameLayout, shimmer: Boolean) {
        if (shimmer) {
            shimmerFrameLayout.startShimmer()
        } else {
            shimmerFrameLayout.stopShimmer()
        }
    }

    fun setupToolbar(
        context: Context,
        onBackPressedDispatcher: OnBackPressedDispatcher,
        title: Int,
        nestedScrollView: NestedScrollView?,
        customToolbarOneHandedBinding: CustomToolbarOneHandedBinding? = null,
        image: Int? = null,
        customBackPressedMethod: (() -> Unit)? = null,
        showBackButton: Boolean = true,
        onAppBarLayoutBound: (AppBarLayout) -> Unit
    ) {
        customToolbarOneHandedBinding?.apply {
            if (showBackButton) {
                customToolbarOneHandedMaterialtoolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            }

            customToolbarOneHandedMaterialtoolbar.setNavigationOnClickListener {
                customBackPressedMethod?.invoke() ?: onBackPressedDispatcher.onBackPressed()
            }
            customToolbarOneHandedMaterialtoolbar.title = context.getString(title)

            if (image != null) {
                customToolbarOneHandedImage.setImageDrawable(ContextCompat.getDrawable(context, image))
            }

            customToolbarOneHandedMaterialtoolbar.setOnClickListener {
                nestedScrollView?.let { nsv -> nsv.post { nsv.smoothScrollTo(0, 0) } }
                customToolbarAppbar.setExpanded(true, true)
            }

            onAppBarLayoutBound(customToolbarAppbar)
        }
    }

    fun setupToolbarAction(button: MaterialButton, icon: Int, onClickListener: View.OnClickListener?) {
        button.apply {
            visibility = View.VISIBLE
            setIconResource(icon)

            animate().alpha(if (onClickListener != null) 1.0f else 0.0f)
            setOnClickListener(onClickListener)
        }
    }
}
