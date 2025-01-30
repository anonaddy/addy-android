package host.stjin.anonaddy.ui.customviews

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.animation.DecelerateInterpolator
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import kotlin.math.roundToInt


class HomeStatCardView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {
    private var onClicklistener: OnLayoutClickedListener? = null
    var description: TextView? = null
    private var buttonText: TextView? = null
    private var title: TextView? = null
    private var icon: ImageView? = null
    private var progress: ProgressBar? = null
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
            description?.visibility = GONE
        } else {
            description?.text = text
            description?.visibility = VISIBLE
        }
    }

    fun setButtonText(text: String?) {
        buttonText?.text = text
    }

    fun setTitle(text: String?) {
        if (text.isNullOrEmpty()) {
            title?.text = text
            title?.visibility = GONE
        } else {
            title?.text = text
            title?.visibility = VISIBLE
        }
    }

    fun setProgress(progressValue: Float) {
        // Not gonna round 0 to Int, that will fail
        if (progressValue > 0) {
            progress?.animateTo(progressValue.roundToInt(), 0)
        }
    }


    private fun ProgressBar.animateTo(progressTo: Int, startDelay: Long) {
        val animation = ObjectAnimator.ofInt(
            this,
            "progress",
            this.progress,
            progressTo
        )
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        animation.startDelay = startDelay
        animation.start()
    }

    private fun setImageResourceIcons(startIcon: Int?, endIcon: Int?) {
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
        inflater.inflate(R.layout.home_stat_card, this)
        cardView = findViewById(R.id.home_stat_card_cardview)
        linearLayout = findViewById(R.id.home_stat_card_button_LL1)
        icon = findViewById(R.id.home_stat_card_icon)
        progress = findViewById(R.id.home_stat_card_progress)
        title = findViewById(R.id.home_stat_card_title)
        description = findViewById(R.id.home_stat_card_desc)
        buttonText = findViewById(R.id.home_stat_card_button_text)

        if (attrs != null) {
            // Get attributes
            val a = getContext()
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.HomeStatCardView,
                    0, 0
                )


            // Set elevation (if set)
            if (a.getFloat(R.styleable.HomeStatCardView_StatCardViewElevation, 999F) != 999F) {
                cardView?.cardElevation = a.getFloat(R.styleable.HomeStatCardView_StatCardViewElevation, 999F)
            }

            // Set title and description
            setTitle(a.getString(R.styleable.HomeStatCardView_StatCardViewTitle))
            setDescription(a.getString(R.styleable.HomeStatCardView_StatCardViewDescription))
            setButtonText(a.getString(R.styleable.HomeStatCardView_StatCardViewButtonText))


            // Set icons
            setImageResourceIcons(
                a.getResourceId(R.styleable.HomeStatCardView_StatCardViewIcon, 0),
                null
            )

            // Set elevation (if set)
            if (a.getBoolean(R.styleable.HomeStatCardView_StatCardViewShowButton, false)) {
                linearLayout?.visibility = VISIBLE
                linearLayout?.setOnClickListener(layoutClickedListener)
            } else {
                linearLayout?.visibility = GONE
                cardView?.setOnClickListener(layoutClickedListener)
            }


            a.recycle()
        }
    }

}