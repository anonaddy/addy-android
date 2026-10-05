package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.ManageBlocklistRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.utils.DateTimeUtils

import java.util.Locale

class BlocklistDiffCallback : DiffUtil.ItemCallback<BlocklistEntries>() {
    override fun areItemsTheSame(oldItem: BlocklistEntries, newItem: BlocklistEntries): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BlocklistEntries, newItem: BlocklistEntries): Boolean {
        return oldItem == newItem
    }
}

class BlocklistAdapter(
    listWithBlocklistEntries: List<BlocklistEntries> = emptyList(),
    private var onManageBlocklistClicker: ClickListener? = null
) : ListAdapter<BlocklistEntries, BlocklistAdapter.ViewHolder>(BlocklistDiffCallback()) {

    init {
        if (listWithBlocklistEntries.isNotEmpty()) {
            submitList(listWithBlocklistEntries)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ManageBlocklistRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.binding.manageBlocklistRecyclerviewListValue.text = entry.value
        holder.binding.manageBlocklistRecyclerviewListType.text = entry.type
        holder.binding.manageBlocklistRecyclerviewListBlockedCount.text = String.format(Locale.getDefault(), "%d", entry.blocked ?: 0)

        if (!entry.last_blocked.isNullOrEmpty()) {
            holder.binding.manageBlocklistRecyclerviewListLastBlocked.visibility = View.VISIBLE
            holder.binding.manageBlocklistRecyclerviewListLastBlocked.text = String.format(Locale.getDefault(), "(%s)", DateTimeUtils.convertStringToLocalTimeZoneString(entry.last_blocked))
        } else {
            holder.binding.manageBlocklistRecyclerviewListLastBlocked.visibility = View.GONE
        }
    }

    fun setClickListener(listener: ClickListener) {
        onManageBlocklistClicker = listener
    }



    interface ClickListener {
        fun onClickDelete(pos: Int, view: View, id: String)
    }

    inner class ViewHolder(val binding: ManageBlocklistRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.manageBlocklistRecyclerviewListDeleteButton.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                binding.manageBlocklistRecyclerviewListDeleteButton.id -> {
                    onManageBlocklistClicker?.onClickDelete(pos, v, getItem(pos).id)
                }
            }
        }
    }
}
