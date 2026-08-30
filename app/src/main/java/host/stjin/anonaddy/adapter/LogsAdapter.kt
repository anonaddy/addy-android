package host.stjin.anonaddy.adapter

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.LogsRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.Logs

class LogDiffCallback : DiffUtil.ItemCallback<Logs>() {
    override fun areItemsTheSame(oldItem: Logs, newItem: Logs): Boolean {
        return oldItem.dateTime == newItem.dateTime && oldItem.message == newItem.message
    }

    override fun areContentsTheSame(oldItem: Logs, newItem: Logs): Boolean {
        return oldItem == newItem
    }
}

class LogsAdapter(
    listWithLogs: List<Logs> = emptyList(),
    private var onLogLayoutClickListener: ClickListener? = null
) : ListAdapter<Logs, LogsAdapter.ViewHolder>(LogDiffCallback()) {

    init {
        if (listWithLogs.isNotEmpty()) {
            submitList(listWithLogs)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LogsRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.logsRecyclerviewDate.text = item.dateTime
        holder.binding.logsRecyclerviewMessage.text = item.message

        val colorRes = when (item.importance) {
            0 -> R.color.logImportanceCritical
            1 -> R.color.logImportanceWarning
            else -> R.color.logImportanceInfo
        }

        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        val filter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        for (drawable in holder.binding.logsRecyclerviewMessage.compoundDrawablesRelative) {
            drawable?.colorFilter = filter
        }
    }

    fun setClickListener(listener: ClickListener) {
        onLogLayoutClickListener = listener
    }



    interface ClickListener {
        fun onClickDetails(pos: Int, view: View)
    }

    inner class ViewHolder(val binding: LogsRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.logsRecyclerviewLl.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                binding.logsRecyclerviewLl.id -> {
                    onLogLayoutClickListener?.onClickDetails(pos, v)
                }
            }
        }
    }
}
