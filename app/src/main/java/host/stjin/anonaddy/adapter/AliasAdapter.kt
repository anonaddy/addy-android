package host.stjin.anonaddy.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.futured.donut.DonutProgressView
import app.futured.donut.DonutSection
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.DateTimeUtils


class AliasAdapter(private val listWithAliases: List<Aliases>, context: Context, private val supportMultipleSelection: Boolean = false) :
    RecyclerView.Adapter<AliasAdapter.ViewHolder>() {

    lateinit var onAliasAliasInterface: AliasInterface
    private val aliasesToWatch = AliasWatcher(context).getAliasesToWatch()
    private var selectedAliases: ArrayList<Aliases> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.aliases_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        applySelectedOverlay(holder, position)

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
            if (aliasesToWatch.contains(listWithAliases[position].id)) View.VISIBLE else View.GONE
    }

    private fun applySelectedOverlay(holder: ViewHolder, position: Int) {
        // Check if the item is selected
        if (selectedAliases.contains(listWithAliases[position])) {
            holder.mCV.cardElevation = 0f
            holder.mLL0.setBackgroundColor(ContextCompat.getColor(holder.mLL0.context, R.color.selected_background_color))
            holder.mWatchAliasLL.setBackgroundColor(ContextCompat.getColor(holder.mWatchAliasLL.context, R.color.selected_background_color_darker))
            holder.mAction.setImageDrawable(ContextCompat.getDrawable(holder.mAction.context, R.drawable.ic_check))
        } else {
            holder.mCV.cardElevation = holder.mCV.context.resources.getDimension(R.dimen.cardview_default_elevation)
            holder.mLL0.setBackgroundColor(0)
            holder.mWatchAliasLL.setBackgroundColor(0)
            holder.mAction.setImageDrawable(ContextCompat.getDrawable(holder.mAction.context, R.drawable.ic_copy))
        }
    }

    override fun getItemCount(): Int = listWithAliases.size

    fun getList(): List<Aliases> {
        return listWithAliases
    }

    fun setClickOnAliasClickListener(aAliasInterface: AliasInterface) {
        onAliasAliasInterface = aAliasInterface
    }

    fun unselectAliases() {
        for (alias in selectedAliases) {
            val findAliasPosition = listWithAliases.indexOfFirst { it == alias }
            if (findAliasPosition > -1) {
                notifyItemChanged(findAliasPosition)
            }

        }
        selectedAliases.clear()
    }

    interface AliasInterface {
        fun onClick(pos: Int)
        fun onClickCopy(pos: Int, aView: View)
        fun onSelectionMode(selectionMode: Boolean, selectedAliases: ArrayList<Aliases>) { /* By default, don't implement */
        }
    }


    var originalCardviewColor: ColorStateList? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener, View.OnLongClickListener {

        var mCV: MaterialCardView = view.findViewById(R.id.recyclerview_list_CV)
        var mTitle: TextView = view.findViewById(R.id.aliases_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.aliases_recyclerview_list_description)
        var mWatchedTextView: TextView = view.findViewById(R.id.aliases_recyclerview_list_watched_textview)
        var mAction: ImageView = view.findViewById(R.id.aliases_recyclerview_list_copy)
        var mChart: DonutProgressView = view.findViewById(R.id.aliases_recyclerview_list_chart)
        var mLL0: LinearLayout = view.findViewById(R.id.aliases_recyclerview_list_LL0)
        var mWatchAliasLL: LinearLayout = view.findViewById(R.id.aliases_recyclerview_list_LL5)

        init {
            mAction.setOnClickListener(this)
            mCV.setOnClickListener(this)

            if (supportMultipleSelection) {
                mCV.setOnLongClickListener(this)
            }

            if (adapterPosition == 0 && originalCardviewColor != null) {
                originalCardviewColor = mCV.cardBackgroundColor
            }
        }

        override fun onClick(p0: View) {
            if (p0.id == R.id.recyclerview_list_CV) {
                if (selectedAliases.any()) {
                    selectItem(adapterPosition)
                } else {
                    onAliasAliasInterface.onClick(adapterPosition)
                }
            } else if (p0.id == R.id.aliases_recyclerview_list_copy) {
                onAliasAliasInterface.onClickCopy(adapterPosition, p0)
            }
        }

        override fun onLongClick(p0: View): Boolean {
            if (p0.id == R.id.recyclerview_list_CV) {
                selectItem(adapterPosition)
            }
            return true
        }

        private fun selectItem(adapterPosition: Int) {
            mCV.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (selectedAliases.contains(listWithAliases[adapterPosition])) {
                selectedAliases.remove(listWithAliases[adapterPosition])
            } else {
                if (selectedAliases.count() < 15) { // TODO make this 25, with that up to count for maxed watches aliases and implement to new API for bulk select
                    selectedAliases.add(listWithAliases[adapterPosition])
                } else {
                    Toast.makeText(mCV.context, mCV.context.resources.getString(R.string.alias_multiple_selection_max_reached), Toast.LENGTH_LONG)
                        .show()
                }
            }

            onAliasAliasInterface.onSelectionMode(
                selectionMode = selectedAliases.any(),
                selectedAliases = selectedAliases
            )

            notifyItemChanged(adapterPosition)
        }

    }
}

