package host.stjin.anonaddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import host.stjin.anonaddy.databinding.ActivitySplashBinding

class SplashActivity : Activity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadSettings()
    }


    private fun loadSettings() {

        if ((application as AnonAddyForWearOS).wearOSSettings == null) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}