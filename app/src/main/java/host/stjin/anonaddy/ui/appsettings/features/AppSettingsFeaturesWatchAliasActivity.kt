package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesWatchAliasBinding
import host.stjin.anonaddy.utils.InsetUtils


class AppSettingsFeaturesWatchAliasActivity : BaseActivity() {
    private lateinit var binding: ActivityAppSettingsFeaturesWatchAliasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesWatchAliasBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.appsettingsFeaturesWatchAliasNSVLL)

        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.watch_alias,
            binding.appsettingsFeaturesWatchAliasNSV,
            binding.appsettingsFeaturesWatchAliasToolbar,
            R.drawable.ic_watch_alias
        )
    }
}
