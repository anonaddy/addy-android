package host.stjin.anonaddy.ui.customviews

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import host.stjin.anonaddy.R


class AnimationView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    RelativeLayout(context, attrs, defStyle) {

    private var animationView: ImageView? = null

    fun playAnimation(playOnLoop: Boolean, animationDrawable: Int) {
        val animated = context?.let { AnimatedVectorDrawableCompat.create(it, animationDrawable) }
        if (playOnLoop) {
            animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    animationView?.post { animated.start() }
                }

            })
        }
        animationView?.setImageDrawable(animated)
        animated?.start()
    }

    fun stopAnimation() {
        animationView?.setImageDrawable(null)
    }

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.custom_view_animation, this)
        animationView = findViewById(R.id.custom_view_animation)


        if (attrs != null) {
            // Get attributes
            val a = getContext()
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.AnimationView,
                    0, 0
                )

            if (a.getResourceId(R.styleable.AnimationView_animationDrawable, 0) != 0) {
                playAnimation(
                    a.getBoolean(R.styleable.AnimationView_loopAnimation, false),
                    a.getResourceId(R.styleable.AnimationView_animationDrawable, 0)
                )
            }

            a.recycle()
        }
    }
}