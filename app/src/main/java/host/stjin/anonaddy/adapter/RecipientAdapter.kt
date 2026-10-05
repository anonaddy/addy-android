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
import host.stjin.anonaddy.databinding.RecipientsRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.Recipients

class RecipientDiffCallback : DiffUtil.ItemCallback<Recipients>() {
    override fun areItemsTheSame(oldItem: Recipients, newItem: Recipients): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Recipients, newItem: Recipients): Boolean {
        return oldItem == newItem
    }
}

class RecipientAdapter(
    listWithRecipients: List<Recipients> = emptyList(),
    private var onRecipientClicker: ClickListener? = null
) : ListAdapter<Recipients, RecipientAdapter.ViewHolder>(RecipientDiffCallback()) {

    init {
        if (listWithRecipients.isNotEmpty()) {
            submitList(listWithRecipients)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecipientsRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.recipientsRecyclerviewListTitle.text = item.email

        val aliases = item.aliases_count

        if (item.description != null) {
            holder.binding.recipientsRecyclerviewListDescription.text = item.description
        } else {
            holder.binding.recipientsRecyclerviewListDescription.text = holder.itemView.context.resources.getString(
                R.string.recipients_list_description,
                aliases
            )
        }

        when {
            item.email_verified_at == null -> {
                holder.binding.recipientsRecyclerviewListIcon.setImageResource(R.drawable.ic_alert_circle)
                holder.binding.recipientsRecyclerviewListDescription.text = holder.itemView.context.resources.getString(R.string.not_verified)

                holder.binding.recipientsRecyclerviewListDeleteButton.visibility = View.VISIBLE
                holder.binding.recipientsRecyclerviewListResendButton.visibility = View.VISIBLE
                holder.binding.recipientsRecyclerviewListSettingsButton.visibility = View.GONE
            }

            item.should_encrypt -> {
                holder.binding.recipientsRecyclerviewListIcon.setImageResource(R.drawable.ic_mail_encrypted)

                holder.binding.recipientsRecyclerviewListDeleteButton.visibility = View.VISIBLE
                holder.binding.recipientsRecyclerviewListResendButton.visibility = View.GONE
                holder.binding.recipientsRecyclerviewListSettingsButton.visibility = View.VISIBLE
            }

            else -> {
                holder.binding.recipientsRecyclerviewListIcon.setImageResource(R.drawable.ic_mail)

                holder.binding.recipientsRecyclerviewListDeleteButton.visibility = View.VISIBLE
                holder.binding.recipientsRecyclerviewListResendButton.visibility = View.GONE
                holder.binding.recipientsRecyclerviewListSettingsButton.visibility = View.VISIBLE
            }
        }

        if (!holder.itemView.context.resources.getBoolean(R.bool.isTablet)) {
            holder.binding.recipientsRecyclerviewListOptionLL.visibility = View.GONE
            holder.binding.recipientsRecyclerviewListExpandOptions.rotation = 0f
        }
    }

    fun setClickListener(listener: ClickListener) {
        onRecipientClicker = listener
    }



    interface ClickListener {
        fun onClickSettings(pos: Int, view: View)
        fun onClickResend(pos: Int, view: View)
        fun onClickDelete(pos: Int, view: View)
    }

    inner class ViewHolder(val binding: RecipientsRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.recipientsRecyclerviewListExpandOptions.setOnClickListener(this)
            binding.recipientsRecyclerviewListCV.setOnClickListener(this)
            binding.recipientsRecyclerviewListSettingsButton.setOnClickListener(this)
            binding.recipientsRecyclerviewListResendButton.setOnClickListener(this)
            binding.recipientsRecyclerviewListDeleteButton.setOnClickListener(this)

            checkForTabletLayout(binding.recipientsRecyclerviewListDeleteButton.context)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                R.id.recipients_recyclerview_list_CV -> {
                    expandOptions()
                }

                R.id.recipients_recyclerview_list_expand_options -> {
                    expandOptions()
                }

                R.id.recipients_recyclerview_list_settings_button -> {
                    onRecipientClicker?.onClickSettings(pos, v)
                }

                R.id.recipients_recyclerview_list_resend_button -> {
                    onRecipientClicker?.onClickResend(pos, v)
                }

                R.id.recipients_recyclerview_list_delete_button -> {
                    onRecipientClicker?.onClickDelete(pos, v)
                }
            }
        }

        private fun expandOptions() {
            if (!binding.recipientsRecyclerviewListOptionLL.context.resources.getBoolean(R.bool.isTablet)) {
                if (binding.recipientsRecyclerviewListOptionLL.isVisible) {
                    binding.recipientsRecyclerviewListOptionLL.visibility = View.GONE
                    binding.recipientsRecyclerviewListExpandOptions.rotation = 0f
                } else {
                    binding.recipientsRecyclerviewListExpandOptions.rotation = 180f
                    binding.recipientsRecyclerviewListOptionLL.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                binding.recipientsRecyclerviewListExpandOptions.visibility = View.GONE
                binding.recipientsRecyclerviewListOptionLL.visibility = View.VISIBLE
            }
        }
    }
}
