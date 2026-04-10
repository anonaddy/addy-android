package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.utils.DateTimeUtils

class BlocklistAdapter(
    private val listWithBlocklistEntries: ArrayList<BlocklistEntries>
) :
    RecyclerView.Adapter<BlocklistAdapter.ViewHolder>() {

    lateinit var onManageBlocklistClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.manage_blocklist_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = listWithBlocklistEntries[position]
        holder.mValue.text = entry.value
        holder.mType.text = entry.type
        holder.mBlockedCount.text = (entry.blocked ?: 0).toString()

        if (!entry.last_blocked.isNullOrEmpty()) {
            holder.mLastBlocked.visibility = View.VISIBLE
            holder.mLastBlocked.text = "(${DateTimeUtils.convertStringToLocalTimeZoneString(entry.last_blocked)})"
        } else {
            holder.mLastBlocked.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listWithBlocklistEntries.size


    fun setClickListener(aClickListener: ClickListener) {
        onManageBlocklistClicker = aClickListener
    }

    fun getList(): ArrayList<BlocklistEntries> {
        return listWithBlocklistEntries
    }


    interface ClickListener {
        fun onClickDelete(pos: Int, aView: View, id: String)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mOptionsButton: MaterialButton =
            view.findViewById(R.id.manage_blocklist_recyclerview_list_delete_button)
        var mValue: TextView = view.findViewById(R.id.manage_blocklist_recyclerview_list_value)
        var mType: TextView =
            view.findViewById(R.id.manage_blocklist_recyclerview_list_type)
        var mBlockedCount: TextView =
            view.findViewById(R.id.manage_blocklist_recyclerview_list_blocked_count)
        var mLastBlocked: TextView =
            view.findViewById(R.id.manage_blocklist_recyclerview_list_last_blocked)

        init {
            mOptionsButton.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.manage_blocklist_recyclerview_list_delete_button -> {
                    onManageBlocklistClicker.onClickDelete(adapterPosition, p0, listWithBlocklistEntries[adapterPosition].id)
                }

            }
        }
    }


}

