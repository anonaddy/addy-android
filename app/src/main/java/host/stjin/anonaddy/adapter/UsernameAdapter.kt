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
import host.stjin.anonaddy.databinding.UsernamesRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.utils.DateTimeUtils

class UsernameDiffCallback : DiffUtil.ItemCallback<Usernames>() {
    override fun areItemsTheSame(oldItem: Usernames, newItem: Usernames): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Usernames, newItem: Usernames): Boolean {
        return oldItem == newItem
    }
}

class UsernameAdapter(
    listWithUsernames: List<Usernames> = emptyList(),
    private var onUsernameClicker: ClickListener? = null
) : ListAdapter<Usernames, UsernameAdapter.ViewHolder>(UsernameDiffCallback()) {

    init {
        if (listWithUsernames.isNotEmpty()) {
            submitList(listWithUsernames)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = UsernamesRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.usernamesRecyclerviewListTitle.text = item.username

        if (item.description != null) {
            holder.binding.usernamesRecyclerviewListDescription.text = holder.itemView.context.resources.getString(
                R.string.s_s_s,
                item.description,
                holder.itemView.context.resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.convertStringToLocalTimeZoneString(item.created_at)
                ),
                holder.itemView.context.resources.getString(
                    R.string.updated_at_s,
                    DateTimeUtils.convertStringToLocalTimeZoneString(item.updated_at)
                )
            )
        } else {
            holder.binding.usernamesRecyclerviewListDescription.text = holder.itemView.context.resources.getString(
                R.string.s_s,
                holder.itemView.context.resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.convertStringToLocalTimeZoneString(item.created_at)
                ),
                holder.itemView.context.resources.getString(
                    R.string.updated_at_s,
                    DateTimeUtils.convertStringToLocalTimeZoneString(item.updated_at)
                )
            )
        }

        if (item.active) {
            holder.binding.usernamesRecyclerviewListUser.setImageResource(R.drawable.ic_user)
        } else {
            holder.binding.usernamesRecyclerviewListUser.setImageResource(R.drawable.ic_user_off)
        }

        if (!holder.itemView.context.resources.getBoolean(R.bool.isTablet)) {
            holder.binding.usernamesRecyclerviewListOptionLL.visibility = View.GONE
            holder.binding.usernamesRecyclerviewListExpandOptions.rotation = 0f
        }
    }

    fun setClickListener(listener: ClickListener) {
        onUsernameClicker = listener
    }



    interface ClickListener {
        fun onClickSettings(pos: Int, view: View)
        fun onClickDelete(pos: Int, view: View)
    }

    inner class ViewHolder(val binding: UsernamesRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.usernamesRecyclerviewListExpandOptions.setOnClickListener(this)
            binding.usernamesRecyclerviewListCV.setOnClickListener(this)
            binding.usernamesRecyclerviewListSettingsButton.setOnClickListener(this)
            binding.usernamesRecyclerviewListDeleteButton.setOnClickListener(this)

            checkForTabletLayout(binding.usernamesRecyclerviewListDeleteButton.context)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                R.id.usernames_recyclerview_list_CV -> {
                    expandOptions()
                }

                R.id.usernames_recyclerview_list_expand_options -> {
                    expandOptions()
                }

                R.id.usernames_recyclerview_list_settings_button -> {
                    onUsernameClicker?.onClickSettings(pos, v)
                }

                R.id.usernames_recyclerview_list_delete_button -> {
                    onUsernameClicker?.onClickDelete(pos, v)
                }
            }
        }

        private fun expandOptions() {
            if (!binding.usernamesRecyclerviewListOptionLL.context.resources.getBoolean(R.bool.isTablet)) {
                if (binding.usernamesRecyclerviewListOptionLL.isVisible) {
                    binding.usernamesRecyclerviewListOptionLL.visibility = View.GONE
                    binding.usernamesRecyclerviewListExpandOptions.rotation = 0f
                } else {
                    binding.usernamesRecyclerviewListExpandOptions.rotation = 180f
                    binding.usernamesRecyclerviewListOptionLL.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                binding.usernamesRecyclerviewListExpandOptions.visibility = View.GONE
                binding.usernamesRecyclerviewListOptionLL.visibility = View.VISIBLE
            }
        }
    }
}
