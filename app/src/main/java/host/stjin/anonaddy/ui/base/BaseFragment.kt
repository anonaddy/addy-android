package host.stjin.anonaddy.ui.base

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.utils.LoggingHelper
import com.todkars.shimmer.ShimmerRecyclerView

abstract class BaseFragment : Fragment() {

    val isTablet: Boolean
        get() = resources.getBoolean(R.bool.isTablet)

    /**
     * Common method to handle UiState transitions in fragments.
     * Centralizing this ensures a consistent UX across all screens.
     */
    fun <T> handleUiState(
        state: UiState<T>,
        shimmer: ShimmerRecyclerView? = null,
        progress: View? = null,
        titleProgress: View? = null,
        errorStringRes: Int = R.string.could_not_refresh_data,
        unavailableView: View? = null,
        contentView: View? = null,
        onSuccess: (T) -> Unit
    ) {
        when (state) {
            is UiState.Loading -> {
                shimmer?.showShimmer()
                progress?.visibility = View.VISIBLE
                titleProgress?.visibility = View.VISIBLE
                unavailableView?.visibility = View.GONE
                contentView?.visibility = View.VISIBLE
            }
            is UiState.Success -> {
                shimmer?.hideShimmer()
                progress?.visibility = View.GONE
                titleProgress?.visibility = View.GONE
                unavailableView?.visibility = View.GONE
                contentView?.visibility = View.VISIBLE
                onSuccess(state.data)
            }
            is UiState.Error -> {
                shimmer?.hideShimmer()
                progress?.visibility = View.GONE
                titleProgress?.visibility = View.GONE
                if (state.statusCode == 404 && unavailableView != null) {
                    unavailableView.visibility = View.VISIBLE
                    contentView?.visibility = View.GONE
                } else {
                    unavailableView?.visibility = View.GONE
                    contentView?.visibility = View.VISIBLE
                    showError(state.message, errorStringRes)
                }
            }
        }
    }

    fun getSnackbarContainer(): View {
        return if (isTablet) {
            activity?.findViewById(R.id.main_container) ?: requireView()
        } else {
            activity?.findViewById(R.id.nav_view) ?: activity?.findViewById(android.R.id.content) ?: requireView()
        }
    }

    /**
     * Shows a snackbar error message with proper anchoring for mobile and tablet.
     */
    fun showError(errorMsg: String?, defaultMessageResId: Int, anchorView: View? = null, showLog: LoggingHelper.LOGFILES? = LoggingHelper.LOGFILES.DEFAULT) {
        if (!isAdded) return
        val defaultMessage = getString(defaultMessageResId)
        val message = if (!errorMsg.isNullOrEmpty()) {
            "$defaultMessage\n$errorMsg"
        } else {
            defaultMessage
        }

        val targetAnchorView = anchorView ?: if (isTablet) {
            (activity as? MainActivity)?.findViewById(R.id.main_container)
        } else {
            activity?.findViewById<BottomNavigationView>(R.id.nav_view)
        } ?: view

        targetAnchorView?.let {
            val snackbar = SnackbarHelper.createSnackbar(
                requireContext(),
                message,
                it,
                showLog
            )
            if (!isTablet && it is BottomNavigationView) {
                snackbar.anchorView = it
            }
            snackbar.show()
        }
    }

    /**
     * Sets up a scroll listener on the NestedScrollView to update the MainActivity's top-of-scroll state.
     */
    fun setupNsvScrollListener(nsv: NestedScrollView) {
        nsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            updateHasReachedTopOfNsv(nsv)
        })
    }

    fun updateHasReachedTopOfNsv(nsv: NestedScrollView) {
        (activity as? MainActivity)?.hasReachedTopOfNsv = !nsv.canScrollVertically(-1)
    }
}
