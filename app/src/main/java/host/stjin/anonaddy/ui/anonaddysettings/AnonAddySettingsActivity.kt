package host.stjin.anonaddy.ui.anonaddysettings

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.*
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import kotlinx.android.synthetic.main.activity_anonaddy_settings.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AnonAddySettingsActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anonaddy_settings)
        networkHelper = NetworkHelper(this)

        setupToolbar(anonaddysettings_toolbar)
        setOnClickListeners()

        getStatistics()
        // Called on OnResume()
        // getDataFromWeb()
    }


    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        getDataFromWeb()
    }


    private fun setOnClickListeners() {
        /*
        ANONADDY SETTINGS CANNOT BE SET BY API. Always open settings
         */
        anonaddy_settings_monthly_bandwidth_more_info_button.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        anonaddy_settings_statistics_recipients_more_info_button.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        anonaddy_settings_statistics_aliases_more_info_button.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        anonaddy_settings_update_default_recipient_LL.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        anonaddy_settings_update_default_alias_domain_LL.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        anonaddy_settings_update_default_alias_format_LL.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }


    private fun getDataFromWeb() {
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getWebStatistics()
        }
    }

    private suspend fun getWebStatistics() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                User.userResource = user
                getStatistics()
            } else {
                val snackbar =
                    Snackbar.make(
                        activity_app_settings_LL, resources.getString(R.string.error_obtaining_user) + "\n" + result,
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

    private fun getStatistics() {
        //  / 1024 / 1024 because api returns bytes
        val currMonthlyBandwidth = User.userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = User.userResource.bandwidth_limit / 1024 / 1024

        setMonthlyBandwidthStatistics(currMonthlyBandwidth, maxMonthlyBandwidth)
        setAliasesStatistics(User.userResource.active_shared_domain_alias_count, User.userResource.active_shared_domain_alias_limit)
        setRecipientStatistics(User.userResource.recipient_count, User.userResource.recipient_limit)
    }

    private fun setAliasesStatistics(count: Int, maxAliases: Int) {
        anonaddy_settings_statistics_aliases_progress.max = maxAliases * 100
        anonaddy_settings_statistics_aliases_current.text = count.toString()

        val maxAliasesString = if (maxAliases == 0) "∞" else maxAliases.toString()
        anonaddy_settings_statistics_aliases_max.text = maxAliasesString

        val maxAliasesDescString = if (maxAliases == 0) resources.getString(R.string.unlimited) else maxAliases.toString()
        anonaddy_settings_statistics_aliases_desc.text =
            resources.getString(R.string.anonaddy_settings_statistics_aliases_desc, count, maxAliasesDescString)

        Handler().postDelayed({
            ObjectAnimator.ofInt(
                anonaddy_settings_statistics_aliases_progress,
                "progress",
                count * 100
            )
                .setDuration(300)
                .start()
        }, 400)
    }

    private fun setMonthlyBandwidthStatistics(
        currMonthlyBandwidth: Double,
        maxMonthlyBandwidth: Int
    ) {
        anonaddy_settings_statistics_monthly_bandwidth_progress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100

        val currMonthlyBandwidthString = "${roundOffDecimal(currMonthlyBandwidth)}MB"
        anonaddy_settings_statistics_monthly_bandwidth_current.text = currMonthlyBandwidthString

        val maxMonthlyBandwidthString = "${if (maxMonthlyBandwidth == 0) "∞" else maxMonthlyBandwidth.toString()}MB"
        val maxMonthlyBandwidthDescString = if (maxMonthlyBandwidth == 0) resources.getString(R.string.unlimited) else "${maxMonthlyBandwidth}MB"

        anonaddy_settings_statistics_monthly_bandwidth_max.text = maxMonthlyBandwidthString


        val cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)

        val nextMonthString = SimpleDateFormat("MMMM").format(cal.time)

        anonaddy_settings_statistics_monthly_bandwidth_desc.text =
            resources.getString(
                R.string.anonaddy_settings_monthly_bandwidth_desc,
                currMonthlyBandwidthString,
                maxMonthlyBandwidthDescString,
                nextMonthString
            )


        ObjectAnimator.ofInt(
            anonaddy_settings_statistics_monthly_bandwidth_progress,
            "progress",
            currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(300)
            .start()
    }

    private fun setRecipientStatistics(currRecipients: Int, maxRecipient: Int) {
        anonaddy_settings_statistics_statistics_recipients_progress.max =
            maxRecipient * 100
        anonaddy_settings_statistics_recipients_current.text = currRecipients.toString()

        val maxRecipientString = if (maxRecipient == 0) "∞" else maxRecipient.toString()
        val maxRecipientDescString = if (maxRecipient == 0) resources.getString(R.string.unlimited) else maxRecipient.toString()
        anonaddy_settings_statistics_recipients_max.text = maxRecipientString

        anonaddy_settings_statistics_recipients_desc.text =
            resources.getString(R.string.anonaddy_settings_statistics_recipients_desc, currRecipients, maxRecipientDescString)

        ObjectAnimator.ofInt(
            anonaddy_settings_statistics_statistics_recipients_progress,
            "progress",
            currRecipients * 100
        )
            .setDuration(300)
            .start()
    }


}