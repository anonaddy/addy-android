package host.stjin.anonaddy.ui.appsettings.logs

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityLogViewerBinding
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.SnackbarHelper

class LogViewerActivity : BaseActivity() {

    private lateinit var loggingHelper: LoggingHelper
    private lateinit var binding: ActivityLogViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogViewerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(binding.appsettingsLogviewerToolbar.customToolbarOneHandedMaterialtoolbar, R.string.logs)
        loggingHelper = LoggingHelper(this)
        loadLogs()

        binding.appsettingsLogviewerEfab.setOnClickListener {
            SnackbarHelper.createSnackbar(this, this.resources.getString(R.string.logs_cleared), binding.appsettingsLogviewerCL, false).show()
            loggingHelper.clearLogs()
            loadLogs()
        }
    }

    private fun loadLogs() {
        val logs = loggingHelper.getLogs()

        if (logs.size > 0) {
            binding.appsettingsLogviewerEfab.show()
        } else {
            binding.appsettingsLogviewerEfab.hide()
        }

        val listOfLogs = arrayListOf<String>()

        for (log in logs) {
            listOfLogs.add(log)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOfLogs)
        binding.appsettingsLogviewerListview.adapter = adapter

        binding.appsettingsLogviewerListview.setOnItemClickListener { _, _, i, _ ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, listOfLogs[i])
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }
}