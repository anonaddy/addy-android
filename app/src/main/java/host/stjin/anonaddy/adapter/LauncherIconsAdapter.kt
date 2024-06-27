package host.stjin.anonaddy.adapter

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper


class LauncherIconsAdapter(var context: Context) : RecyclerView.Adapter<LauncherIconsAdapter.ViewHolder>() {

    lateinit var onIconClickListener: ClickListener

    var launcherIcons = LauncherIconController.LauncherIcon.entries.toTypedArray()
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

    private fun isColorResource(value: Int): Boolean {
        return try {
            ResourcesCompat.getColor(context.resources, value, null)
            true
        } catch (e: Resources.NotFoundException) {
            false
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // set the data in items
        holder.name.text = context.resources.getString(launcherIcons[position].title)

        val launcherIconController = LauncherIconController(context)


        /*
        I noticed some MIUI devices crash when loading the resources for Launchericons. Hence the try-catch and hiding the icon
         */
        try {
            if (isColorResource(launcherIcons[position].background)) {
                holder.icon.setBackgroundColor(ContextCompat.getColor(context, launcherIcons[position].background))
            } else {
                holder.icon.setBackgroundResource(launcherIcons[position].background)
            }
        } catch (e: Exception) {
            holder.iconLl.visibility = View.GONE
            LoggingHelper(context).addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "LauncherIconsAdapter;onBindViewHolder", null)
        }

        holder.icon.setImageResource(launcherIcons[position].foreground)

        holder.animateImage(launcherIconController.isEnabled(launcherIcons[position]))
    }

    fun getItem(pos: Int): LauncherIconController.LauncherIcon {
        return launcherIcons[pos]
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // init the item view's
        // get the reference of item view's
        var iconLl: LinearLayout = itemView.findViewById<View>(R.id.appearance_icon_list_item_icon_LL) as LinearLayout
        var icon: ImageFilterView = itemView.findViewById<View>(R.id.appearance_icon_list_item_icon) as ImageFilterView
        var name: TextView = itemView.findViewById<View>(R.id.appearance_icon_list_item_name) as TextView
        private var iconMotionLayout: MotionLayout = itemView.findViewById<View>(R.id.appearance_icon_list_item_icon_ML) as MotionLayout

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
                R.id.appearance_icon_list_item_icon -> {
                    onIconClickListener.onClick(adapterPosition, p0)
                    LauncherIconController(context).setIcon(launcherIcons[adapterPosition])
                }
            }
        }
    }

}