package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.FailedDeliveriesRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.utils.DateTimeUtils

class FailedDeliveryDiffCallback : DiffUtil.ItemCallback<FailedDeliveries>() {
    override fun areItemsTheSame(oldItem: FailedDeliveries, newItem: FailedDeliveries): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FailedDeliveries, newItem: FailedDeliveries): Boolean {
        return oldItem == newItem
    }
}

class FailedDeliveryAdapter(
    listWithFailedDeliveries: List<FailedDeliveries> = emptyList(),
    private var onFailedDeliveryClicker: ClickListener? = null
) : ListAdapter<FailedDeliveries, FailedDeliveryAdapter.ViewHolder>(FailedDeliveryDiffCallback()) {

    init {
        if (listWithFailedDeliveries.isNotEmpty()) {
            submitList(listWithFailedDeliveries)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FailedDeliveriesRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.failedDeliveriesRecyclerviewListAlias.text = item.alias_email
        holder.binding.failedDeliveriesRecyclerviewListCreated.text = DateTimeUtils.convertStringToLocalTimeZoneString(item.created_at)
        holder.binding.failedDeliveriesRecyclerviewListCode.text = item.code
        holder.binding.failedDeliveriesRecyclerviewListType.text = item.email_type_text
    }

    fun setClickListener(listener: ClickListener) {
        onFailedDeliveryClicker = listener
    }



    interface ClickListener {
        fun onClickDetails(pos: Int, view: View)
    }

    inner class ViewHolder(val binding: FailedDeliveriesRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.failedDeliveriesRecyclerviewListDetailsButton.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                binding.failedDeliveriesRecyclerviewListDetailsButton.id -> {
                    onFailedDeliveryClicker?.onClickDetails(pos, v)
                }
            }
        }
    }
}
