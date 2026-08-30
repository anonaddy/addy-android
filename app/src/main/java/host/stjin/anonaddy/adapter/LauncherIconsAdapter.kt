package host.stjin.anonaddy.adapter

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.AppearanceIconsListItemBinding
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper

class LauncherIconsAdapter(private val context: Context) : RecyclerView.Adapter<LauncherIconsAdapter.ViewHolder>() {

    var onIconClickListener: ClickListener? = null
    private val launcherIconController = LauncherIconController(context)

    var launcherIcons = LauncherIconController.LauncherIcon.entries.toTypedArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AppearanceIconsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    interface ClickListener {
        fun onClick(pos: Int, view: View)
    }

    fun setClickListener(listener: ClickListener) {
        onIconClickListener = listener
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
        holder.binding.appearanceIconListItemName.text = context.resources.getString(launcherIcons[position].title)

        try {
            if (isColorResource(launcherIcons[position].background)) {
                holder.binding.appearanceIconListItemIcon.setBackgroundColor(ContextCompat.getColor(context, launcherIcons[position].background))
            } else {
                holder.binding.appearanceIconListItemIcon.setBackgroundResource(launcherIcons[position].background)
            }
        } catch (e: Exception) {
            holder.binding.appearanceIconListItemIconLL.visibility = View.GONE
            LoggingHelper(context).addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "LauncherIconsAdapter;onBindViewHolder", null)
        }

        holder.binding.appearanceIconListItemIcon.setImageResource(launcherIcons[position].foreground)
        holder.animateImage(launcherIconController.isEnabled(launcherIcons[position]))
    }

    fun getItem(pos: Int): LauncherIconController.LauncherIcon {
        return launcherIcons[pos]
    }

    inner class ViewHolder(val binding: AppearanceIconsListItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.appearanceIconListItemIcon.setOnClickListener(this)
        }

        fun animateImage(enabled: Boolean) {
            if (enabled) {
                binding.appearanceIconListItemIconML.transitionToEnd()
            } else {
                binding.appearanceIconListItemIconML.transitionToStart()
            }
        }

        override fun onClick(v: View) {
            if (v.id == binding.appearanceIconListItemIcon.id) {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                onIconClickListener?.onClick(pos, v)
                launcherIconController.setIcon(launcherIcons[pos])
            }
        }
    }
}