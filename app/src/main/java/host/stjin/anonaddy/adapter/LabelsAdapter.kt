package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.LabelsRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.Labels

class LabelDiffCallback : DiffUtil.ItemCallback<Labels>() {
    override fun areItemsTheSame(oldItem: Labels, newItem: Labels): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Labels, newItem: Labels): Boolean {
        return oldItem == newItem
    }
}

class LabelsAdapter(
    listWithLabels: List<Labels> = emptyList(),
    private var onManageLabelsClicker: ClickListener? = null
) : ListAdapter<Labels, LabelsAdapter.ViewHolder>(LabelDiffCallback()) {

    init {
        if (listWithLabels.isNotEmpty()) {
            submitList(listWithLabels)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LabelsRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.binding.manageLabelsRecyclerviewListName.text = entry.name
        val aliasesCount = entry.aliases_count ?: 0
        holder.binding.manageLabelsRecyclerviewListColorText.text = holder.itemView.context.resources.getQuantityString(
            R.plurals.d_aliases,
            aliasesCount,
            aliasesCount
        )

        try {
            holder.binding.manageLabelsRecyclerviewListColorIndicator.setColorFilter(entry.colour.toColorInt())
        } catch (_: Exception) {
            // fallback
        }
    }

    fun setClickListener(listener: ClickListener) {
        onManageLabelsClicker = listener
    }



    interface ClickListener {
        fun onClickDelete(pos: Int, view: View, id: String)
        fun onClickEdit(pos: Int, view: View, label: Labels)
    }

    inner class ViewHolder(val binding: LabelsRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.manageLabelsRecyclerviewListDeleteButton.setOnClickListener(this)
            binding.manageLabelsRecyclerviewListCV.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                binding.manageLabelsRecyclerviewListDeleteButton.id -> {
                    onManageLabelsClicker?.onClickDelete(pos, v, getItem(pos).id)
                }

                binding.manageLabelsRecyclerviewListCV.id -> {
                    onManageLabelsClicker?.onClickEdit(pos, v, getItem(pos))
                }
            }
        }
    }
}
