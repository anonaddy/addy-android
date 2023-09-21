package host.stjin.anonaddy.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R


class DashboardStatCardView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {
    private var onClicklistener: OnLayoutClickedListener? = null
    var description: TextView? = null
    private var title: TextView? = null
    private var icon: ImageView? = null
    private var linearLayout: LinearLayout? = null
    private var cardView: MaterialCardView? = null


    fun getOnLayoutClickedListener(): OnLayoutClickedListener? {
        return onClicklistener
    }

    fun setOnLayoutClickedListener(listener: OnLayoutClickedListener?) {
        this.onClicklistener = listener
    }

    private val layoutClickedListener =
        OnClickListener {
            // If the OnClickListener was set (an action was assigned) call Onclick.
            // Else flip the switch
            if (onClicklistener != null) {
                onClicklistener?.onClick()
            }
        }


    fun setDescription(text: String?) {
        if (text.isNullOrEmpty()) {
            description?.text = null
            description?.visibility = View.GONE
        } else {
            description?.text = text
            description?.visibility = View.VISIBLE
        }
    }

    fun setTitle(text: String?) {
        if (text.isNullOrEmpty()) {
            title?.text = text
            title?.visibility = View.GONE
        } else {
            title?.text = text
            title?.visibility = View.VISIBLE
        }
    }


    fun setImageResourceIcons(startIcon: Int?, endIcon: Int?) {
        if (startIcon != null) {
            icon?.setImageResource(startIcon)
        }
        if (endIcon != null) {
            icon?.setImageResource(endIcon)
        }
    }


    interface OnSwitchCheckedChangedListener {
        fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean)
    }

    interface OnLayoutClickedListener {
        fun onClick()
    }

    interface OnLayoutLongClickedListener {
        fun onLongClick()
    }

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.dashboard_stat_card, this)
        cardView = findViewById(R.id.dashboard_stat_card_cardview)
        linearLayout = findViewById(R.id.custom_view_section_LL)
        icon = findViewById(R.id.dashboard_stat_card_icon)
        title = findViewById(R.id.dashboard_stat_card_title)
        description = findViewById(R.id.dashboard_stat_card_desc)

        if (attrs != null) {
            // Get attributes
            val a = getContext()
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.DashboardStatCardView,
                    0, 0
                )


            // Set elevation (if set)
            if (a.getFloat(R.styleable.DashboardStatCardView_StatCardViewElevation, 999F) != 999F) {
                cardView?.cardElevation = a.getFloat(R.styleable.DashboardStatCardView_StatCardViewElevation, 999F)
            }

            // Set title and description
            setTitle(a.getString(R.styleable.DashboardStatCardView_StatCardViewTitle))
            setDescription(a.getString(R.styleable.DashboardStatCardView_StatCardViewDescription))


            // Set icons
            setImageResourceIcons(
                a.getResourceId(R.styleable.DashboardStatCardView_StatCardViewIcon, 0),
                null
            )

            // Set elevation (if set)
            if (a.getBoolean(R.styleable.DashboardStatCardView_StatCardViewShowButton, false)) {
                linearLayout?.visibility = View.VISIBLE
            } else {
                linearLayout?.visibility = View.GONE
            }

            linearLayout?.setOnClickListener(layoutClickedListener)

            a.recycle()
        }
    }

}