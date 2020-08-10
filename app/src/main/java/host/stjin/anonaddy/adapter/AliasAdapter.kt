package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.DateTimeUtils
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.Aliases

class AliasAdapter(private val listWithAliases: List<Aliases>, private val showStatus: Boolean) :
    RecyclerView.Adapter<AliasAdapter.AliasesViewHolder>() {

    lateinit var onAliasClickListener: ClickListener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AliasesViewHolder {
        return AliasesViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.aliases_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AliasesViewHolder, position: Int) {
        holder.mTitle.text = listWithAliases[position].email

        if (listWithAliases[position].description != null) {
            holder.mDescription.text =
                "${listWithAliases[position].description}\n${(holder.mStatus.context).resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.turnStringIntoLocalPrettyString(listWithAliases[position].created_at)
                )}\n${(holder.mStatus.context).resources.getString(
                    R.string.updated_at_s,
                    DateTimeUtils.turnStringIntoLocalPrettyString(listWithAliases[position].updated_at)
                )}"
        } else {
            holder.mDescription.text =
                "${(holder.mStatus.context).resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.turnStringIntoLocalPrettyString(listWithAliases[position].created_at)
                )}\n${(holder.mStatus.context).resources.getString(
                    R.string.updated_at_s,
                    DateTimeUtils.turnStringIntoLocalPrettyString(listWithAliases[position].updated_at)
                )}"
        }


        holder.mCount.text = listWithAliases[position].emails_forwarded.toString()

        if (showStatus) {
            holder.mStatus.visibility = View.VISIBLE
            if (listWithAliases[position].deleted_at == null) {
                if (listWithAliases[position].active) {
                    holder.mStatus.text =
                        (holder.mStatus.context).resources.getString(R.string.active)
                    holder.mCopy.alpha = 1f
                } else {
                    holder.mStatus.text =
                        (holder.mStatus.context).resources.getString(R.string.inactive)
                    holder.mCopy.alpha = 0.3f
                }
            } else {
                holder.mStatus.text = (holder.mStatus.context).resources.getString(R.string.deleted)
                holder.mCopy.alpha = 0.3f
            }
        } else {
            holder.mStatus.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listWithAliases.size


    fun setClickOnAliasClickListener(aClickListener: ClickListener) {
        onAliasClickListener = aClickListener
    }

    interface ClickListener {
        fun onClick(pos: Int, aView: View)
        fun onClickCopy(pos: Int, aView: View)
    }

    inner class AliasesViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mLL: LinearLayout = view.findViewById(R.id.aliases_recyclerview_list_LL)
        var mTitle: TextView = view.findViewById(R.id.aliases_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.aliases_recyclerview_list_description)
        var mCount: TextView = view.findViewById(R.id.aliases_recyclerview_list_count)
        var mStatus: TextView = view.findViewById(R.id.aliases_recyclerview_list_status)
        var mCopy: ImageView = view.findViewById(R.id.aliases_recyclerview_list_copy)


        init {
            mCopy.setOnClickListener(this)
            mLL.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            if (p0.id == R.id.aliases_recyclerview_list_copy) {
                onAliasClickListener.onClick(adapterPosition, p0)
            } else if (p0.id == R.id.aliases_recyclerview_list_LL) {
                onAliasClickListener.onClickCopy(adapterPosition, p0)
            }
        }

    }
}

