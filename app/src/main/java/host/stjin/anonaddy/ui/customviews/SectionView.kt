package host.stjin.anonaddy.ui.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import host.stjin.anonaddy.R

class SectionView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {
    private var listener: OnSwitchCheckedChangedListener? = null
    private var onClicklistener: OnLayoutClickedListener? = null
    var switchMaterial: SwitchMaterial? = null
    var description: TextView? = null
    var progressBar: ProgressBar? = null
    var title: TextView? = null

    var linearLayout: LinearLayout? = null


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
        } else {
            linearLayout?.setOnClickListener(null)
        }
    }

    fun showProgressBar(boolean: Boolean) {
        progressBar?.visibility = if (boolean) View.VISIBLE else View.GONE
    }

    fun setDescription(text: String) {
        description?.text = text
    }

    fun setTitle(text: String) {
        title?.text = text
    }

    interface OnSwitchCheckedChangedListener {
        fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean)
    }

    interface OnLayoutClickedListener {
        fun onClick()
    }

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.custom_view_section, this)
        linearLayout = findViewById(R.id.custom_view_section_LL)
        val iconStart = findViewById<ImageView>(R.id.custom_view_section_start_icon)
        val iconEnd = findViewById<ImageView>(R.id.custom_view_section_end_icon)
        title = findViewById(R.id.custom_view_section_title)
        description = findViewById(R.id.custom_view_section_desc)
        switchMaterial = findViewById(R.id.custom_view_section_switch)
        progressBar = findViewById(R.id.custom_view_section_progressbar)

        //TODO delete these from the styles.xml
        /*
        <item name="android:clickable">false</item>
        <item name="android:focusable">false</item>
        <item name="android:background">@null</item>
         */

        if (attrs != null) {
            val a = getContext()
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.SectionView,
                    0, 0
                )

            // Set ripple, default is enabled
            if (!a.getBoolean(R.styleable.SectionView_sectionRippleEffect, true)) {
                linearLayout?.background = null
            }


            // Set title and description
            a.getString(R.styleable.SectionView_sectionTitle)?.let { setTitle(it) }
            a.getString(R.styleable.SectionView_sectionDescription)?.let { setDescription(it) }

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
                ImageViewCompat.setImageTintList(iconStart, imageTintColor?.let { ColorStateList.valueOf(it) })
            }


            // Set icon, this is only done at init
            iconStart.setImageResource(a.getResourceId(R.styleable.SectionView_sectionStartIcon, 0))
            iconEnd.setImageResource(a.getResourceId(R.styleable.SectionView_sectionEndIcon, 0))

            // Set switch this is ony done at init, default is invisible
            if (a.getBoolean(R.styleable.SectionView_sectionShowSwitch, false)) {
                switchMaterial?.visibility = VISIBLE
                switchMaterial?.setOnCheckedChangeListener(switchCheckedChangeListener)
            }


            // Set layout enabled
            setLayoutEnabled(a.getBoolean(R.styleable.SectionView_sectionEnabled, true))


            a.recycle()
        }
    }

}