package host.stjin.anonaddy.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gigamole.quatrograde.GradeModel
import com.gigamole.quatrograde.GradeOrientation
import com.gigamole.quatrograde.QuatroGradeView
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.DateTimeUtils


class AliasAdapter(private val listWithAliases: List<Aliases>, private val context: Context) :
    RecyclerView.Adapter<AliasAdapter.ViewHolder>() {

    lateinit var onAliasClickListener: ClickListener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.aliases_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {


        holder.mTitle.text = listWithAliases[position].email


        if (listWithAliases[position].description != null) {
            holder.mDescription.text = listWithAliases[position].description
        } else {
            holder.mDescription.text = (holder.mDescription.context).resources.getString(
                R.string.aliases_recyclerview_list_item_date_time,
                DateTimeUtils.turnStringIntoLocalString(listWithAliases[position].created_at, DateTimeUtils.DATETIMEUTILS.SHORT_DATE),
                DateTimeUtils.turnStringIntoLocalString(listWithAliases[position].created_at, DateTimeUtils.DATETIMEUTILS.TIME)
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

        val colorArray = arrayOf(
            arrayOf(forwarded, ContextCompat.getColor(context, color1)),
            arrayOf(replied, ContextCompat.getColor(context, color2)),
            arrayOf(sent, ContextCompat.getColor(context, color3)),
            arrayOf(blocked, ContextCompat.getColor(context, color4))
        ).maxByOrNull { it[0].toFloat() }

        holder.mDescription.setTextColor(colorArray?.get(1) as Int)

        holder.mStar.backgroundTintList = ColorStateList.valueOf(colorArray[1] as Int)

        if (FavoriteAliasHelper(context).getFavoriteAliases()?.contains(listWithAliases[position].id) == true) {
            holder.mStar.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_starred))
        } else {
            holder.mStar.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star))
        }

        holder.mQGV.setGrades(
            listOf(GradeModel(colorArray[1] as Int, 0f), GradeModel(ContextCompat.getColor(context, R.color.colorSurface), 0.6f)), listOf(
                GradeModel(
                    colorArray[1] as Int, 0f
                ), GradeModel(ContextCompat.getColor(context, R.color.colorSurface), 0f)
            )
        )
        holder.mQGV.orientation = GradeOrientation.HORIZONTAL

        holder.mQGV.outlineProvider = ViewOutlineProvider.BACKGROUND
        holder.mQGV.clipToOutline = true
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
        fun onClickStar(pos: Int)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mCV: MaterialCardView = view.findViewById(R.id.recyclerview_list_CV)
        var mQGV: QuatroGradeView = view.findViewById(R.id.recyclerview_list_QGV)
        var mTitle: TextView = view.findViewById(R.id.aliases_recyclerview_list_title)
        var mStar: ImageView = view.findViewById(R.id.aliases_recyclerview_list_star)
        var mDescription: TextView =
            view.findViewById(R.id.aliases_recyclerview_list_description)

        init {
            mCV.setOnClickListener(this)
            mStar.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            if (p0.id == R.id.recyclerview_list_CV) {
                onAliasClickListener.onClick(adapterPosition)
            } else if (p0.id == R.id.aliases_recyclerview_list_star) {
                onAliasClickListener.onClickStar(adapterPosition)
            }
        }

    }
}

