package host.stjin.anonaddy.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlin.math.abs
import kotlin.math.cos


class CustomScrollingLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {

    override fun onLayoutFinished(child: View, parent: RecyclerView) {
        val MAX_ICON_PROGRESS = 1f

        try {
            val centerOffset = child.height.toFloat() / 2.0f / parent.height.toFloat()
            val yRelativeToCenterOffset = child.y / parent.height + centerOffset

            // Normalize for center, adjusting to the maximum scale
            var progressToCenter = abs(0.5f - yRelativeToCenterOffset).coerceAtMost(MAX_ICON_PROGRESS)

            // Follow a curved path, rather than triangular!
            progressToCenter = cos(progressToCenter * Math.PI * 0.40f).toFloat()
            child.scaleX = progressToCenter
            child.scaleY = progressToCenter
            //child.alpha = abs(-0.5f - yRelativeToCenterOffset)
        } catch (ignored: Exception) {
        }
    }
}