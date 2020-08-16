package host.stjin.anonaddy.ui.appsettings.logs

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.utils.LoggingHelper
import kotlinx.android.synthetic.main.activity_log_viewer.*

class LogViewerActivity : BaseActivity() {

    private lateinit var loggingHelper: LoggingHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        setupToolbar(appsettings_logviewer_toolbar)
        loggingHelper = LoggingHelper(this)
        loadLogs()

        appsettings_logviewer_efab.setOnClickListener {
            Snackbar.make(
                findViewById(R.id.appsettings_logviewer_RL),
                resources.getString(R.string.logs_cleared),
                Snackbar.LENGTH_SHORT
            ).show()
            loggingHelper.clearLogs()
            loadLogs()
        }
    }

    private fun loadLogs() {
        val logs = loggingHelper.getLogs()

        if (logs.size > 0) {
            appsettings_logviewer_efab.show()
        } else {
            appsettings_logviewer_efab.hide()
        }

        val listOfLogs = arrayListOf<String>()

        for (log in logs) {
            listOfLogs.add(log)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOfLogs)
        appsettings_logviewer_listview.adapter = adapter

        appsettings_logviewer_listview.setOnItemClickListener { _, _, i, _ ->
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