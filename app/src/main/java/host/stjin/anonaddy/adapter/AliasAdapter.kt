package host.stjin.anonaddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.futured.donut.DonutProgressView
import app.futured.donut.DonutSection
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.DateTimeUtils


class AliasAdapter(private val listWithAliases: List<Aliases>, context: Context) :
    RecyclerView.Adapter<AliasAdapter.ViewHolder>() {

    lateinit var onAliasClickListener: ClickListener
    private val aliasesToWatch = AliasWatcher(context).getAliasesToWatch()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.aliases_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.mTitle.text = listWithAliases[position].email

        if (listWithAliases[position].description != null) {
            holder.mDescription.text = holder.mDescription.context.resources.getString(
                R.string.s_s_s,
                listWithAliases[position].description,
                (holder.mDescription.context).resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.turnStringIntoLocalString(listWithAliases[position].created_at)
                ),
                (holder.mDescription.context).resources.getString(
                    R.string.updated_at_s,
                    DateTimeUtils.turnStringIntoLocalString(listWithAliases[position].updated_at)
                )
            )
        } else {
            holder.mDescription.text = holder.mDescription.context.resources.getString(
                R.string.s_s,
                (holder.mDescription.context).resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.turnStringIntoLocalString(listWithAliases[position].created_at)
                ),
                (holder.mDescription.context).resources.getString(
                    R.string.updated_at_s,
                    DateTimeUtils.turnStringIntoLocalString(listWithAliases[position].updated_at)
                )
            )
        }


        /*
        CHART
         */

        val forwarded = listWithAliases[position].emails_forwarded.toFloat()
        val replied = listWithAliases[position].emails_replied.toFloat()
        val sent = listWithAliases[position].emails_sent.toFloat()
        val blocked = listWithAliases[position].emails_blocked.toFloat()

        val color1 = if (listWithAliases[position].active) R.color.portalOrange else R.color.md_grey_500
        val color2 = if (listWithAliases[position].active) R.color.portalBlue else R.color.md_grey_600
        val color3 = if (listWithAliases[position].active) R.color.easternBlue else R.color.md_grey_700
        val color4 = if (listWithAliases[position].active) R.color.softRed else R.color.md_grey_800


        val listOfDonutSection: ArrayList<DonutSection> = arrayListOf()
        // DONUT
        val section1 = DonutSection(
            name = holder.mChart.context.resources.getString(R.string.d_forwarded, forwarded.toInt()),
            color = ContextCompat.getColor(holder.mChart.context, color1),
            amount = forwarded
        )
        // Always show section 1
        listOfDonutSection.add(section1)

        if (replied > 0) {
            val section2 = DonutSection(
                name = holder.mChart.context.resources.getString(R.string.d_replied, replied.toInt()),
                color = ContextCompat.getColor(holder.mChart.context, color2),
                amount = replied
            )
            listOfDonutSection.add(section2)
        }

        if (sent > 0) {
            val section3 = DonutSection(
                name = holder.mChart.context.resources.getString(R.string.d_sent, sent.toInt()),
                color = ContextCompat.getColor(holder.mChart.context, color3),
                amount = sent
            )
            listOfDonutSection.add(section3)
        }

        if (blocked > 0) {
            val section4 = DonutSection(
                name = holder.mChart.context.resources.getString(R.string.d_blocked, blocked.toInt()),
                color = ContextCompat.getColor(holder.mChart.context, color4),
                amount = blocked
            )
            listOfDonutSection.add(section4)
        }

        holder.mChart.cap = listOfDonutSection.sumOf { it.amount.toInt() }.toFloat()
        // Sort the list by amount so that the biggest number will fill the whole ring
        holder.mChart.submitData(listOfDonutSection.sortedBy { it.amount })
        // DONUT


        holder.mWatchedTextView.visibility =
            if (aliasesToWatch?.contains(listWithAliases[position].id) == true) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = listWithAliases.size

    fun getList(): List<Aliases> {
        return listWithAliases
    }

    fun setClickOnAliasClickListener(aClickListener: ClickListener) {
        onAliasClickListener = aClickListener
    }

    interface ClickListener {
        fun onClick(pos: Int)
        fun onClickCopy(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mCV: MaterialCardView = view.findViewById(R.id.recyclerview_list_CV)
        var mTitle: TextView = view.findViewById(R.id.aliases_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.aliases_recyclerview_list_description)
        var mWatchedTextView: TextView = view.findViewById(R.id.aliases_recyclerview_list_watched_textview)
        private var mCopy: ImageView = view.findViewById(R.id.aliases_recyclerview_list_copy)
        var mChart: DonutProgressView = view.findViewById(R.id.aliases_recyclerview_list_chart)


        init {
            mCopy.setOnClickListener(this)
            mCV.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            if (p0.id == R.id.recyclerview_list_CV) {
                onAliasClickListener.onClick(adapterPosition)
            } else if (p0.id == R.id.aliases_recyclerview_list_copy) {
                onAliasClickListener.onClickCopy(adapterPosition, p0)
            }
        }

    }
}

