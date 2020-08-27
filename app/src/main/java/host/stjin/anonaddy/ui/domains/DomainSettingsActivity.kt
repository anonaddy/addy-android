package host.stjin.anonaddy.ui.domains

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import kotlinx.android.synthetic.main.activity_domain_settings.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DomainSettingsActivity : BaseActivity(), AddDomainBottomDialogFragment.AddDomainBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true

    private val addDomainFragment: AddDomainBottomDialogFragment = AddDomainBottomDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_domain_settings)
        setupToolbar(activity_domain_settings_toolbar)

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        setOnClickListener()
        getDataFromWeb()
    }

    private fun setOnClickListener() {
        activity_domain_settings_add_domain.setOnClickListener {
            if (!addDomainFragment.isAdded) {
                addDomainFragment.show(
                    supportFragmentManager,
                    "addDomainFragment"
                )
            }
        }
    }

    private fun getDataFromWeb() {
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllDomains()
        }
    }

    private suspend fun getAllDomains() {
        activity_domain_settings_all_domains_recyclerview.apply {

            if (itemDecorationCount > 0) {
                addItemDecoration(
                    DividerItemDecoration(
                        this.context,
                        (layoutManager as LinearLayoutManager).orientation
                    )
                )
            }
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(context)
            // set the custom adapter to the RecyclerView

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                activity_domain_settings_all_domains_recyclerview.layoutAnimation = animation
            }


            networkHelper?.getAllDomains { list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                if (list != null) {

                    if (list.size > 0) {
                        activity_domain_settings_no_domains.visibility = View.GONE
                    } else {
                        activity_domain_settings_no_domains.visibility = View.VISIBLE
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
                    activity_domain_settings_all_domains_recyclerview.hideShimmerAdapter()
                } else {
                    activity_domain_settings_LL1.visibility = View.GONE
                    activity_domain_settings_RL_lottieview.visibility = View.VISIBLE
                }
            }

        }

    }


    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun deleteDomain(id: String, context: Context) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = context.resources.getString(R.string.delete_domain)
        customLayout.dialog_text.text = context.resources.getString(R.string.delete_domain_desc_confirm)
        customLayout.dialog_positive_button.text =
            context.resources.getString(R.string.delete_domain)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteDomainHttpRequest(id, context)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        networkHelper?.deleteDomain(id) { result ->
            if (result == "204") {
                dialog.dismiss()
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                    getAllDomains()
                }
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text =
                    context.resources.getString(R.string.error_deleting_domain) + "\n" + result
            }
        }
    }

    override fun onAdded() {
        addDomainFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllDomains()
        }
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllDomains()
        }
    }
}