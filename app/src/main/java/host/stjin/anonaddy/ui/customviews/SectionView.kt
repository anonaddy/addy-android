package host.stjin.anonaddy.ui.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import host.stjin.anonaddy.R

class SectionView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {
    private var listener: OnSwitchCheckedChangedListener? = null
    private var onClicklistener: OnLayoutClickedListener? = null
    private var onLongClicklistener: OnLayoutLongClickedListener? = null
    private var switchMaterial: SwitchMaterial? = null
    var description: TextView? = null
    private var progressBar: ProgressBar? = null
    private var title: TextView? = null
    private var iconStart: ImageView? = null
    private var iconEnd: ImageView? = null

    private var linearLayout: LinearLayout? = null
    private var cardView: CardView? = null


    fun getOnLayoutLongClickedListener(): OnLayoutLongClickedListener? {
        return onLongClicklistener
    }

    fun setOnLayoutLongClickedListener(listener: OnLayoutLongClickedListener?) {
        this.onLongClicklistener = listener
    }

    fun getOnLayoutClickedListener(): OnLayoutClickedListener? {
        return onClicklistener
    }

    fun setOnLayoutClickedListener(listener: OnLayoutClickedListener?) {
        this.onClicklistener = listener
    }

    fun getOnSwitchCheckedChangedListener(): OnSwitchCheckedChangedListener? {
        return listener
    }

    fun setOnSwitchCheckedChangedListener(listener: OnSwitchCheckedChangedListener?) {
        this.listener = listener
    }

    private val switchCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { compoundButton, b -> listener?.onCheckedChange(compoundButton, b) }

    private val layoutClickedListener =
        OnClickListener {
            // If the OnClickListener was set (an action was assigned) call Onclick.
            // Else flip the switch
            if (onClicklistener != null) {
                onClicklistener?.onClick()
            } else {
                setSwitchChecked(!getSwitchChecked())
            }
        }

    private val layoutLongClickedListener =
        OnLongClickListener {
            // If the OnLongClickListener was set (an action was assigned) call onLongClick.
            // Else flip the switch
            if (onLongClicklistener != null) {
                onLongClicklistener?.onLongClick()
            }
            false
        }

    fun setSwitchChecked(boolean: Boolean) {
        switchMaterial?.isChecked = boolean
    }

    fun getSwitchChecked(): Boolean {
        return switchMaterial?.isChecked == true
    }

    fun setLayoutEnabled(boolean: Boolean) {
        switchMaterial?.isEnabled = boolean
        switchMaterial?.isClickable = boolean

        linearLayout?.alpha = if (boolean) 1f else 0.5f

        if (boolean) {
            linearLayout?.setOnClickListener(layoutClickedListener)
            linearLayout?.setOnLongClickListener(layoutLongClickedListener)
        } else {
            linearLayout?.setOnClickListener(null)
            linearLayout?.setOnLongClickListener(null)
        }
    }

    fun showProgressBar(boolean: Boolean) {
        progressBar?.visibility = if (boolean) View.VISIBLE else View.GONE
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

    fun setSectionAlert(boolean: Boolean) {
        if (boolean) {
            iconStart?.background = context?.let { ContextCompat.getDrawable(it, R.drawable.custom_view_dialog_circle) }
            // The tint color is set to white
            ImageViewCompat.setImageTintList(iconStart!!,
                context?.let { ContextCompat.getColor(it, android.R.color.white) }?.let { ColorStateList.valueOf(it) })
        } else {
            iconStart?.background = null
            // The tint color is set to default
            ImageViewCompat.setImageTintList(iconStart!!,
                context?.let { ContextCompat.getColor(it, R.color.colorControlNormal) }?.let { ColorStateList.valueOf(it) })
        }
    }

    fun setImageResourceIcons(startIcon: Int?, endIcon: Int?) {
        if (startIcon != null) {
            iconStart?.setImageResource(startIcon)
        }
        if (endIcon != null) {
            iconEnd?.setImageResource(endIcon)
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
        inflater.inflate(R.layout.custom_view_section, this)
        cardView = findViewById(R.id.custom_view_section_CV)
        linearLayout = findViewById(R.id.custom_view_section_LL)
        iconStart = findViewById(R.id.custom_view_section_start_icon)
        iconEnd = findViewById(R.id.custom_view_section_end_icon)
        title = findViewById(R.id.custom_view_section_title)
        description = findViewById(R.id.custom_view_section_desc)
        switchMaterial = findViewById(R.id.custom_view_section_switch)
        progressBar = findViewById(R.id.custom_view_section_progressbar)


        if (attrs != null) {
            // Get attributes
            val a = getContext()
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.SectionView,
                    0, 0
                )

            // Set ripple, default is enabled. Ripple pref is only set once
            if (!a.getBoolean(R.styleable.SectionView_sectionRippleEffect, true)) {
                linearLayout?.background = null
            }

            // Set title and description
            setTitle(a.getString(R.styleable.SectionView_sectionTitle))
            setDescription(a.getString(R.styleable.SectionView_sectionDescription))

            // Set section starticon background and change icon color to white
            setSectionAlert(a.getBoolean(R.styleable.SectionView_sectionAlert, false))

            // Get colorAccent
            val hasColorAccentDefined = a.hasValue(R.styleable.SectionView_sectionColorAccent)

            // Set colorAccent on the title and the startIcon only if set
            if (hasColorAccentDefined) {
                val accentColorResource = a.getResourceId(
                    R.styleable.SectionView_sectionColorAccent,
                    0
                )
                val imageTintColor = context?.let { ContextCompat.getColor(it, accentColorResource) }
                imageTintColor?.let { title?.setTextColor(it) }

                if (iconStart != null) {
                    // The tint color is only set once, won't be changes at runtime.
                    ImageViewCompat.setImageTintList(iconStart!!, imageTintColor?.let { ColorStateList.valueOf(it) })
                }
            }


            // Set icons
            setImageResourceIcons(
                a.getResourceId(R.styleable.SectionView_sectionStartIcon, 0),
                a.getResourceId(R.styleable.SectionView_sectionEndIcon, 0)
            )

            // Set switch this is ony done at init, default is invisible
            if (a.getBoolean(R.styleable.SectionView_sectionShowSwitch, false)) {
                switchMaterial?.visibility = VISIBLE
                switchMaterial?.setOnCheckedChangeListener(switchCheckedChangeListener)
            }

            // Set if switch is checked or not
            switchMaterial?.isChecked = a.getBoolean(R.styleable.SectionView_sectionSwitchChecked, false)


            // Set layout enabled
            setLayoutEnabled(a.getBoolean(R.styleable.SectionView_sectionEnabled, true))

            a.recycle()
        }
    }

}