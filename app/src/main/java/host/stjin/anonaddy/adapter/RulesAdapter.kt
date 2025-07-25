package host.stjin.anonaddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import androidx.core.view.isVisible

class RulesAdapter(
    private val listWithRules: ArrayList<Rules>,
    private val recipients: ArrayList<Recipients>?,
    private val allowDrag: Boolean
) :
    RecyclerView.Adapter<RulesAdapter.ViewHolder>() {

    lateinit var onRuleClicker: ClickListener


    fun moveItem(fromPosition: Int, toPosition: Int) {
        onRuleClicker.onItemMove(fromPosition, toPosition)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.rules_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (allowDrag) {
            holder.rulesRecyclerviewListDragLL.visibility = View.VISIBLE
        } else {
            holder.rulesRecyclerviewListDragLL.visibility = View.GONE
        }

        holder.mTitle.text = listWithRules[position].name

        holder.rulesRecyclerviewListActivateButton.text =
            if (listWithRules[position].active) holder.rulesRecyclerviewListActivateButton.context.resources.getString(R.string.deactivate) else holder.rulesRecyclerviewListActivateButton.context.resources.getString(
                R.string.activate
            )


        val typeText =
            holder.rulesRecyclerviewListActivateButton.context.resources.getStringArray(R.array.conditions_type_name)[holder.rulesRecyclerviewListActivateButton.context.resources.getStringArray(R.array.conditions_type)
                .indexOf(listWithRules[position].conditions[0].type)]
        val matchText =
            holder.rulesRecyclerviewListActivateButton.context.resources.getStringArray(R.array.conditions_match_name)[holder.rulesRecyclerviewListActivateButton.context.resources.getStringArray(R.array.conditions_match)
                .indexOf(listWithRules[position].conditions[0].match)]
        val descConditions =
            "$typeText $matchText ${listWithRules[position].conditions[0].values[0]}"

        val actionTypeText =
            holder.rulesRecyclerviewListActivateButton.context.resources.getStringArray(R.array.actions_type_name)[holder.rulesRecyclerviewListActivateButton.context.resources.getStringArray(R.array.actions_type).indexOf(listWithRules[position].actions[0].type)]

        // If forward_to type resolve the recipient
        if (listWithRules[position].actions[0].type == "forwardTo" && recipients != null){

            val recipient = recipients.firstOrNull { it.id == listWithRules[position].actions[0].value }?.email ?:
            holder.mDescription.context.resources.getString(R.string.unknown)

            val descActions = "$actionTypeText $recipient"

            holder.mDescription.text = holder.mDescription.context.resources.getString(R.string.manage_rules_list_desc, descConditions, descActions)

        } else {
            val descActions = "$actionTypeText ${listWithRules[position].actions[0].value}"

            holder.mDescription.text = holder.mDescription.context.resources.getString(R.string.manage_rules_list_desc, descConditions, descActions)

        }



        if (listWithRules[position].active) {
            holder.rulesRecyclerviewListIcon.setImageResource(R.drawable.ic_clipboard_list)
        } else {
            holder.rulesRecyclerviewListIcon.setImageResource(R.drawable.ic_clipboard_list_off)
        }
    }

    override fun getItemCount(): Int = listWithRules.size


    fun setClickListener(aClickListener: ClickListener) {
        onRuleClicker = aClickListener
    }

    fun getList(): ArrayList<Rules> {
        return listWithRules
    }


    interface ClickListener {
        fun onClickActivate(pos: Int, aView: View)
        fun onClickSettings(pos: Int, aView: View)
        fun onClickDelete(pos: Int, aView: View)
        fun onItemMove(fromPosition: Int, toPosition: Int)
        fun startDragging(viewHolder: RecyclerView.ViewHolder?)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mCV: MaterialCardView = view.findViewById(R.id.rules_recyclerview_list_CV)
        private var rulesRecyclerviewListOptionLl: LinearLayout =
            view.findViewById(R.id.rules_recyclerview_list_option_LL)
        var rulesRecyclerviewListDragLL: LinearLayout =
            view.findViewById(R.id.rules_recyclerview_list_drag)
        private var mOptionsButton: LinearLayout =
            view.findViewById(R.id.rules_recyclerview_list_expand_options)
        var mTitle: TextView = view.findViewById(R.id.rules_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.rules_recyclerview_list_description)
        var rulesRecyclerviewListIcon: ImageView =
            view.findViewById(R.id.rules_recyclerview_list_icon)
        private var rulesRecyclerviewListSettingsButton: MaterialButton =
            view.findViewById(R.id.rules_recyclerview_list_settings_button)
        var rulesRecyclerviewListActivateButton: MaterialButton =
            view.findViewById(R.id.rules_recyclerview_list_activate_button)
        private var rulesRecyclerviewListDeleteButton: MaterialButton =
            view.findViewById(R.id.rules_recyclerview_list_delete_button)


        init {
            mOptionsButton.setOnClickListener(this)
            mCV.setOnClickListener(this)
            rulesRecyclerviewListSettingsButton.setOnClickListener(this)
            rulesRecyclerviewListActivateButton.setOnClickListener(this)
            rulesRecyclerviewListDeleteButton.setOnClickListener(this)

            if (allowDrag) {
                rulesRecyclerviewListDragLL.setOnTouchListener { _, motionEvent ->
                    if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                        onRuleClicker.startDragging(this)
                    }
                    return@setOnTouchListener true
                }
            }

            checkForTabletLayout(rulesRecyclerviewListDeleteButton.context)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.rules_recyclerview_list_CV -> {
                    expandOptions()
                }
                R.id.rules_recyclerview_list_expand_options -> {
                    expandOptions()
                }
                R.id.rules_recyclerview_list_activate_button -> {
                    onRuleClicker.onClickActivate(adapterPosition, p0)
                }
                R.id.rules_recyclerview_list_settings_button -> {
                    onRuleClicker.onClickSettings(adapterPosition, p0)
                }
                R.id.rules_recyclerview_list_delete_button -> {
                    onRuleClicker.onClickDelete(adapterPosition, p0)
                }
            }
        }

        private fun expandOptions() {
            if (!rulesRecyclerviewListOptionLl.context.resources.getBoolean(R.bool.isTablet)) {
                if (rulesRecyclerviewListOptionLl.isVisible) {
                    rulesRecyclerviewListOptionLl.visibility = View.GONE
                    mOptionsButton.rotation = 0f
                } else {
                    mOptionsButton.rotation = 180f
                    rulesRecyclerviewListOptionLl.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                mOptionsButton.visibility = View.GONE
                rulesRecyclerviewListOptionLl.visibility = View.VISIBLE
            }
        }

    }
}

