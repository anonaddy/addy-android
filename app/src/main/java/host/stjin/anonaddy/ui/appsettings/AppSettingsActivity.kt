package host.stjin.anonaddy.ui.appsettings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import kotlinx.android.synthetic.main.activity_app_settings.*

class AppSettingsActivity : BaseActivity(),
    DarkModeBottomDialogFragment.AddDarkmodeBottomDialogListener {

    private val addDarkModeBottomDialogFragment: DarkModeBottomDialogFragment =
        DarkModeBottomDialogFragment.newInstance()

    private val addChangelogBottomDialogFragment: ChangelogBottomDialogFragment =
        ChangelogBottomDialogFragment.newInstance()


    lateinit var settingsManager: SettingsManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        settingsManager = SettingsManager(false, applicationContext)
        setupToolbar(appsettings_toolbar)
        setVersion()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        activity_app_settings_section_app_theme.setOnClickListener {
            addDarkModeBottomDialogFragment.show(
                supportFragmentManager,
                "addDarkModeBottomDialogFragment"
            )
        }
        activity_app_settings_section_changelog.setOnClickListener {
            addChangelogBottomDialogFragment.show(
                supportFragmentManager,
                "addChangelogBottomDialogFragment"
            )
        }
        activity_app_settings_section_faq.setOnClickListener {
            val url = "https://anonaddy.com/faq/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        activity_app_settings_section_help.setOnClickListener {
            val url = "https://anonaddy.com/help/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        activity_app_settings_section_pricing.setOnClickListener {
            val url = "https://anonaddy.com/#pricing"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

    }

    private fun setVersion() {
        activity_app_settings_version.text = BuildConfig.VERSION_NAME
    }

    override fun OnDarkModeOff() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        settingsManager.putSettingsInt("dark_mode", 0)
        delegate.applyDayNight()
    }

    override fun OnDarkModeOn() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        settingsManager.putSettingsInt("dark_mode", 1)
        delegate.applyDayNight()
    }

    override fun OnDarkModeAutomatic() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        settingsManager.putSettingsInt("dark_mode", -1)
        delegate.applyDayNight()
    }


}