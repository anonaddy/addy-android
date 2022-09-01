package host.stjin.anonaddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.RelativeCornerSize
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.controllers.LauncherIconController


class LauncherIconsAdapter(context: Context) : RecyclerView.Adapter<LauncherIconsAdapter.ViewHolder>() {

    lateinit var onIconClickListener: ClickListener

    var context: Context
    var launcherIcons = LauncherIconController.LauncherIcon.values()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // infalte the item Layout
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.appearance_icons_list_item, parent, false)
        // set the view's size, margins, paddings and layout parameters
        return ViewHolder(v)
    }

    interface ClickListener {
        fun onClick(pos: Int, aView: View)
    }

    fun setClickListener(aClickListener: ClickListener) {
        onIconClickListener = aClickListener
    }


    override fun getItemCount(): Int = launcherIcons.size


    private val roundedCornerRadius = 0.15f
    private val selectedRadius = 0.5f
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // set the data in items
        holder.name.text = context.resources.getString(launcherIcons[position].title)

        val launcherIconController = LauncherIconController(context)
        holder.icon.setBackgroundColor(ContextCompat.getColor(context, launcherIcons[position].background))
        holder.icon.setImageResource(launcherIcons[position].foreground)

        var cornerRadius = roundedCornerRadius
        if (launcherIconController.isEnabled(launcherIcons[position])) {
            cornerRadius = selectedRadius
        }
        updateImage(holder, cornerRadius)
    }

    private fun updateImage(holder: ViewHolder, radius: Float) {
        // set appearance
        holder.icon.shapeAppearanceModel = holder.icon.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(
                RelativeCornerSize(radius)
            )
            .build()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // init the item view's
        var icon: ShapeableImageView
        var name: TextView

        init {
            // get the reference of item view's
            icon = itemView.findViewById<View>(R.id.appearance_icon_list_item_icon) as ShapeableImageView
            name = itemView.findViewById<View>(R.id.appearance_icon_list_item_name) as TextView

            icon.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.appearance_icon_list_item_icon -> {
                    LauncherIconController(context).setIcon(launcherIcons[adapterPosition])
                    onIconClickListener.onClick(adapterPosition, p0)
                }
            }
        }
    }

    init {
        this.context = context
    }

}