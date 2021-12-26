package host.stjin.anonaddy.ui.domains

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.*
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.databinding.ActivityDomainSettingsBinding
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch

class DomainSettingsActivity : BaseActivity(), AddDomainBottomDialogFragment.AddDomainBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private val addDomainFragment: AddDomainBottomDialogFragment = AddDomainBottomDialogFragment.newInstance()

    private lateinit var binding: ActivityDomainSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDomainSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.activityDomainSettingsNSVRL)

        setupToolbar(
            R.string.manage_domains,
            binding.activityDomainSettingsNSV,
            binding.activityDomainSettingsToolbar,
            R.drawable.ic_dns
        )

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        // Set stats right away, update later
        setStats()

        setOnClickListener()
        // Called on OnResume()
        // getDataFromWeb()
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

    private fun getDataFromWeb() {
        binding.activityDomainSettingsLL1.visibility = View.VISIBLE
        binding.activityDomainSettingsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getAllDomainsAndSetView()
            getUserResource()
        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (this.application as AnonAddyForAndroid).userResource = user
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    resources.getString(R.string.error_obtaining_user) + "\n" + result,
                    binding.activityDomainSettingsCL
                ).show()
            }
        }
    }

    private fun setStats(currentCount: Int? = null) {
        binding.activityDomainSettingsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_active_domains,
            currentCount ?: (this.application as AnonAddyForAndroid).userResource.active_domain_count,
            if ((this.application as AnonAddyForAndroid).userResource.subscription != null) (this.application as AnonAddyForAndroid).userResource.active_domain_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        // If userResource.subscription == null, that means that the user has no subscription (thus a self-hosted instance without limits)
        if ((this.application as AnonAddyForAndroid).userResource.subscription != null) {
            binding.activityDomainSettingsAddDomain.isEnabled =
                (this.application as AnonAddyForAndroid).userResource.active_domain_count < (this.application as AnonAddyForAndroid).userResource.active_domain_limit
        } else {
            binding.activityDomainSettingsAddDomain.isEnabled = true
        }
    }

    private lateinit var domainsAdapter: DomainAdapter
    private suspend fun getAllDomainsAndSetView() {
        binding.activityDomainSettingsAllDomainsRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = settingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, 2) ?: 2
                shimmerLayoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(this@DomainSettingsActivity, 2)
                } else {
                    LinearLayoutManager(this@DomainSettingsActivity)
                }

                layoutManager = if (this@DomainSettingsActivity.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(this@DomainSettingsActivity, 2)
                } else {
                    LinearLayoutManager(this@DomainSettingsActivity)
                }
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
            networkHelper?.getAllDomains { list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new domains since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::domainsAdapter.isInitialized && list == domainsAdapter.getList()) {
                    return@getAllDomains
                }

                if (list != null) {
                    // Update stats
                    setStats(list.size)

                    if (list.size > 0) {
                        binding.activityDomainSettingsNoDomains.visibility = View.GONE
                    } else {
                        binding.activityDomainSettingsNoDomains.visibility = View.VISIBLE
                    }

                    // Set the count of aliases so that the shimmerview looks better next time
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, list.size)

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

                } else {

                    SnackbarHelper.createSnackbar(
                        this@DomainSettingsActivity,
                        this.resources.getString(R.string.error_obtaining_domain) + "\n" + error,
                        binding.activityDomainSettingsCL
                    ).show()

                    binding.activityDomainSettingsLL1.visibility = View.GONE
                    binding.activityDomainSettingsRLLottieview.visibility = View.VISIBLE
                }
                hideShimmer()
            }

        }

    }


    private lateinit var deleteDomainSnackbar: Snackbar
    private fun deleteDomain(id: String, context: Context) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.delete_domain))
            .setIcon(R.drawable.ic_trash)
            .setMessage(resources.getString(R.string.delete_domain_desc_confirm))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
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
            .show()
    }

    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        networkHelper?.deleteDomain({ result ->
            if (result == "204") {
                deleteDomainSnackbar.dismiss()
                getDataFromWeb()
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
        addDomainFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }
}