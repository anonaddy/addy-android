package host.stjin.anonaddy.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import host.stjin.anonaddy.databinding.LabelColorListItemBinding

class ColorPickerAdapter(private val colors: List<String>) : RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    var onColorClickListener: ClickListener? = null
    var selectedColor: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LabelColorListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    interface ClickListener {
        fun onClick(pos: Int, color: String)
    }

    fun setClickListener(listener: ClickListener) {
        onColorClickListener = listener
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
            val bitmap = createBitmap(1, 1)
            bitmap.eraseColor(colorHex.toColorInt())
            holder.binding.labelColorListItemIcon.setImageBitmap(bitmap)
            holder.binding.labelColorListItemIcon.setBackgroundColor(Color.TRANSPARENT)
        } catch (e: Exception) {
            holder.binding.labelColorListItemIconLL.visibility = View.GONE
        }

        // Just set the state instantly if not a payload update
        if (selectedColor == colorHex) {
            holder.binding.labelColorListItemIconML.progress = 1f
        } else {
            holder.binding.labelColorListItemIconML.progress = 0f
        }
    }

    inner class ViewHolder(val binding: LabelColorListItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.labelColorListItemIcon.setOnClickListener(this)
        }

        fun animateImage(enabled: Boolean) {
            if (enabled) {
                binding.labelColorListItemIconML.transitionToEnd()
            } else {
                binding.labelColorListItemIconML.transitionToStart()
            }
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.label_color_list_item_icon -> {
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) return
                    val prevSelected = selectedColor
                    val newColor = colors[pos]
                    selectedColor = newColor

                    // Trigger re-bind to animate new selection and old selection
                    if (prevSelected != null) {
                        notifyItemChanged(colors.indexOf(prevSelected), true)
                    }
                    notifyItemChanged(pos, true)

                    onColorClickListener?.onClick(pos, newColor)
                }
            }
        }
    }
}
