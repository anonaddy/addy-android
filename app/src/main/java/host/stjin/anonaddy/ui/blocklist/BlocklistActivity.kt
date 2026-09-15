package host.stjin.anonaddy.ui.blocklist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityBlocklistSettingsBinding
import host.stjin.anonaddy.utils.DeepLinkActionHelper
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.models.NewBlocklistEntry
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class BlocklistActivity : BaseActivity() {
    private val blocklistViewModel: BlocklistViewModel by viewModels()
    private lateinit var binding: ActivityBlocklistSettingsBinding

    private fun getFragment(): BlocklistFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_blocklist_settings_fcv) as? BlocklistFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlocklistSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.blocklist,
            null,
            binding.activityBlocklistSettingsToolbar,
            R.drawable.ic_close
        )

        setRefreshLayout()
        setPage()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requireAuthentication {
            handleIntent(intent)
        }
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityBlocklistSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityBlocklistSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_blocklist_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_blocklist_settings_fcv, BlocklistFragment.newInstance())
                    .commit()
            }
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val (_, blockAction) = DeepLinkActionHelper.parseAction(data.toString())
            if (blockAction != null) {
                promptBlockConfirmation(blockAction)
            }
        }
    }

    private fun promptBlockConfirmation(action: DeepLinkActionHelper.BlockAction) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.blocklist_add),
            message = resources.getString(R.string.blocklist_add_confirm_desc, action.value),
            icon = R.drawable.ic_forbid,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.blocklist_add),
            positiveButtonAction = {
                lifecycleScope.launch {
                    blockSender(action.type, action.value)
                }
            }
        ).show()
    }

    private suspend fun blockSender(type: String, value: String) {
        when (val result = blocklistViewModel.addBlocklistEntry(NewBlocklistEntry(type, value))) {
            is NetworkResult.Success -> {
                val message = if (type == "domain") {
                    resources.getString(R.string.blocklist_add_domain_success)
                } else {
                    resources.getString(R.string.blocklist_add_success)
                }
                SnackbarHelper.createSnackbar(
                    this,
                    message,
                    binding.root
                ).show()
                getFragment()?.onRefreshData()
            }
            is NetworkResult.Error -> {
                MaterialDialogHelper.showMaterialDialog(
                    context = this,
                    title = resources.getString(R.string.blocklist_add),
                    message = if (!result.error.isNullOrEmpty()) {
                        result.error
                    } else {
                        resources.getString(R.string.error_adding_blocklist_entry)
                    },
                    icon = R.drawable.ic_forbid,
                    neutralButtonText = resources.getString(R.string.close)
                ).show()
            }
        }
    }
}
