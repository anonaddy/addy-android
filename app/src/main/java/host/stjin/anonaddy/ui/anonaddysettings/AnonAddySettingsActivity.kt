package host.stjin.anonaddy.ui.anonaddysettings

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityAnonaddySettingsBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AnonAddySettingsActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    private lateinit var binding: ActivityAnonaddySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnonaddySettingsBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)
        networkHelper = NetworkHelper(this)

        setupToolbar(binding.anonaddysettingsToolbar)
        setOnClickListeners()

        getStatistics()
        // Called on OnResume()
        // getDataFromWeb()
    }


    // If the user comes back from eg. settings re-check for updated data
    override fun onResume() {
        super.onResume()
        getDataFromWeb()
    }


    private fun setOnClickListeners() {
        /*
        ANONADDY SETTINGS CANNOT BE SET BY API. Always open settings
         */
        binding.anonaddySettingsMonthlyBandwidthMoreInfoButton.setOnClickListener {
            openSettings()
        }
        binding.anonaddySettingsStatisticsRecipientsMoreInfoButton.setOnClickListener {
            openSettings()
        }
        binding.anonaddySettingsStatisticsAliasesMoreInfoButton.setOnClickListener {
            openSettings()
        }


        binding.anonaddySettingsUpdateDefaultRecipientLL.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                openSettings()
            }
        })


        binding.anonaddySettingsUpdateDefaultAliasDomainLL.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                openSettings()
            }
        })

        binding.anonaddySettingsUpdateDefaultAliasFormatLL.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                openSettings()
            }
        })
    }

    private fun openSettings() {
        val url = "${AnonAddy.API_BASE_URL}/settings"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
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
                        binding.activityAppSettingsLL, resources.getString(R.string.error_obtaining_user) + "\n" + result,
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
        binding.anonaddySettingsStatisticsAliasesProgress.max = maxAliases * 100
        binding.anonaddySettingsStatisticsAliasesCurrent.text = count.toString()

        val maxAliasesString = if (maxAliases == 0) "∞" else maxAliases.toString()
        binding.anonaddySettingsStatisticsAliasesMax.text = maxAliasesString

        val maxAliasesDescString = if (maxAliases == 0) resources.getString(R.string.unlimited) else maxAliases.toString()
        binding.anonaddySettingsStatisticsAliasesDesc.text =
            resources.getString(R.string.anonaddy_settings_statistics_aliases_desc, count, maxAliasesDescString)

        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofInt(
                binding.anonaddySettingsStatisticsAliasesProgress,
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
        binding.anonaddySettingsStatisticsMonthlyBandwidthProgress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100

        val currMonthlyBandwidthString = "${roundOffDecimal(currMonthlyBandwidth)}MB"
        binding.anonaddySettingsStatisticsMonthlyBandwidthCurrent.text = currMonthlyBandwidthString

        val maxMonthlyBandwidthString = "${if (maxMonthlyBandwidth == 0) "∞" else maxMonthlyBandwidth.toString()}MB"
        val maxMonthlyBandwidthDescString = if (maxMonthlyBandwidth == 0) resources.getString(R.string.unlimited) else "${maxMonthlyBandwidth}MB"

        binding.anonaddySettingsStatisticsMonthlyBandwidthMax.text = maxMonthlyBandwidthString


        val cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)

        val nextMonthString = SimpleDateFormat("MMMM").format(cal.time)

        binding.anonaddySettingsStatisticsMonthlyBandwidthDesc.text =
            resources.getString(
                R.string.anonaddy_settings_monthly_bandwidth_desc,
                currMonthlyBandwidthString,
                maxMonthlyBandwidthDescString,
                nextMonthString
            )


        ObjectAnimator.ofInt(
            binding.anonaddySettingsStatisticsMonthlyBandwidthProgress,
            "progress",
            currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(300)
            .start()
    }

    private fun setRecipientStatistics(currRecipients: Int, maxRecipient: Int) {
        binding.anonaddySettingsStatisticsStatisticsRecipientsProgress.max =
            maxRecipient * 100
        binding.anonaddySettingsStatisticsRecipientsCurrent.text = currRecipients.toString()

        val maxRecipientString = if (maxRecipient == 0) "∞" else maxRecipient.toString()
        val maxRecipientDescString = if (maxRecipient == 0) resources.getString(R.string.unlimited) else maxRecipient.toString()
        binding.anonaddySettingsStatisticsRecipientsMax.text = maxRecipientString

        binding.anonaddySettingsStatisticsRecipientsDesc.text =
            resources.getString(R.string.anonaddy_settings_statistics_recipients_desc, currRecipients, maxRecipientDescString)

        ObjectAnimator.ofInt(
            binding.anonaddySettingsStatisticsStatisticsRecipientsProgress,
            "progress",
            currRecipients * 100
        )
            .setDuration(300)
            .start()
    }


}