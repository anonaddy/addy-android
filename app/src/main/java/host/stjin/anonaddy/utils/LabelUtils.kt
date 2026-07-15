package host.stjin.anonaddy.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.Labels

object LabelUtils {

    fun populateLabelsChipGroup(
        context: Context,
        chipGroup: ChipGroup,
        labels: List<Labels>,
        checkedLabelIds: List<String>,
        onCheckedChanged: ((String, Boolean) -> Unit)? = null
    ) {
        chipGroup.removeAllViewsInLayout()
        chipGroup.requestLayout()
        chipGroup.invalidate()

        for (label in labels) {
            val chip = LayoutInflater.from(context).inflate(R.layout.chip_view, chipGroup, false) as Chip
            chip.text = label.name
            chip.tag = label.id
            chip.isCheckable = true
            chip.isChecked = checkedLabelIds.contains(label.id)

            try {
                val colorInt = Color.parseColor(label.colour)
                val alphaColor = Color.argb(
                    (0.2 * 255).toInt(),
                    Color.red(colorInt),
                    Color.green(colorInt),
                    Color.blue(colorInt)
                )

                val isDarkMode =
                    (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                val hsv = FloatArray(3)
                Color.colorToHSV(colorInt, hsv)
                if (isDarkMode) {
                    hsv[2] = 1.0f
                    hsv[1] = Math.max(0f, hsv[1] - 0.2f)
                } else {
                    hsv[2] = Math.min(1f, hsv[2] * 0.7f)
                    hsv[1] = Math.min(1f, hsv[1] * 1.2f)
                }
                val textColorInt = Color.HSVToColor(hsv)

                val defaultBgColor = chip.chipBackgroundColor?.defaultColor ?: Color.TRANSPARENT
                val defaultTextColor = chip.textColors?.defaultColor ?: Color.GRAY

                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                )

                val bgColors = intArrayOf(alphaColor, defaultBgColor)
                val textColors = intArrayOf(textColorInt, defaultTextColor)

                chip.chipBackgroundColor = ColorStateList(states, bgColors)
                chip.setTextColor(ColorStateList(states, textColors))
                chip.chipStrokeWidth = 0f
                chip.checkedIconTint = ColorStateList.valueOf(textColorInt)
                chip.isChipIconVisible = false
            } catch (e: Exception) {
                // Fallback
            }

            if (onCheckedChanged != null) {
                chip.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChanged(label.id, isChecked)
                }
            }

            chipGroup.addView(chip)
        }
    }

    fun setupCollapsibleHeader(header: View, chipGroup: View, arrow: View) {
        header.setOnClickListener {
            if (chipGroup.visibility == View.VISIBLE) {
                chipGroup.visibility = View.GONE
                arrow.animate().rotation(0f).start()
            } else {
                chipGroup.visibility = View.VISIBLE
                arrow.animate().rotation(180f).start()
            }
        }
    }
}
