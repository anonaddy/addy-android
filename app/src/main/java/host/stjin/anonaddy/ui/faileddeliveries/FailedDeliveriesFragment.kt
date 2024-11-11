package host.stjin.anonaddy.ui.faileddeliveries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.FailedDeliveryAdapter
import host.stjin.anonaddy.databinding.FragmentFailedDeliveriesBinding
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class FailedDeliveriesFragment : Fragment(), FailedDeliveryDetailsBottomDialogFragment.AddFailedDeliveryBottomDialogListener {

    private var failedDeliveries: ArrayList<FailedDeliveries>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private var failedDeliveryDetailsBottomDialogFragment: FailedDeliveryDetailsBottomDialogFragment? = null


    companion object {
        fun newInstance() = FailedDeliveriesFragment()
    }


    private var _binding: FragmentFailedDeliveriesBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFailedDeliveriesBinding.inflate(inflater, container, false)
        InsetUtil.applyBottomInset(binding.fragmentFailedDeliveriesLL1)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        setFailedDeliveriesRecyclerView()
        getDataFromWeb(savedInstanceState)


        return root
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(failedDeliveries)
        outState.putString("failedDeliveries", json)
    }


    fun getDataFromWeb(savedInstanceState: Bundle?, callback: () -> Unit? = {}) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            if (savedInstanceState != null) {

                val failedDeliveriesJson = savedInstanceState.getString("failedDeliveries")
                if (failedDeliveriesJson!!.isNotEmpty() && failedDeliveriesJson != "null") {
                    val gson = Gson()

                    val myType = object : TypeToken<ArrayList<FailedDeliveries>>() {}.type
                    val list = gson.fromJson<ArrayList<FailedDeliveries>>(failedDeliveriesJson, myType)
                    setFailedDeliveriesAdapter(list)
                } else {
                    // failedDeliveriesJson could be null when an embedded activity is opened instantly
                    getAllFailedDeliveriesAndSetRecyclerview()
                }

            } else {
                getAllFailedDeliveriesAndSetRecyclerview()
            }
            callback()
        }
    }


    private fun setFailedDeliveriesRecyclerView() {
        binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount =
                    encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
        }
    }

    private lateinit var failedDeliveriesAdapter: FailedDeliveryAdapter
    private suspend fun getAllFailedDeliveriesAndSetRecyclerview() {
        binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview.apply {
            networkHelper?.getAllFailedDeliveries { list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new domains since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::failedDeliveriesAdapter.isInitialized && list == failedDeliveriesAdapter.getList()) {
                    return@getAllFailedDeliveries
                }

                if (list != null) {
                    setFailedDeliveriesAdapter(list)
                } else {
                    // If the error is 404, the feature is unavailable, let the user know that the feature is not available
                    if (error == "404") {
                        binding.fragmentFailedDeliveriesLL1.visibility = View.GONE
                        binding.root.findViewById<View>(R.id.fragment_content_unavailable).visibility = View.VISIBLE
                    } else {
                        if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                            SnackbarHelper.createSnackbar(
                                requireContext(),
                                requireContext().resources.getString(R.string.error_obtaining_failed_deliveries) + "\n" + error,
                                (activity as MainActivity).findViewById(R.id.main_container),
                                LoggingHelper.LOGFILES.DEFAULT
                            ).show()
                        } else {
                            SnackbarHelper.createSnackbar(
                                requireContext(),
                                requireContext().resources.getString(R.string.error_obtaining_failed_deliveries) + "\n" + error,
                                (activity as FailedDeliveriesActivity).findViewById(R.id.activity_failed_deliveries_settings_CL),
                                LoggingHelper.LOGFILES.DEFAULT
                            ).show()
                        }

                        // Show error animations
                        binding.fragmentFailedDeliveriesLL1.visibility = View.GONE
                        binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                    }


                }
                hideShimmer()
            }

        }

    }

    fun fragmentShown() {
        if (::failedDeliveriesAdapter.isInitialized) {
            // Set the count of failed deliveries so that the shimmerview looks better next time AND so that we can use it for the backgroundservice AND mark this a read for the badge
            encryptedSettingsManager?.putSettingsInt(
                SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT,
                failedDeliveriesAdapter.itemCount
            )
        }
    }


    private fun setFailedDeliveriesAdapter(list: ArrayList<FailedDeliveries>) {
        binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview.apply {
            failedDeliveries = list
            if (list.size > 0) {
                binding.fragmentFailedDeliveriesNoFailedDeliveries.visibility = View.GONE
            } else {
                binding.fragmentFailedDeliveriesNoFailedDeliveries.visibility = View.VISIBLE
            }


            failedDeliveriesAdapter = FailedDeliveryAdapter(list)
            failedDeliveriesAdapter.setClickListener(object : FailedDeliveryAdapter.ClickListener {

                override fun onClickDetails(pos: Int, aView: View) {
                    failedDeliveryDetailsBottomDialogFragment = FailedDeliveryDetailsBottomDialogFragment(
                        list[pos].id,
                        list[pos].created_at,
                        list[pos].attempted_at,
                        list[pos].alias_email,
                        list[pos].recipient_email,
                        list[pos].bounce_type,
                        list[pos].remote_mta,
                        list[pos].sender,
                        list[pos].code
                    )
                    failedDeliveryDetailsBottomDialogFragment!!.show(
                        childFragmentManager,
                        "failedDeliveryDetailsBottomDialogFragment"
                    )
                }

            })
            adapter = failedDeliveriesAdapter


            // When in tablet mode (aka split screen mode) loading this fragment should not automatically update the value, it should only be updated
            // upon showing (so that the value keeps notifying the user until the user clicks on it.
            // When in phone mode the activity is in foreground and should update the value automatically.
            if (!this.resources.getBoolean(R.bool.isTablet)) {
                fragmentShown()
            }

            binding.animationFragment.stopAnimation()
            //binding.activityFailedDeliveriesNSV.animate().alpha(1.0f) -> Do not animate as there is a shimmerview
        }
    }


    override fun onDeleted(failedDeliveryId: String) {
        failedDeliveryDetailsBottomDialogFragment?.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(null)
    }
}