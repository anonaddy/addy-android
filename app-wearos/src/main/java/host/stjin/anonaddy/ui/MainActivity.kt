package host.stjin.anonaddy.ui

import android.app.Activity
import android.os.Bundle
import androidx.wear.widget.WearableLinearLayoutManager
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.ActivityMainBinding
import host.stjin.anonaddy.utils.CustomScrollingLayoutCallback
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.GsonTools

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.AppTheme)
        setContentView(binding.root)

        val settingsManager = SettingsManager(encrypt = true, this)
        // Cache contains 15 most popular aliases
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_15_MOST_ACTIVE_ALIASES_DATA)

        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(this, it) }

        aliasesList?.let { showAliases(it) }
    }

    private fun showAliases(listWithAliases: ArrayList<Aliases>) {
        binding.activityMainRecyclerLauncherView.apply {
            layoutManager =
                WearableLinearLayoutManager(this@MainActivity, CustomScrollingLayoutCallback())

            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            var aliasAdapter = AliasAdapter(listWithAliases, this@MainActivity)
            aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                override fun onClick(pos: Int) {
                    // BEEP
                }
            })

            adapter = aliasAdapter
        }
    }
}