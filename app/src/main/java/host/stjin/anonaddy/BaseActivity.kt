package host.stjin.anonaddy

import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseActivity : AppCompatActivity() {

    fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding) -> Unit) {
        // Create a snapshot of the view's padding state
        val initialPadding = recordInitialPaddingForView(this)
        // Set an actual OnApplyWindowInsetsListener which proxies to the given
        // lambda, also passing in the original padding state
        setOnApplyWindowInsetsListener { v, insets ->
            f(v, insets, initialPadding)
            // Always return the insets, so that children can also use them
            insets
        }
        // request some insets
        //requestApplyInsetsWhenAttached()
    }

    data class InitialPadding(
        val left: Int, val top: Int,
        val right: Int, val bottom: Int
    )

    private fun recordInitialPaddingForView(view: View) = InitialPadding(
        view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
    )

    fun setupToolbar(toolbar: MaterialToolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24) // need to set the icon here to have a navigation icon. You can simple create an vector image by "Vector Asset" and using here
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}
