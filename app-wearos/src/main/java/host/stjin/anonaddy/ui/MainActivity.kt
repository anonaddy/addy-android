package host.stjin.anonaddy.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.wear.widget.WearableLinearLayoutManager
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.ActivityMainBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.utils.CustomScrollingLayoutCallback
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.GsonTools

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var favoriteAliasHelper: FavoriteAliasHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        favoriteAliasHelper = FavoriteAliasHelper(this)

        val settingsManager = SettingsManager(encrypt = true, this)
        // Cache contains 15 most popular aliases
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_15_MOST_ACTIVE_ALIASES_DATA)

        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(this, it) }

        aliasesList?.let { showAliases(it) }


        // Schedule the background worker (in case this has not been done before) (this will cancel if already scheduled)
        BackgroundWorkerHelper(this@MainActivity).scheduleBackgroundWorker()
    }


    private fun showAliases(listWithAliases: ArrayList<Aliases>) {

        binding.activityMainRecyclerLauncherView.apply {
            isEdgeItemsCenteringEnabled = true
            layoutManager =
                WearableLinearLayoutManager(this@MainActivity, CustomScrollingLayoutCallback())
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))


            val aliasAdapter = AliasAdapter(listWithAliases, this@MainActivity)
            aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                override fun onClick(pos: Int) {
                    val intent = Intent(this@MainActivity, ManageAliasActivity::class.java)
                    intent.putExtra("alias", listWithAliases[pos])
                    startActivity(intent)
                }

                override fun onClickStar(pos: Int) {
                    val favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
                    if (favoriteAliases?.contains(listWithAliases[pos].id) == true) {
                        favoriteAliasHelper.removeAliasAsFavorite(listWithAliases[pos].id)
                    } else {
                        if (!favoriteAliasHelper.addAliasAsFavorite(listWithAliases[pos].id)) {
                            Toast.makeText(this@MainActivity, resources.getString(R.string.max_favorites_reached), Toast.LENGTH_SHORT).show()
                        }
                    }
                    aliasAdapter.notifyItemChanged(pos)
                }
            })

            adapter = aliasAdapter
            requestFocus()
        }
        LinearSnapHelper().attachToRecyclerView(binding.activityMainRecyclerLauncherView)
    }


}