package host.stjin.anonaddy.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.EmailClientListItemBinding

class EmailClientDiffCallback : DiffUtil.ItemCallback<EmailClientAdapter.EmailClientItem>() {
    override fun areItemsTheSame(oldItem: EmailClientAdapter.EmailClientItem, newItem: EmailClientAdapter.EmailClientItem): Boolean {
        return oldItem.packageName == newItem.packageName && oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: EmailClientAdapter.EmailClientItem, newItem: EmailClientAdapter.EmailClientItem): Boolean {
        return oldItem == newItem
    }
}

class EmailClientAdapter(
    items: List<EmailClientItem> = emptyList(),
    private val showSelection: Boolean = true,
    private val onItemClick: (EmailClientItem) -> Unit
) : ListAdapter<EmailClientAdapter.EmailClientItem, EmailClientAdapter.ViewHolder>(EmailClientDiffCallback()) {

    data class EmailClientItem(
        val packageName: String?,
        val name: String,
        val icon: Drawable,
        val isSelected: Boolean = false
    )

    init {
        if (items.isNotEmpty()) {
            submitList(items)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = EmailClientListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.emailClientItemName.text = item.name
        holder.binding.emailClientItemIcon.setImageDrawable(item.icon)
        holder.binding.emailClientItemCheck.isVisible = showSelection && item.isSelected
        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    class ViewHolder(val binding: EmailClientListItemBinding) : RecyclerView.ViewHolder(binding.root)
}
