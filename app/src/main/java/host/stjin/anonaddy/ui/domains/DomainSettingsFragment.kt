package host.stjin.anonaddy.ui.domains

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.databinding.FragmentDomainSettingsBinding
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class DomainSettingsFragment : Fragment(), AddDomainBottomDialogFragment.AddDomainBottomDialogListener {

    private var domains: ArrayList<Domains>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private val addDomainFragment: AddDomainBottomDialogFragment = AddDomainBottomDialogFragment.newInstance()


    companion object {
        fun newInstance() = DomainSettingsFragment()
    }

    private var _binding: FragmentDomainSettingsBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDomainSettingsBinding.inflate(inflater, container, false)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        // Set stats right away, update later
        setStats()

        setOnClickListener()
        setDomainsRecyclerView()
        getDataFromWeb(savedInstanceState)

        return root
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(domains)
        outState.putString("domains", json)
    }


    private fun setOnClickListener() {
        binding.fragmentDomainSettingsAddDomain.setOnClickListener {
            if (!addDomainFragment.isAdded) {
                addDomainFragment.show(
                    childFragmentManager,
                    "addDomainFragment"
                )
            }
        }
    }

    fun getDataFromWeb(savedInstanceState: Bundle?) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {

            if (savedInstanceState != null) {
                setStats()

                val domainsJson = savedInstanceState.getString("domains")
                if (!domainsJson.isNullOrEmpty() && domainsJson != "null") {
                    val gson = Gson()

                    val myType = object : TypeToken<ArrayList<Domains>>() {}.type
                    val list = gson.fromJson<ArrayList<Domains>>(domainsJson, myType)
                    setDomainsAdapter(list)
                } else {
                    // domainsJson could be null when an embedded activity is opened instantly
                    getUserResource()
                    getAllDomainsAndSetView()
                }

            } else {
                getUserResource()
                getAllDomainsAndSetView()
            }

        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AddyIoApp).userResource = user
                // Update stats
                setStats()
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        (activity as DomainSettingsActivity).findViewById(R.id.activity_domain_settings_CL),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                }
            }
        }
    }

    private fun setStats() {
        binding.fragmentDomainSettingsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_active_domains,
            (activity?.application as AddyIoApp).userResource.active_domain_count,
            if ((activity?.application as AddyIoApp).userResource.subscription != null) (activity?.application as AddyIoApp).userResource.active_domain_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        // If userResource.subscription == null, that means that the user has no subscription (thus a self-hosted instance without limits)
        if ((activity?.application as AddyIoApp).userResource.subscription != null) {
            binding.fragmentDomainSettingsAddDomain.isEnabled =
                (activity?.application as AddyIoApp).userResource.active_domain_count < (activity?.application as AddyIoApp).userResource.active_domain_limit!! //Cannot be null since subscription is not null
        } else {
            binding.fragmentDomainSettingsAddDomain.isEnabled = true
        }
    }

    private lateinit var domainsAdapter: DomainAdapter
    private suspend fun getAllDomainsAndSetView() {
        binding.fragmentDomainSettingsAllDomainsRecyclerview.apply {
            networkHelper?.getAllDomains { list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new domains since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::domainsAdapter.isInitialized && list == domainsAdapter.getList()) {
                    return@getAllDomains
                }

                if (list != null) {
                    setDomainsAdapter(list)
                } else {

                    if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            this.resources.getString(R.string.error_obtaining_domain) + "\n" + error,
                            (activity as MainActivity).findViewById(R.id.main_container),
                            LoggingHelper.LOGFILES.DEFAULT
                        ).show()
                    } else {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            this.resources.getString(R.string.error_obtaining_domain) + "\n" + error,
                            (activity as DomainSettingsActivity).findViewById(R.id.activity_domain_settings_CL),
                            LoggingHelper.LOGFILES.DEFAULT
                        ).show()
                    }


                    // Show error animations
                    binding.fragmentDomainSettingsLL1.visibility = View.GONE
                    //binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                }
                hideShimmer()
            }

        }

    }

    private fun setDomainsAdapter(list: java.util.ArrayList<Domains>) {
        binding.fragmentDomainSettingsAllDomainsRecyclerview.apply {
            domains = list
            if (list.size > 0) {
                binding.fragmentDomainSettingsNoDomains.visibility = View.GONE
            } else {
                binding.fragmentDomainSettingsNoDomains.visibility = View.VISIBLE
            }

            // Set the count of aliases so that the shimmerview looks better next time
            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, list.size)

            domainsAdapter = DomainAdapter(list)
            domainsAdapter.setClickListener(object : DomainAdapter.ClickListener {

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, ManageDomainsActivity::class.java)
                    intent.putExtra("domain_id", list[pos].id)
                    resultLauncher.launch(intent)
                }


                override fun onClickDelete(pos: Int, aView: View) {
                    deleteDomain(list[pos].id, context)
                }

            })
            adapter = domainsAdapter

            //binding.animationFragment.stopAnimation()
            //binding.fragmentDomainSettingsNSV.animate().alpha(1.0f) -> Do not animate as there is a shimmerview
        }
    }

    private fun setDomainsRecyclerView() {
        binding.fragmentDomainSettingsAllDomainsRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, 2) ?: 2
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


    private lateinit var deleteDomainSnackbar: Snackbar
    private fun deleteDomain(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.delete_domain),
            message = resources.getString(R.string.delete_domain_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {


                deleteDomainSnackbar = if (context.resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        this.resources.getString(R.string.deleting_domain),
                        (activity as MainActivity).findViewById(R.id.main_container),
                        length = Snackbar.LENGTH_INDEFINITE
                    )
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        this.resources.getString(R.string.deleting_domain),
                        (activity as DomainSettingsActivity).findViewById(R.id.activity_domain_settings_CL),
                        length = Snackbar.LENGTH_INDEFINITE
                    )
                }


                deleteDomainSnackbar.show()
                lifecycleScope.launch {
                    deleteDomainHttpRequest(id, context)
                }
            }
        ).show()
    }

    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        networkHelper?.deleteDomain({ result ->
            if (result == "204") {
                deleteDomainSnackbar.dismiss()
                getDataFromWeb(null)
            } else {

                if (context.resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_domain), result
                        ),
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_domain), result
                        ),
                        (activity as DomainSettingsActivity).findViewById(R.id.activity_domain_settings_CL),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                }
            }
        }, id)
    }

    override fun onAdded() {
        addDomainFragment.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(null)
    }

}