package host.stjin.anonaddy.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R

class MarginItemDecoration(private val spaceSize: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with(outRect) {
            if (parent.context.resources.getBoolean(R.bool.isTablet)) {
                top = spaceSize / 2
                left = spaceSize / 2
                right = spaceSize / 2

                bottom = spaceSize / 2
            } else {
                top = if (parent.getChildAdapterPosition(view) == 0) {
                    0
                } else {
                    spaceSize / 2
                }
                left = spaceSize / 2
                right = spaceSize / 2

                bottom = if (parent.getChildAdapterPosition(view) == (parent.adapter?.itemCount ?: 0) - 1) {
                    0
                } else {
                    spaceSize / 2
                }
            }

        }
    }
}