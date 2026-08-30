package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.AccountNotificationsRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.utils.DateTimeUtils

class AccountNotificationDiffCallback : DiffUtil.ItemCallback<AccountNotifications>() {
    override fun areItemsTheSame(oldItem: AccountNotifications, newItem: AccountNotifications): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AccountNotifications, newItem: AccountNotifications): Boolean {
        return oldItem == newItem
    }
}

class AccountNotificationsAdapter(
    listWithAccountNotifications: List<AccountNotifications> = emptyList(),
    private var onAccountNotificationClicker: ClickListener? = null
) : ListAdapter<AccountNotifications, AccountNotificationsAdapter.ViewHolder>(AccountNotificationDiffCallback()) {

    init {
        if (listWithAccountNotifications.isNotEmpty()) {
            submitList(listWithAccountNotifications)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AccountNotificationsRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.accountNotificationsRecyclerviewListTitle.text = item.title
        holder.binding.accountNotificationsRecyclerviewListCreated.text = DateTimeUtils.convertStringToLocalTimeZoneString(item.created_at)
        holder.binding.accountNotificationsRecyclerviewListText.text = HtmlCompat.fromHtml(item.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    fun setClickListener(listener: ClickListener) {
        onAccountNotificationClicker = listener
    }



    interface ClickListener {
        fun onClickDetails(pos: Int, view: View)
    }

    inner class ViewHolder(val binding: AccountNotificationsRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.accountNotificationsRecyclerviewListDetailsButton.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                binding.accountNotificationsRecyclerviewListDetailsButton.id -> {
                    onAccountNotificationClicker?.onClickDetails(pos, v)
                }
            }
        }
    }
}
