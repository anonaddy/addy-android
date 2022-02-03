package host.stjin.anonaddy

import android.app.Activity
import android.os.Bundle
import host.stjin.anonaddy.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.BaseTheme)
        setContentView(binding.root)

    }
}