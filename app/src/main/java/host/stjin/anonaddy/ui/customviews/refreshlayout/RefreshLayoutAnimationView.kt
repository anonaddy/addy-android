package host.stjin.anonaddy.ui.customviews.refreshlayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.min


class RefreshLayoutAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var shouldRefreshOnRelease: Boolean = false
    private var alreadyVibrated: Boolean = false

    private var pullHeight: Int = 0
    private var pullDelta: Int = 0
    private var widthOffset: Float = 0.toFloat()

    private var aniStatus = AnimatorStatus.PULL_DOWN

    private var backPaint: Paint? = null
    private var outPaint: Paint? = null
    private var path: Path? = null

    private var radius: Int = 0
    private var localWidth: Int = 0
    private var localHeight: Int = 0

    private var isRefreshing = true

    private var lastHeight: Int = 0

    private val relHeight: Int get() = (spriDeta * (1 - relRatio)).toInt()


    private var start1: Long = 0
    private var stop: Long = 0
    private var spriDeta: Int = 0

    private val relRatio: Float
        get() {
            if (System.currentTimeMillis() >= stop) {
                return 1f
            }
            val ratio = (System.currentTimeMillis() - start1) / REL_DRAG_DUR.toFloat()
            return min(ratio, 1f)
        }

    private var onViewAniDone: OnViewAniDone? = null

    internal enum class AnimatorStatus {
        PULL_DOWN,
        DRAG_DOWN,
        REL_DRAG,
        OUTER_CIR,
        REFRESHING,
        DONE;

        override fun toString(): String = when (this) {
            PULL_DOWN -> "pull down"
            DRAG_DOWN -> "drag down"
            REL_DRAG -> "release drag"
            OUTER_CIR -> "outer circle"
            REFRESHING -> "refreshing..."
            DONE -> "done!"
        }
    }

    init {
        initView(context)
    }

    private fun initView(context: Context) {

        pullHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 20f,
            context.resources.displayMetrics
        )
            .toInt()
        pullDelta = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 70f,
            context.resources.displayMetrics
        )
            .toInt()
        widthOffset = 0.5f
        backPaint = Paint()
        backPaint!!.isAntiAlias = true
        backPaint!!.style = Paint.Style.FILL
        backPaint!!.color = -0x746f51

        outPaint = Paint()
        outPaint!!.isAntiAlias = true
        outPaint!!.color = -0x1
        outPaint!!.style = Paint.Style.STROKE
        outPaint!!.strokeWidth = 5f


        path = Path()

    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        var tempHeightMeasureSpec = heightMeasureSpec
        val height = MeasureSpec.getSize(tempHeightMeasureSpec)
        if (height > pullDelta + pullHeight) {
            tempHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                pullDelta + pullHeight,
                MeasureSpec.getMode(tempHeightMeasureSpec)
            )
        }
        super.onMeasure(widthMeasureSpec, tempHeightMeasureSpec)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            radius = height / 6
            localWidth = width
            localHeight = height

            when {
                localHeight < pullHeight -> aniStatus = AnimatorStatus.PULL_DOWN
            }

            when {
                aniStatus == AnimatorStatus.PULL_DOWN && localHeight >= pullHeight -> aniStatus =
                    AnimatorStatus.DRAG_DOWN
            }

            // If almost scrolled to the bottom, change status of shouldRefreshOnRelease
            shouldRefreshOnRelease = localHeight >= pullDelta
            if (localHeight >= pullDelta) {
                if (!alreadyVibrated) {
                    this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    alreadyVibrated = true
                }
            } else {
                alreadyVibrated = false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (aniStatus) {
            AnimatorStatus.PULL_DOWN -> {
                canvas.drawRect(0f, 0f, localWidth.toFloat(), localHeight.toFloat(), backPaint!!)
            }
            AnimatorStatus.REL_DRAG, AnimatorStatus.DRAG_DOWN -> {
                drawDrag(canvas)
            }
            AnimatorStatus.OUTER_CIR -> {
                invalidate()
            }
            AnimatorStatus.REFRESHING -> {
                invalidate()
            }
            AnimatorStatus.DONE -> {
                invalidate()
            }
        }

        if (aniStatus == AnimatorStatus.REL_DRAG) {
            val params = layoutParams
            var height: Int
            do {
                height = relHeight
            } while (height == lastHeight && relRatio != 1f)
            lastHeight = height
            params.height = pullHeight + height
            requestLayout()
        }

    }

    private fun drawDrag(canvas: Canvas) {
        canvas.drawRect(0f, 0f, localWidth.toFloat(), pullHeight.toFloat(), backPaint!!)
        path!!.reset()
        path!!.moveTo(0f, pullHeight.toFloat())
        path!!.quadTo(
            widthOffset * localWidth, (pullHeight + (localHeight - pullHeight) * 2).toFloat(),
            localWidth.toFloat(), pullHeight.toFloat()
        )
        canvas.drawPath(path!!, backPaint!!)
    }

    fun setRefreshing(isFresh: Boolean) {
        isRefreshing = isFresh
    }

    fun releaseDrag() {
        start1 = System.currentTimeMillis()
        stop = start1 + REL_DRAG_DUR
        aniStatus = AnimatorStatus.REL_DRAG
        spriDeta = localHeight - pullHeight
        requestLayout()
    }

    fun setOnViewAniDone(onViewAniDone: OnViewAniDone) {
        this.onViewAniDone = onViewAniDone
    }

    interface OnViewAniDone {
        fun viewAniDone()
    }

    fun setAniBackColor(color: Int) {
        backPaint!!.color = color
    }

    fun setAniForeColor(color: Int) {
        outPaint!!.color = color
        setBackgroundColor(color)
    }

    companion object {
        private const val REL_DRAG_DUR: Long = 200
    }
}