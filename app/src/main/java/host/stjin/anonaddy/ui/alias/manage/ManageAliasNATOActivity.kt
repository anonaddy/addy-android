package host.stjin.anonaddy.ui.alias.manage

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.NatoAdapter
import host.stjin.anonaddy.databinding.ActivityManageAliasNatoBinding
import host.stjin.anonaddy.utils.NatoAlphabet

class ManageAliasNATOActivity : BaseActivity() {

    private lateinit var binding: ActivityManageAliasNatoBinding
    private var alias: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAliasNatoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Only setup toolbar in portrait mode
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setupToolbar(R.string.alias, null,
                binding.activityManageAliasNatoToolbar,
                R.drawable.ic_letters_case)
        } else {
            // Hide toolbar in landscape mode
            binding.activityManageAliasNatoToolbar?.root?.visibility = View.GONE
        }

        alias = intent.getStringExtra("alias")

        if (alias == null) {
            finish()
            return
        }

        setupRecyclerView()

        binding.activityManageAliasNatoFab?.setOnClickListener {
            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private fun setupRecyclerView() {
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutManager = GridLayoutManager(this, 5)
            binding.activityManageAliasNatoRecyclerview.layoutManager = layoutManager
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.activityManageAliasNatoRecyclerview)
        } else {
            val layoutManager = LinearLayoutManager(this)
            binding.activityManageAliasNatoRecyclerview.layoutManager = layoutManager
        }


        val natoList = ArrayList<NatoAlphabet.NatoItem>()
        for (char in alias!!) {
            natoList.add(NatoAlphabet.getWord(char))
        }

        val adapter = NatoAdapter(natoList, currentOrientation)
        binding.activityManageAliasNatoRecyclerview.adapter = adapter
    }
}
