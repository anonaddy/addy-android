package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.Rules
import host.stjin.anonaddy.utils.ItemTouchHelperAdapter

class RulesAdapter(
    private val listWithRules: ArrayList<Rules>
) :
    RecyclerView.Adapter<RulesAdapter.ViewHolder>(), ItemTouchHelperAdapter {

    lateinit var onRuleClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.rules_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle.text = listWithRules[position].name

        val descConditions =
            "${listWithRules[position].conditions[0].type} ${listWithRules[position].conditions[0].match} ${listWithRules[position].conditions[0].values[0]}"
        val descActions = "${listWithRules[position].conditions[0].type} ${listWithRules[position].conditions[0].values}"

        holder.mDescription.text = holder.mDescription.context.resources.getString(R.string.manage_rules_list_desc, descConditions, descActions)

        if (listWithRules[position].active) {
            holder.rulesRecyclerviewListIcon.setImageResource(R.drawable.ic_clipboard_text_outline)
        } else {
            holder.rulesRecyclerviewListIcon.setImageResource(R.drawable.ic_clipboard_text_off_outline)
        }
    }

    override fun getItemCount(): Int = listWithRules.size


    fun setClickListener(aClickListener: ClickListener) {
        onRuleClicker = aClickListener
    }


    interface ClickListener {
        fun onClickSettings(pos: Int, aView: View)
        fun onClickDelete(pos: Int, aView: View)
        fun onItemMove(fromPosition: Int, toPosition: Int)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mLL: LinearLayout = view.findViewById(R.id.rules_recyclerview_list_LL)
        private var rulesRecyclerviewListOptionLl: LinearLayout =
            view.findViewById(R.id.rules_recyclerview_list_option_LL)
        private var mOptionsButton: LinearLayout =
            view.findViewById(R.id.rules_recyclerview_list_expand_options)
        var mTitle: TextView = view.findViewById(R.id.rules_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.rules_recyclerview_list_description)
        var rulesRecyclerviewListIcon: ImageView =
            view.findViewById(R.id.rules_recyclerview_list_icon)
        private var rulesRecyclerviewListSettingsButton: MaterialButton =
            view.findViewById(R.id.rules_recyclerview_list_settings_button)
        private var rulesRecyclerviewListDeleteButton: MaterialButton =
            view.findViewById(R.id.rules_recyclerview_list_delete_button)


        init {
            mOptionsButton.setOnClickListener(this)
            mLL.setOnClickListener(this)
            rulesRecyclerviewListSettingsButton.setOnClickListener(this)
            rulesRecyclerviewListDeleteButton.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.rules_recyclerview_list_LL -> {
                    expandOptions()
                }
                R.id.rules_recyclerview_list_expand_options -> {
                    expandOptions()
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
            if (rulesRecyclerviewListOptionLl.visibility == View.VISIBLE) {
                rulesRecyclerviewListOptionLl.visibility = View.GONE
                mOptionsButton.rotation = 0f
            } else {
                mOptionsButton.rotation = 180f
                rulesRecyclerviewListOptionLl.visibility = View.VISIBLE
            }
        }

    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        notifyItemMoved(fromPosition, toPosition)
        onRuleClicker.onItemMove(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        // not being used
    }

}

