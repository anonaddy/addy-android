package host.stjin.anonaddy.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.LruCache
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.futured.donut.DonutSection
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.AliasesRecyclerviewListItemBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import org.ocpsoft.prettytime.PrettyTime

class AliasDiffCallback : DiffUtil.ItemCallback<Aliases>() {
    override fun areItemsTheSame(oldItem: Aliases, newItem: Aliases): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Aliases, newItem: Aliases): Boolean {
        return oldItem == newItem
    }
}

class AliasAdapter(
    listWithAliases: List<Aliases> = emptyList(),
    context: Context
) : ListAdapter<Aliases, AliasAdapter.ViewHolder>(AliasDiffCallback()) {

    companion object {
        private val badgeBitmapCache = LruCache<String, Bitmap>(100)
    }

    var onAliasClickListener: AliasInterface? = null
    private val aliasWatcher = AliasWatcher(context)
    private var aliasesToWatch: Set<String> = aliasWatcher.getAliasesToWatch()
    private var selectedAliases: ArrayList<Aliases> = arrayListOf()

    fun updateWatchedAliases() {
        aliasesToWatch = aliasWatcher.getAliasesToWatch()
        notifyItemRangeChanged(0, itemCount)
    }

    override fun submitList(list: List<Aliases>?) {
        aliasesToWatch = aliasWatcher.getAliasesToWatch()
        super.submitList(list)
    }

    override fun submitList(list: List<Aliases>?, commitCallback: Runnable?) {
        aliasesToWatch = aliasWatcher.getAliasesToWatch()
        super.submitList(list, commitCallback)
    }

    init {
        if (listWithAliases.isNotEmpty()) {
            submitList(listWithAliases)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AliasesRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        applySelectedOverlay(holder, position)

        val alias = getItem(position)
        holder.binding.aliasesRecyclerviewListTitle.text = alias.email

        val context = holder.binding.aliasesRecyclerviewListDescription.context
        val prettyTime = PrettyTime()
        val descriptionParts = mutableListOf<String>()

        // Add description if it exists
        alias.description?.let { descriptionParts.add(it) }

        // Add date information
        if (alias.deleted_at != null) {
            descriptionParts.add(
                context.getString(
                    R.string.deleted_at_s,
                    prettyTime.format(DateTimeUtils.convertStringToLocalTimeZoneDate(alias.deleted_at))
                )
            )
            // If there's no description for a deleted alias, show created_at as well
            if (alias.description == null) {
                descriptionParts.add(
                    context.getString(
                        R.string.created_at_s,
                        prettyTime.format(DateTimeUtils.convertStringToLocalTimeZoneDate(alias.created_at))
                    )
                )
            }
        } else {
            descriptionParts.add(
                context.getString(
                    R.string.created_at_s,
                    prettyTime.format(DateTimeUtils.convertStringToLocalTimeZoneDate(alias.created_at))
                )
            )
            descriptionParts.add(
                context.getString(
                    R.string.updated_at_s,
                    prettyTime.format(DateTimeUtils.convertStringToLocalTimeZoneDate(alias.updated_at))
                )
            )
        }

        holder.binding.aliasesRecyclerviewListDescription.text = descriptionParts.joinToString("\n")

        /*
        Labels using ImageSpan for text ellipsizing
         */
        if (!alias.labels.isNullOrEmpty()) {
            holder.binding.aliasesRecyclerviewListLabelsTv.visibility = View.VISIBLE
            val ssb = SpannableStringBuilder()
            val isDarkMode =
                (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            for (label in alias.labels!!) {
                val cacheKey = "${label.id}_${label.name}_${label.colour}_$isDarkMode"
                val bitmap = badgeBitmapCache.get(cacheKey) ?: run {
                    val badgeView = LayoutInflater.from(context).inflate(R.layout.layout_label_badge, holder.binding.root, false)
                    val text = badgeView.findViewById<TextView>(R.id.label_badge_text)
                    text.text = label.name

                    try {
                        val colorInt = label.colour.toColorInt()
                        val hsv = FloatArray(3)
                        Color.colorToHSV(colorInt, hsv)
                        if (isDarkMode) {
                            hsv[2] = 1.0f
                            hsv[1] = 0f.coerceAtLeast(hsv[1] - 0.2f)
                        } else {
                            hsv[2] = 1f.coerceAtMost(hsv[2] * 0.7f)
                            hsv[1] = 1f.coerceAtMost(hsv[1] * 1.2f)
                        }
                        val textColorInt = Color.HSVToColor(hsv)

                        // Background
                        val bgDrawable = GradientDrawable()
                        bgDrawable.shape = GradientDrawable.RECTANGLE
                        bgDrawable.cornerRadius = 100f // Large radius for rounded capsule
                        val alphaColor = Color.argb(
                            (0.2 * 255).toInt(),
                            Color.red(colorInt),
                            Color.green(colorInt),
                            Color.blue(colorInt)
                        )
                        bgDrawable.setColor(alphaColor)
                        badgeView.background = bgDrawable
                        text.setTextColor(textColorInt)

                    } catch (_: Exception) {
                        // Fallback
                    }

                    // Measure and layout badgeView
                    val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    badgeView.measure(spec, spec)
                    badgeView.layout(0, 0, badgeView.measuredWidth, badgeView.measuredHeight)

                    val bmp = createBitmap(badgeView.measuredWidth, badgeView.measuredHeight)
                    val canvas = Canvas(bmp)
                    badgeView.draw(canvas)
                    badgeBitmapCache.put(cacheKey, bmp)
                    bmp
                }

                ssb.append(" ")
                val alignment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ImageSpan.ALIGN_CENTER else ImageSpan.ALIGN_BASELINE
                val span = ImageSpan(context, bitmap, alignment)
                ssb.setSpan(span, ssb.length - 1, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append(" ") // small gap
            }
            holder.binding.aliasesRecyclerviewListLabelsTv.text = ssb
        } else {
            holder.binding.aliasesRecyclerviewListLabelsTv.visibility = View.GONE
        }

        /*
        CHART
         */
        val forwarded = alias.emails_forwarded.toFloat()
        val replied = alias.emails_replied.toFloat()
        val sent = alias.emails_sent.toFloat()
        val blocked = alias.emails_blocked.toFloat()

        val color1 = if (alias.active) R.color.portalOrange else R.color.md_grey_500
        val color2 = if (alias.active) R.color.portalBlue else R.color.md_grey_600
        val color3 = if (alias.active) R.color.easternBlue else R.color.md_grey_700
        val color4 = if (alias.active) R.color.softRed else R.color.md_grey_800

        val listOfDonutSection: ArrayList<DonutSection> = arrayListOf()

        // If there are no statistics, set the emptyDonut value to 1 so that a donut can be drawn
        val emptyDonut = if (alias.emails_forwarded == 0 &&
            alias.emails_replied == 0 &&
            alias.emails_sent == 0 &&
            alias.emails_blocked == 0
        ) 1 else 0

        // DONUT
        val section1 = DonutSection(
            name = holder.binding.aliasesRecyclerviewListChart.context.resources.getQuantityString(R.plurals.d_forwarded, forwarded.toInt(), forwarded.toInt()),
            color = ContextCompat.getColor(holder.binding.aliasesRecyclerviewListChart.context, color1),
            amount = forwarded + emptyDonut
        )
        // Always show section 1
        listOfDonutSection.add(section1)

        if (replied > 0) {
            val section2 = DonutSection(
                name = holder.binding.aliasesRecyclerviewListChart.context.resources.getQuantityString(R.plurals.d_replied, replied.toInt(), replied.toInt()),
                color = ContextCompat.getColor(holder.binding.aliasesRecyclerviewListChart.context, color2),
                amount = replied
            )
            listOfDonutSection.add(section2)
        }

        if (sent > 0) {
            val section3 = DonutSection(
                name = holder.binding.aliasesRecyclerviewListChart.context.resources.getQuantityString(R.plurals.d_sent, sent.toInt(), sent.toInt()),
                color = ContextCompat.getColor(holder.binding.aliasesRecyclerviewListChart.context, color3),
                amount = sent
            )
            listOfDonutSection.add(section3)
        }

        if (blocked > 0) {
            val section4 = DonutSection(
                name = holder.binding.aliasesRecyclerviewListChart.context.resources.getQuantityString(R.plurals.d_blocked, blocked.toInt(), blocked.toInt()),
                color = ContextCompat.getColor(holder.binding.aliasesRecyclerviewListChart.context, color4),
                amount = blocked
            )
            listOfDonutSection.add(section4)
        }

        holder.binding.aliasesRecyclerviewListChart.cap = listOfDonutSection.sumOf { it.amount.toInt() }.toFloat()
        // Sort the list by amount so that the biggest number will fill the whole ring
        holder.binding.aliasesRecyclerviewListChart.submitData(listOfDonutSection.sortedBy { it.amount })
        // DONUT

        holder.binding.aliasesRecyclerviewListWatchedIcon.visibility =
            if (aliasesToWatch.contains(alias.id)) View.VISIBLE else View.GONE

        holder.binding.aliasesRecyclerviewListPinned.visibility = if (alias.pinned) View.VISIBLE else View.GONE
    }

    private fun applySelectedOverlay(holder: ViewHolder, position: Int) {
        // Check if the item is selected
        if (selectedAliases.contains(getItem(position))) {
            holder.binding.recyclerviewListCV.cardElevation = 0f
            holder.binding.aliasesRecyclerviewListLL0.setBackgroundColor(ContextCompat.getColor(holder.binding.aliasesRecyclerviewListLL0.context, R.color.selected_background_color))
            holder.binding.aliasesRecyclerviewListCopy.setImageDrawable(ContextCompat.getDrawable(holder.binding.aliasesRecyclerviewListCopy.context, R.drawable.ic_check))
        } else {
            holder.binding.recyclerviewListCV.cardElevation = holder.binding.recyclerviewListCV.context.resources.getDimension(R.dimen.cardview_default_elevation)
            holder.binding.aliasesRecyclerviewListLL0.setBackgroundColor(0)
            holder.binding.aliasesRecyclerviewListCopy.setImageDrawable(ContextCompat.getDrawable(holder.binding.aliasesRecyclerviewListCopy.context, R.drawable.ic_copy))
        }
    }



    fun setClickOnAliasClickListener(listener: AliasInterface) {
        onAliasClickListener = listener
    }

    fun unselectAliases() {
        for (alias in selectedAliases) {
            val findAliasPosition = currentList.indexOfFirst { it == alias }
            if (findAliasPosition > -1) {
                notifyItemChanged(findAliasPosition)
            }
        }
        selectedAliases.clear()
    }

    interface AliasInterface {
        fun onClick(pos: Int)
        fun onClickCopy(pos: Int, view: View)
        fun onSelectionMode(selectionMode: Boolean, selectedAliases: ArrayList<Aliases>) { /* By default, don't implement */
        }
    }

    inner class ViewHolder(val binding: AliasesRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {

        init {
            binding.aliasesRecyclerviewListCopy.setOnClickListener(this)
            binding.recyclerviewListCV.setOnClickListener(this)
            binding.recyclerviewListCV.setOnLongClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            if (v.id == binding.recyclerviewListCV.id) {
                if (selectedAliases.any()) {
                    selectItem(pos)
                } else {
                    onAliasClickListener?.onClick(pos)
                }
            } else if (v.id == binding.aliasesRecyclerviewListCopy.id) {
                onAliasClickListener?.onClickCopy(pos, v)
            }
        }

        override fun onLongClick(v: View): Boolean {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return false

            if (v.id == binding.recyclerviewListCV.id) {
                selectItem(pos)
            }
            return true
        }

        private fun selectItem(adapterPosition: Int) {
            binding.recyclerviewListCV.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val item = getItem(adapterPosition)
            if (selectedAliases.contains(item)) {
                selectedAliases.remove(item)
            } else {
                if (selectedAliases.count() < 25) {
                    selectedAliases.add(item)
                } else {
                    Toast.makeText(
                        binding.recyclerviewListCV.context,
                        binding.recyclerviewListCV.context.resources.getString(R.string.alias_multiple_selection_max_reached),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            onAliasClickListener?.onSelectionMode(
                selectionMode = selectedAliases.isNotEmpty(),
                selectedAliases = selectedAliases
            )

            notifyItemChanged(adapterPosition)
        }
    }
}
