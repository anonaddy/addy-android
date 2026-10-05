package host.stjin.anonaddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.DomainsRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.Domains

class DomainDiffCallback : DiffUtil.ItemCallback<Domains>() {
    override fun areItemsTheSame(oldItem: Domains, newItem: Domains): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Domains, newItem: Domains): Boolean {
        return oldItem == newItem
    }
}

class DomainAdapter(
    listWithDomains: List<Domains> = emptyList(),
    private var onDomainClicker: ClickListener? = null
) : ListAdapter<Domains, DomainAdapter.ViewHolder>(DomainDiffCallback()) {

    init {
        if (listWithDomains.isNotEmpty()) {
            submitList(listWithDomains)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DomainsRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.domainsRecyclerviewListTitle.text = item.domain

        when (item.domain_sending_verified_at) {
            null -> {
                holder.binding.domainsRecyclerviewListDescription.text = holder.itemView.context.resources.getString(
                    R.string.configuration_error
                )
                holder.binding.domainsRecyclerviewListIcon.setImageResource(R.drawable.ic_alert_circle)
            }

            else -> {
                if (item.description != null) {
                    holder.binding.domainsRecyclerviewListDescription.text = item.description
                } else {
                    holder.binding.domainsRecyclerviewListDescription.text = holder.itemView.context.resources.getString(
                        R.string.domains_list_description,
                        item.aliases_count
                    )
                }
                holder.binding.domainsRecyclerviewListIcon.setImageResource(R.drawable.ic_dns)
            }
        }

        if (!holder.itemView.context.resources.getBoolean(R.bool.isTablet)) {
            holder.binding.domainsRecyclerviewListOptionLL.visibility = View.GONE
            holder.binding.domainsRecyclerviewListExpandOptions.rotation = 0f
        }
    }

    fun setClickListener(listener: ClickListener) {
        onDomainClicker = listener
    }



    interface ClickListener {
        fun onClickSettings(pos: Int, view: View)
        fun onClickDelete(pos: Int, view: View)
    }

    inner class ViewHolder(val binding: DomainsRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.domainsRecyclerviewListExpandOptions.setOnClickListener(this)
            binding.domainsRecyclerviewListCV.setOnClickListener(this)
            binding.domainsRecyclerviewListSettingsButton.setOnClickListener(this)
            binding.domainsRecyclerviewListDeleteButton.setOnClickListener(this)

            checkForTabletLayout(binding.domainsRecyclerviewListDeleteButton.context)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                R.id.domains_recyclerview_list_CV -> {
                    expandOptions()
                }

                R.id.domains_recyclerview_list_expand_options -> {
                    expandOptions()
                }

                R.id.domains_recyclerview_list_settings_button -> {
                    onDomainClicker?.onClickSettings(pos, v)
                }

                R.id.domains_recyclerview_list_delete_button -> {
                    onDomainClicker?.onClickDelete(pos, v)
                }
            }
        }

        private fun expandOptions() {
            if (!binding.domainsRecyclerviewListOptionLL.context.resources.getBoolean(R.bool.isTablet)) {
                if (binding.domainsRecyclerviewListOptionLL.isVisible) {
                    binding.domainsRecyclerviewListOptionLL.visibility = View.GONE
                    binding.domainsRecyclerviewListExpandOptions.rotation = 0f
                } else {
                    binding.domainsRecyclerviewListExpandOptions.rotation = 180f
                    binding.domainsRecyclerviewListOptionLL.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                binding.domainsRecyclerviewListExpandOptions.visibility = View.GONE
                binding.domainsRecyclerviewListOptionLL.visibility = View.VISIBLE
            }
        }
    }
}
