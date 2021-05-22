package host.stjin.anonaddy.ui.domains

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.databinding.ActivityDomainSettingsBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DomainSettingsActivity : BaseActivity(), AddDomainBottomDialogFragment.AddDomainBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true

    private val addDomainFragment: AddDomainBottomDialogFragment = AddDomainBottomDialogFragment.newInstance()

    private lateinit var binding: ActivityDomainSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDomainSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(binding.activityDomainSettingsToolbar.customToolbarOneHandedMaterialtoolbar, R.string.manage_domains)

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
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllDomains()
            getUserResource()
        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                User.userResource = user
                setStats()
            } else {
                val snackbar =
                    Snackbar.make(
                        binding.activityDomainSettingsCL,
                        resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        Snackbar.LENGTH_SHORT
                    )

                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }
    }

    private fun setStats() {
        binding.activityDomainSettingsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_active_domains,
            User.userResource.active_domain_count,
            User.userResource.active_domain_limit
        )

        binding.activityDomainSettingsAddDomain.isEnabled = User.userResource.active_domain_count < User.userResource.active_domain_limit

    }


    private suspend fun getAllDomains() {
        binding.activityDomainSettingsAllDomainsRecyclerview.apply {

            layoutManager = if (context.resources.getBoolean(R.bool.isTablet)){
                // set a GridLayoutManager for tablets
                GridLayoutManager(this@DomainSettingsActivity, 2)
            } else {
                LinearLayoutManager(this@DomainSettingsActivity)
            }

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                binding.activityDomainSettingsAllDomainsRecyclerview.layoutAnimation = animation
            }


            networkHelper?.getAllDomains { list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                if (list != null) {

                    if (list.size > 0) {
                        binding.activityDomainSettingsNoDomains.visibility = View.GONE
                    } else {
                        binding.activityDomainSettingsNoDomains.visibility = View.VISIBLE
                    }

                    val domainsAdapter = DomainAdapter(list)
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
                    binding.activityDomainSettingsAllDomainsRecyclerview.hideShimmerAdapter()
                } else {
                    binding.activityDomainSettingsLL1.visibility = View.GONE
                    binding.activityDomainSettingsRLLottieview.visibility = View.VISIBLE
                }
            }

        }

    }


    lateinit var dialog: AlertDialog
    private fun deleteDomain(id: String, context: Context) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)
// create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = context.resources.getString(R.string.delete_domain)
        anonaddyCustomDialogBinding.dialogText.text = context.resources.getString(R.string.delete_domain_desc_confirm)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            context.resources.getString(R.string.delete_domain)
        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            anonaddyCustomDialogBinding.dialogProgressbar.visibility = View.VISIBLE
            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteDomainHttpRequest(id, context, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteDomainHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper?.deleteDomain({ result ->
            if (result == "204") {
                dialog.dismiss()
                getDataFromWeb()
            } else {
                anonaddyCustomDialogBinding.dialogProgressbar.visibility = View.INVISIBLE
                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_domain), result
                )
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