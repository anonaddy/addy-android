package host.stjin.anonaddy.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.RulesRecyclerviewListItemBinding
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import java.util.Collections

class RulesAdapter(
    listWithRules: List<Rules> = emptyList(),
    private var recipients: ArrayList<Recipients>?,
    private val allowDrag: Boolean,
    private var onRuleClicker: ClickListener? = null
) : RecyclerView.Adapter<RulesAdapter.ViewHolder>() {

    private val rulesList = ArrayList<Rules>(listWithRules)

    val currentList: List<Rules>
        get() = rulesList

    fun submitList(list: List<Rules>?) {
        rulesList.clear()
        if (list != null) {
            rulesList.addAll(list)
        }
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition in rulesList.indices && toPosition in rulesList.indices && fromPosition != toPosition) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(rulesList, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(rulesList, i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    fun updateRecipients(newRecipients: ArrayList<Recipients>?) {
        if (this.recipients != newRecipients) {
            this.recipients = newRecipients
            if (itemCount > 0) {
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    override fun getItemCount(): Int = rulesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RulesRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = rulesList[position]

        if (allowDrag) {
            holder.binding.rulesRecyclerviewListDrag.visibility = View.VISIBLE
        } else {
            holder.binding.rulesRecyclerviewListDrag.visibility = View.GONE
        }

        holder.binding.rulesRecyclerviewListTitle.text = item.name

        holder.binding.rulesRecyclerviewListActivateButton.text =
            if (item.active) holder.itemView.context.resources.getString(R.string.deactivate)
            else holder.itemView.context.resources.getString(R.string.activate)

        val context = holder.itemView.context
        val condition = item.conditions.firstOrNull()
        val descConditions = if (condition != null) {
            val typeTypes = context.resources.getStringArray(R.array.conditions_type)
            val typeNames = context.resources.getStringArray(R.array.conditions_type_name)
            val typeIndex = typeTypes.indexOf(condition.type)
            val typeText = if (typeIndex != -1) typeNames[typeIndex] else condition.type

            val isBooleanCondition = when (condition.type) {
                "alias_created_by_catch_all", "alias_not_created_by_catch_all",
                "has_attachments", "has_no_attachments",
                "email_is_spam", "email_is_not_spam",
                "dmarc_failed", "dmarc_did_not_fail" -> true
                else -> false
            }

            if (isBooleanCondition) {
                typeText
            } else {
                val matchTypes = context.resources.getStringArray(R.array.conditions_match)
                val matchNames = context.resources.getStringArray(R.array.conditions_match_name)
                val matchIndex = if (condition.match != null) matchTypes.indexOf(condition.match) else -1
                val matchText = if (matchIndex != -1) matchNames[matchIndex] else condition.match

                val firstValue = condition.values?.firstOrNull() ?: ""
                if (!matchText.isNullOrEmpty() && firstValue.isNotEmpty()) {
                    "$typeText $matchText $firstValue"
                } else if (!matchText.isNullOrEmpty()) {
                    "$typeText $matchText"
                } else {
                    typeText
                }
            }
        } else {
            ""
        }

        val action = item.actions.firstOrNull()
        val descActions = if (action != null) {
            val actionTypes = context.resources.getStringArray(R.array.actions_type)
            val actionNames = context.resources.getStringArray(R.array.actions_type_name)
            val actionTypeIndex = actionTypes.indexOf(action.type)
            val actionTypeText = if (actionTypeIndex != -1) actionNames[actionTypeIndex] else action.type

            when (action.type) {
                "forwardTo" -> {
                    val recipient = recipients?.firstOrNull { it.id == action.value }?.email
                        ?: context.resources.getString(R.string.unknown)
                    "$actionTypeText $recipient"
                }
                "block", "encryption", "blocklistSender", "blocklistDomain", "removeAttachments", "deactivateAlias", "deleteAlias" -> {
                    actionTypeText
                }
                else -> {
                    if (!action.value.isNullOrEmpty()) "$actionTypeText ${action.value}" else actionTypeText
                }
            }
        } else {
            ""
        }

        holder.binding.rulesRecyclerviewListDescription.text = context.resources.getString(R.string.manage_rules_list_desc, descConditions, descActions)

        if (item.active) {
            holder.binding.rulesRecyclerviewListIcon.setImageResource(R.drawable.ic_clipboard_list)
        } else {
            holder.binding.rulesRecyclerviewListIcon.setImageResource(R.drawable.ic_clipboard_list_off)
        }
    }

    fun setClickListener(listener: ClickListener) {
        onRuleClicker = listener
    }



    interface ClickListener {
        fun onClickActivate(pos: Int, view: View)
        fun onClickSettings(pos: Int, view: View)
        fun onClickDelete(pos: Int, view: View)
        fun startDragging(viewHolder: RecyclerView.ViewHolder?)
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(val binding: RulesRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.rulesRecyclerviewListExpandOptions.setOnClickListener(this)
            binding.rulesRecyclerviewListCV.setOnClickListener(this)
            binding.rulesRecyclerviewListSettingsButton.setOnClickListener(this)
            binding.rulesRecyclerviewListActivateButton.setOnClickListener(this)
            binding.rulesRecyclerviewListDeleteButton.setOnClickListener(this)

            if (allowDrag) {
                binding.rulesRecyclerviewListDrag.setOnTouchListener { _, motionEvent ->
                    if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                        onRuleClicker?.startDragging(this)
                    }
                    return@setOnTouchListener true
                }
            }

            checkForTabletLayout(binding.rulesRecyclerviewListDeleteButton.context)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            when (v.id) {
                R.id.rules_recyclerview_list_CV -> {
                    expandOptions()
                }

                R.id.rules_recyclerview_list_expand_options -> {
                    expandOptions()
                }

                R.id.rules_recyclerview_list_activate_button -> {
                    onRuleClicker?.onClickActivate(pos, v)
                }

                R.id.rules_recyclerview_list_settings_button -> {
                    onRuleClicker?.onClickSettings(pos, v)
                }

                R.id.rules_recyclerview_list_delete_button -> {
                    onRuleClicker?.onClickDelete(pos, v)
                }
            }
        }

        private fun expandOptions() {
            if (!binding.rulesRecyclerviewListOptionLL.context.resources.getBoolean(R.bool.isTablet)) {
                if (binding.rulesRecyclerviewListOptionLL.isVisible) {
                    binding.rulesRecyclerviewListOptionLL.visibility = View.GONE
                    binding.rulesRecyclerviewListExpandOptions.rotation = 0f
                } else {
                    binding.rulesRecyclerviewListExpandOptions.rotation = 180f
                    binding.rulesRecyclerviewListOptionLL.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                binding.rulesRecyclerviewListExpandOptions.visibility = View.GONE
                binding.rulesRecyclerviewListOptionLL.visibility = View.VISIBLE
            }
        }
    }
}
