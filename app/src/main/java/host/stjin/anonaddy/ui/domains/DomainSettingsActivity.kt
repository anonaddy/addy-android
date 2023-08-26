package host.stjin.anonaddy.ui.domains

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.databinding.ActivityDomainSettingsBinding
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

class DomainSettingsActivity : BaseActivity(), AddDomainBottomDialogFragment.AddDomainBottomDialogListener {

    private var domains: ArrayList<Domains>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private val addDomainFragment: AddDomainBottomDialogFragment = AddDomainBottomDialogFragment.newInstance()

    private lateinit var binding: ActivityDomainSettingsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDomainSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityDomainSettingsLL1)
        )

        setupToolbar(
            R.string.manage_domains,
            binding.activityDomainSettingsNSV,
            binding.activityDomainSettingsToolbar,
            R.drawable.ic_dns
        )


        encryptedSettingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        // Set stats right away, update later
        setStats()

        setOnClickListener()
        setDomainsRecyclerView()
        getDataFromWeb(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(domains)
        outState.putString("domains", json)
    }


    private fun setOnClickListener() {
        binding.activityDomainSettingsAddDomain.setOnClickListener {
            if (!addDomainFragment.isAdded) {
                addDomainFragment.show(
                    supportFragmentManager,
                    "addDomainFragment"
                )
            }
        }
    }

    private fun getDataFromWeb(savedInstanceState: Bundle?) {
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
                (this.application as AddyIoApp).userResource = user
                // Update stats
                setStats()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    resources.getString(R.string.error_obtaining_user) + "\n" + result,
                    binding.activityDomainSettingsCL
                ).show()
            }
        }
    }

    private fun setStats() {
        binding.activityDomainSettingsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_active_domains,
            (this.application as AddyIoApp).userResource.active_domain_count,
            if ((this.application as AddyIoApp).userResource.subscription != null) (this.application as AddyIoApp).userResource.active_domain_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        // If userResource.subscription == null, that means that the user has no subscription (thus a self-hosted instance without limits)
        if ((this.application as AddyIoApp).userResource.subscription != null) {
            binding.activityDomainSettingsAddDomain.isEnabled =
                (this.application as AddyIoApp).userResource.active_domain_count < (this.application as AddyIoApp).userResource.active_domain_limit
        } else {
            binding.activityDomainSettingsAddDomain.isEnabled = true
        }
    }

    private lateinit var domainsAdapter: DomainAdapter
    private suspend fun getAllDomainsAndSetView() {
        binding.activityDomainSettingsAllDomainsRecyclerview.apply {
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

                    SnackbarHelper.createSnackbar(
                        this@DomainSettingsActivity,
                        this.resources.getString(R.string.error_obtaining_domain) + "\n" + error,
                        binding.activityDomainSettingsCL
                    ).show()

                    // Show error animations
                    binding.activityDomainSettingsLL1.visibility = View.GONE
                    binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                }
                hideShimmer()
            }

        }

    }

    private fun setDomainsAdapter(list: java.util.ArrayList<Domains>) {
        binding.activityDomainSettingsAllDomainsRecyclerview.apply {
            domains = list
            if (list.size > 0) {
                binding.activityDomainSettingsNoDomains.visibility = View.GONE
            } else {
                binding.activityDomainSettingsNoDomains.visibility = View.VISIBLE
            }

            // Set the count of aliases so that the shimmerview looks better next time
            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, list.size)

            domainsAdapter = DomainAdapter(list)
            domainsAdapter.setClickListener(object : DomainAdapter.ClickListener {

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, ManageDomainsActivity::class.java)
                    intent.putExtra("domain_id", list[pos].id)
                    startActivity(intent)
                }


                override fun onClickDelete(pos: Int, aView: View) {
                    deleteDomain(list[pos].id, context)
                }

            })
            adapter = domainsAdapter

            binding.animationFragment.stopAnimation()
            //binding.activityDomainSettingsNSV.animate().alpha(1.0f) -> Do not animate as there is a shimmerview
        }
    }

    private fun setDomainsRecyclerView() {
        binding.activityDomainSettingsAllDomainsRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(this@DomainSettingsActivity, ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(this@DomainSettingsActivity, ScreenSizeUtils.calculateNoOfColumns(context))

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
            context = this,
            title = resources.getString(R.string.delete_domain),
            message = resources.getString(R.string.delete_domain_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteDomainSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_domain),
                    binding.activityDomainSettingsCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
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

                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_domain), result
                    ),
                    binding.activityDomainSettingsCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    override fun onAdded() {
        addDomainFragment.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(null)
    }

}