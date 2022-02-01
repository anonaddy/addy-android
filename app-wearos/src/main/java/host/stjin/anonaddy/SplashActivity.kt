package host.stjin.anonaddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import host.stjin.anonaddy.databinding.ActivitySplashBinding
import host.stjin.anonaddy_shared.SettingsManager
import host.stjin.anonaddy_shared.utils.GsonTools

class SplashActivity : Activity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadSettings()
    }


    private fun loadSettings() {
        val configuration = SettingsManager(true, this).getSettingsString(SettingsManager.PREFS.WEAROS_CONFIGURATION)
            ?.let { GsonTools.jsonToWearOSSettingsObject(this, it) }

        if (configuration != null) {
            binding.text.text = configuration.api_key
        } else {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            // Tell the user to setup the app
            binding.text.text = this.resources.getString(R.string.common_open_on_phone)
        }
    }

}