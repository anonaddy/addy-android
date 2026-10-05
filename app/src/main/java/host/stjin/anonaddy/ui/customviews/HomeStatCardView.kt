package host.stjin.anonaddy.ui.customviews

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.withStyledAttributes
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

    private val layoutClickedListener =

        OnClickListener {
            // If the OnClickListener was set (an action was assigned) call Onclick.
            // Else flip the switch
            if (onClicklistener != null) {
                onClicklistener?.onClick()
            }
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
            getContext()
                .withStyledAttributes(
                    attrs,
                    R.styleable.HomeStatCardView,
                    0, 0
                ) {


                    // Set elevation (if set)
                    if (getFloat(R.styleable.HomeStatCardView_StatCardViewElevation, 999F) != 999F) {
                        cardView?.cardElevation = getFloat(R.styleable.HomeStatCardView_StatCardViewElevation, 999F)
                    }

                    // Set title and description
                    setTitle(getString(R.styleable.HomeStatCardView_StatCardViewTitle))
                    setDescription(getString(R.styleable.HomeStatCardView_StatCardViewDescription))
                    setButtonText(getString(R.styleable.HomeStatCardView_StatCardViewButtonText))


                    // Set icon
                    val iconRes = getResourceId(R.styleable.HomeStatCardView_StatCardViewIcon, 0)
                    if (iconRes != 0) {
                        setIconResource(iconRes)
                    }

                    // Set elevation (if set)
                    if (getBoolean(R.styleable.HomeStatCardView_StatCardViewShowButton, false)) {
                        linearLayout?.visibility = VISIBLE
                        linearLayout?.setOnClickListener(layoutClickedListener)
                    } else {
                        linearLayout?.visibility = GONE
                        cardView?.setOnClickListener(layoutClickedListener)
                    }


                }
        }
    }

    fun getOnLayoutClickedListener(): OnLayoutClickedListener? {
        return onClicklistener
    }

    fun setOnLayoutClickedListener(listener: OnLayoutClickedListener?) {
        this.onClicklistener = listener
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

    fun setIconResource(iconRes: Int?) {
        if (iconRes != null && iconRes != 0) {
            icon?.setImageResource(iconRes)
        }
    }

    fun interface OnLayoutClickedListener {
        fun onClick()
    }
}
