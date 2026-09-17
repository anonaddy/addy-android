package host.stjin.anonaddy.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.todkars.shimmer.ShimmerRecyclerView
import host.stjin.anonaddy.R

object ScreenSizeUtils {

    /**
     * Calculates available content width in dp by subtracting system insets, navigation rail,
     * and layout paddings from the total screen width.
     */
    fun getAvailableContentWidthDp(screenWidthDp: Int, isTablet: Boolean): Int {
        // In tablet mode, navigation rail (96dp) + layout end padding (16dp) + fragment padding (32dp) = 144dp
        // In phone mode: fragment padding (32dp)
        val insetsDp = if (isTablet) 144 else 32
        return (screenWidthDp - insetsDp).coerceAtLeast(0)
    }

    /**
     * Calculates the number of columns that can fit in the available width in dp.
     *
     * - Narrow windows / split-screen (< 420dp available): 1 column
     * - Tablet / Foldable Unfolded mode (isTablet == true, sw >= 600dp):
     *     - < 1000dp: 2 columns (Foldables unfolded in portrait & landscape, Surface Duo spanned, portrait tablets)
     *     - 1000dp ..< 1400dp: 3 columns (10"-11" tablets in landscape)
     *     - >= 1400dp: 4 columns (Large 12.4"+ tablets, desktop/Chromebook)
     * - Phone / Foldable Folded mode (isTablet == false, sw < 600dp):
     *     - < 550dp: 1 column (Phones in portrait, foldable cover screen, Surface Duo single screen)
     *     - 550dp ..< 900dp: 2 columns (Phones in landscape)
     *     - >= 900dp: 3 columns
     */
    fun calculateNoOfColumns(availableWidthDp: Int, isTablet: Boolean = false, minItemWidthDp: Int? = null): Int {
        if (minItemWidthDp != null) {
            var cols = availableWidthDp / minItemWidthDp
            if (cols < 1) cols = 1
            return cols
        }

        // Narrow windows or split-screen (< 420dp available) always use 1 column
        if (availableWidthDp < 420) {
            return 1
        }

        // When in tablet / foldable unfolded mode (sw >= 600dp)
        if (isTablet) {
            return when {
                availableWidthDp < 1000 -> 2
                availableWidthDp < 1400 -> 3
                else -> 4
            }
        }

        // When in phone / foldable folded mode (sw < 600dp)
        return when {
            availableWidthDp < 550 -> 1
            availableWidthDp < 900 -> 2
            else -> 3
        }
    }

    /**
     * Calculates the number of grid columns for the current window context.
     * Takes into account whether the device is in tablet mode (sw600dp with navigation rail).
     */
    fun calculateNoOfColumns(context: Context, minItemWidthDp: Int? = null): Int {
        val isTablet = context.resources.getBoolean(R.bool.isTablet)
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = context.resources.configuration.screenWidthDp.takeIf { it > 0 }
            ?: (displayMetrics.widthPixels / displayMetrics.density).toInt()
        val availableWidthDp = getAvailableContentWidthDp(screenWidthDp, isTablet)
        return calculateNoOfColumns(availableWidthDp, isTablet, minItemWidthDp)
    }

    /**
     * Calculates the number of grid columns based on the actual measured pixel width of the RecyclerView.
     */
    fun calculateNoOfColumnsForWidth(context: Context, widthPx: Int, minItemWidthDp: Int? = null): Int {
        val density = context.resources.displayMetrics.density
        if (density <= 0f || widthPx <= 0) return calculateNoOfColumns(context, minItemWidthDp)
        val isTablet = context.resources.getBoolean(R.bool.isTablet)
        val widthDp = (widthPx / density).toInt()
        return calculateNoOfColumns(widthDp, isTablet, minItemWidthDp)
    }

    /**
     * Automatically adjusts the spanCount of a RecyclerView's GridLayoutManager
     * (and its ShimmerRecyclerView counterpart if applicable) as the view's layout size changes
     * (e.g. folding/unfolding, rotation, or multi-window resizing).
     */
    fun setupAutoFitGrid(
        recyclerView: RecyclerView,
        minItemWidthDp: Int? = null
    ) {
        val context = recyclerView.context
        val initialColumns = calculateNoOfColumns(context, minItemWidthDp)

        // Set initial spanCount if layoutManager is already a GridLayoutManager
        (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = initialColumns
        ((recyclerView as? ShimmerRecyclerView)?.shimmerLayoutManager as? GridLayoutManager)?.spanCount = initialColumns

        recyclerView.addOnLayoutChangeListener { v, left, _, right, _, oldLeft, _, oldRight, _ ->
            val width = (right - left) - v.paddingLeft - v.paddingRight
            val oldWidth = (oldRight - oldLeft)
            if (width > 0 && width != oldWidth) {
                val newColumns = calculateNoOfColumnsForWidth(context, width, minItemWidthDp)
                recyclerView.post {
                    if (!recyclerView.isAttachedToWindow) return@post
                    val glm = recyclerView.layoutManager as? GridLayoutManager
                    if (glm != null && glm.spanCount != newColumns) {
                        glm.spanCount = newColumns
                    }
                    val slm = (recyclerView as? ShimmerRecyclerView)?.shimmerLayoutManager as? GridLayoutManager
                    if (slm != null && slm.spanCount != newColumns) {
                        slm.spanCount = newColumns
                    }
                }
            }
        }
    }
}