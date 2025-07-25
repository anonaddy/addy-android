package host.stjin.anonaddy.ui.customviews.refreshlayout

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import host.stjin.anonaddy.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.core.content.withStyledAttributes

open class RefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var headerBackColor = -0x746f51
    private var headerForeColor = -0x1
    var shouldShowRefreshLayoutOnScroll = true

    private var pullHeight: Float = 0.toFloat()
    private var headerHeight: Float = 0.toFloat()
    private var childView: View? = null
    private var header: RefreshLayoutAnimationView? = null

    private var isRefreshing: Boolean = false

    private var touchStartY: Float = 0.toFloat()

    private var touchCurY: Float = 0.toFloat()

    private var upTopAnimator: ValueAnimator? = null

    private val decelerateInterpolator = DecelerateInterpolator(10f)

    private var onRefreshListener: OnRefreshListener? = null


    private var mTouchSlop = 0

    init {
        init(context, attrs)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?
    ) {

        if (childCount > 1) {
            throw RuntimeException("you can only attach one child")
        }
        setAttrs(attrs)
        pullHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 150f,
            context.resources.displayMetrics
        )
        headerHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10f,
            context.resources.displayMetrics
        )

        this.post {
            childView = getChildAt(0)
            addHeaderView()
        }

        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

    }

    private fun setAttrs(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.RefreshLayout) {

            headerBackColor = getColor(R.styleable.RefreshLayout_AniBackColor, headerBackColor)
            headerForeColor = getColor(R.styleable.RefreshLayout_AniForeColor, headerForeColor)

        }
    }

    private fun addHeaderView() {
        header = RefreshLayoutAnimationView(context)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, 0)
        params.gravity = Gravity.TOP
        header!!.layoutParams = params

        addViewInternal(header!!)
        header!!.setAniBackColor(headerBackColor)
        header!!.setAniForeColor(headerForeColor)

        setUpChildAnimation()
    }

    private fun setUpChildAnimation() {
        if (childView == null) {
            return
        }
        upTopAnimator = ValueAnimator.ofFloat(headerHeight, 0f)
        upTopAnimator!!.addUpdateListener { animation ->
            var value = animation.animatedValue as Float
            value *= decelerateInterpolator.getInterpolation(value / headerHeight)
            if (childView != null) {
                childView!!.translationY = animation.animatedValue as Float
            }
            header!!.layoutParams.height = value.toInt()
            header!!.requestLayout()
        }
        upTopAnimator!!.duration = BACK_TOP_DUR

        header!!.setOnViewAniDone(object : RefreshLayoutAnimationView.OnViewAniDone {
            override fun viewAniDone() {
                upTopAnimator!!.start()
            }
        })
    }

    private fun addViewInternal(child: View) {
        super.addView(child)
    }

    override fun addView(child: View) {
        if (childCount >= 1) {
            throw RuntimeException("you can only attach one child")
        }

        childView = child
        super.addView(child)
        setUpChildAnimation()
    }

    private fun canChildScrollUp(): Boolean {
        return if (childView == null) {
            false
        } else !shouldShowRefreshLayoutOnScroll
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = ev.y
                touchCurY = touchStartY
            }
            MotionEvent.ACTION_MOVE -> {

                val curY = ev.y
                val dy = curY - touchStartY
                if (dy > 0 && !canChildScrollUp() && abs(dy) > mTouchSlop) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRefreshing) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                touchCurY = event.y
                var dy = touchCurY - touchStartY
                dy = min(pullHeight * 2, dy)
                dy = max(0f, dy)

                if (childView != null) {
                    val offsetY = decelerateInterpolator.getInterpolation(dy / 2f / pullHeight) * dy / 2
                    childView!!.translationY = offsetY

                    header!!.layoutParams.height = offsetY.toInt()
                    header!!.requestLayout()
                }

                onRefreshListener?.pullDown(dy, header!!.shouldRefreshOnRelease)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (childView != null) {
                    if (header!!.shouldRefreshOnRelease) {
                        val height = childView!!.translationY
                        val upBackAnimator = ValueAnimator.ofFloat(height, headerHeight)
                        upBackAnimator.addUpdateListener { animation ->
                            var value = animation.animatedValue as Float
                            if (childView != null) {
                                if (value < 0) {
                                    value = animation.animatedValue as Float
                                }
                                childView!!.translationY = value
                            }
                        }
                        upBackAnimator.duration = REL_DRAG_DUR
                        upBackAnimator.start()


                        header!!.releaseDrag()
                        isRefreshing = true
                        onRefreshListener?.refresh()

                    } else {
                        val height = childView!!.translationY
                        val backTopAni = ValueAnimator.ofFloat(height, 0f)
                        backTopAni.addUpdateListener { animation ->
                            var value = animation.animatedValue as Float
                            value *= decelerateInterpolator.getInterpolation(value)
                            if (childView != null) {
                                if (value < 0) {
                                    value = animation.animatedValue as Float
                                }
                                childView!!.translationY = value
                            }
                            header!!.layoutParams.height = value.toInt()

                            header!!.requestLayout()
                        }
                        backTopAni.duration = (height * BACK_TOP_DUR / headerHeight).toLong()
                        backTopAni.start()
                        onRefreshListener?.cancel()
                    }
                }
                return true
            }
            else -> return super.onTouchEvent(event)
        }
    }

    fun finishRefreshing() {
        isRefreshing = false
        header!!.setRefreshing(false)
        finishLoading()
    }

    private fun finishLoading() {
        val height = childView!!.translationY
        val backTopAni = ValueAnimator.ofFloat(height, 0f)
        backTopAni.addUpdateListener { animation ->
            var variable = animation.animatedValue as Float
            variable *= decelerateInterpolator.getInterpolation(variable / headerHeight)
            if (childView != null) {
                childView!!.translationY = variable
            }
            header!!.layoutParams.height = variable.toInt()
            header!!.requestLayout()
        }
        backTopAni.duration = (height * BACK_TOP_DUR / headerHeight).toLong()
        backTopAni.start()
    }

    fun setOnRefreshListener(onRefreshListener: OnRefreshListener) {
        this.onRefreshListener = onRefreshListener
    }

    interface OnRefreshListener {
        fun refresh()
        fun pullDown(pixelsMoved: Float, shouldRefreshOnRelease: Boolean)
        fun cancel()
    }

    companion object {
        private const val BACK_TOP_DUR: Long = 50
        private const val REL_DRAG_DUR: Long = 100
    }
}