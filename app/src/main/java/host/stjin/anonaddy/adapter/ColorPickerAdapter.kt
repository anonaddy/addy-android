package host.stjin.anonaddy.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R

class ColorPickerAdapter(var context: Context, private val colors: List<String>) : RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    lateinit var onColorClickListener: ClickListener
    var selectedColor: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.label_color_list_item, parent, false)
        return ViewHolder(v)
    }

    interface ClickListener {
        fun onClick(pos: Int, color: String)
    }

    fun setClickListener(aClickListener: ClickListener) {
        onColorClickListener = aClickListener
    }

    override fun getItemCount(): Int = colors.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val colorHex = colors[position]
            holder.animateImage(selectedColor == colorHex)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val colorHex = colors[position]

        try {
            val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.parseColor(colorHex))
            holder.icon.setImageBitmap(bitmap)
            holder.icon.setBackgroundColor(Color.TRANSPARENT)
        } catch (e: Exception) {
            holder.iconLl.visibility = View.GONE
        }

        // Just set the state instantly if not a payload update
        if (selectedColor == colorHex) {
            holder.iconMotionLayout.progress = 1f
        } else {
            holder.iconMotionLayout.progress = 0f
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var iconLl: LinearLayout = itemView.findViewById(R.id.label_color_list_item_icon_LL)
        var icon: ImageFilterView = itemView.findViewById(R.id.label_color_list_item_icon)
        var iconMotionLayout: MotionLayout = itemView.findViewById(R.id.label_color_list_item_icon_ML)

        init {
            icon.setOnClickListener(this)
        }

        fun animateImage(enabled: Boolean) {
            if (enabled) {
                iconMotionLayout.transitionToEnd()
            } else {
                iconMotionLayout.transitionToStart()
            }
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.label_color_list_item_icon -> {
                    val prevSelected = selectedColor
                    selectedColor = colors[adapterPosition]
                    
                    // Trigger re-bind to animate new selection and old selection
                    if (prevSelected != null) {
                        notifyItemChanged(colors.indexOf(prevSelected), true)
                    }
                    notifyItemChanged(adapterPosition, true)

                    if (::onColorClickListener.isInitialized) {
                        onColorClickListener.onClick(adapterPosition, selectedColor!!)
                    }
                }
            }
        }
    }
}
