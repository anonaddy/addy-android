package host.stjin.anonaddy.ui.faileddeliveries

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.FailedDeliveryAdapter
import host.stjin.anonaddy.databinding.ActivityFailedDeliveriesBinding
import host.stjin.anonaddy.utils.MarginItemDecoration
import kotlinx.coroutines.launch

class FailedDeliveriesActivity : BaseActivity(), FailedDeliveryDetailsBottomDialogFragment.AddFailedDeliveryBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private var failedDeliveryDetailsBottomDialogFragment: FailedDeliveryDetailsBottomDialogFragment? = null

    private lateinit var binding: ActivityFailedDeliveriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFailedDeliveriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(binding.activityFailedDeliveriesToolbar.customToolbarOneHandedMaterialtoolbar, R.string.failed_deliveries)

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        setPage()
    }

    private fun setPage() {
        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    getDataFromWeb()
                }
            }
        }

    }

    private fun getDataFromWeb() {
        binding.activityFailedDeliveriesLL1.visibility = View.VISIBLE
        binding.activityFailedDeliveriesRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getAllFailedDeliveriesAndSetRecyclerview()
        }
    }


    private lateinit var failedDeliveriesAdapter: FailedDeliveryAdapter
    private suspend fun getAllFailedDeliveriesAndSetRecyclerview() {
        binding.activityFailedDeliveriesAllFailedDeliveriesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = settingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, 2) ?: 2
                shimmerLayoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(this@FailedDeliveriesActivity, 2)
                } else {
                    LinearLayoutManager(this@FailedDeliveriesActivity)
                }

                layoutManager = if (this@FailedDeliveriesActivity.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(this@FailedDeliveriesActivity, 2)
                } else {
                    LinearLayoutManager(this@FailedDeliveriesActivity)
                }
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
            networkHelper?.getAllFailedDeliveries({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new domains since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::failedDeliveriesAdapter.isInitialized && list == failedDeliveriesAdapter.getList()) {
                    return@getAllFailedDeliveries
                }

                if (list != null) {

                    if (list.size > 0) {
                        binding.activityFailedDeliveriesNoFailedDeliveries.visibility = View.GONE
                    } else {
                        binding.activityFailedDeliveriesNoFailedDeliveries.visibility = View.VISIBLE
                    }

                    // Set the count of failed deliveries so that the shimmerview looks better next time AND so that we can use it for the backgroundservice
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, list.size)

                    failedDeliveriesAdapter = FailedDeliveryAdapter(list)
                    failedDeliveriesAdapter.setClickListener(object : FailedDeliveryAdapter.ClickListener {

                        override fun onClickDetails(pos: Int, aView: View) {
                            failedDeliveryDetailsBottomDialogFragment = FailedDeliveryDetailsBottomDialogFragment(
                                list[pos].id,
                                list[pos].created_at,
                                list[pos].alias_email,
                                list[pos].recipient_email,
                                list[pos].bounce_type,
                                list[pos].remote_mta,
                                list[pos].sender,
                                list[pos].code
                            )
                            failedDeliveryDetailsBottomDialogFragment!!.show(
                                supportFragmentManager,
                                "failedDeliveryDetailsBottomDialogFragment"
                            )
                        }

                    })
                    adapter = failedDeliveriesAdapter

                } else {
                    binding.activityFailedDeliveriesLL1.visibility = View.GONE
                    binding.activityFailedDeliveriesRLLottieview.visibility = View.VISIBLE
                }
                hideShimmer()
            }, show404Toast = true)

        }

    }


    override fun onDeleted(failedDeliveryId: String) {
        failedDeliveryDetailsBottomDialogFragment?.dismiss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }
}