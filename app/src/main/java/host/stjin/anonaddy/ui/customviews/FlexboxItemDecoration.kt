package host.stjin.anonaddy.ui.customviews

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager

class FlexboxItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val layoutManager = parent.layoutManager as? FlexboxLayoutManager ?: return
        val position = parent.getChildAdapterPosition(view)

        val lines = layoutManager.flexLines
        var lineIndex = -1
        var itemsInCurrentLine = 0
        var currentLineItemCount = 0

        for ((index, line) in lines.withIndex()) {
            itemsInCurrentLine += line.itemCount
            if (position < itemsInCurrentLine) {
                lineIndex = index
                currentLineItemCount = line.itemCount
                break
            }
        }


        if (lineIndex != -1) {
            // Add top margin for all items except the first line
            if (lineIndex > 0) {
                outRect.top = spacing
            }

            val itemIndexInLine = position - (itemsInCurrentLine - currentLineItemCount)
            if (itemIndexInLine < currentLineItemCount - 1) {
                outRect.right = spacing
            }
        }
    }
}