package host.stjin.anonaddy.ui.appsettings.logs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.LogsAdapter
import host.stjin.anonaddy.databinding.ActivityLogViewerBinding
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper

class LogViewerActivity : BaseActivity() {

    private lateinit var loggingHelper: LoggingHelper
    private lateinit var binding: ActivityLogViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogViewerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // TODO Fix this eFab being hidden below navbar
        drawBehindNavBar(view, binding.appsettingsLogviewerNSVLL)

        setupToolbar(binding.appsettingsLogviewerToolbar.customToolbarOneHandedMaterialtoolbar, R.string.logs)

        val filename = intent.getStringExtra("logfile")
        if (filename.isNullOrEmpty()) {
            Toast.makeText(this, this.resources.getString(R.string.no_logfile_selected), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loggingHelper = LoggingHelper(this, LoggingHelper.LOGFILES.values().first { it.filename == filename })
        getAllLogsAndSetRecyclerview()

        binding.appsettingsLogviewerEfab.setOnClickListener {
            SnackbarHelper.createSnackbar(this, this.resources.getString(R.string.logs_cleared), binding.appsettingsLogviewerCL).show()
            loggingHelper.clearLogs()
            getAllLogsAndSetRecyclerview()
        }
    }


    private lateinit var logsAdapter: LogsAdapter
    private var OneTimeRecyclerViewActions: Boolean = true

    private fun getAllLogsAndSetRecyclerview() {
        binding.appsettingsLogviewerRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
            }

            //Retrieve the values
            //Retrieve the values
            val list = loggingHelper.getLogs()
            list?.reverse()

            if (list != null) {
                if (list.size > 0) {
                    binding.activityFailedDeliveriesNoLogs.visibility = View.GONE
                } else {
                    binding.activityFailedDeliveriesNoLogs.visibility = View.VISIBLE
                }


                logsAdapter = LogsAdapter(list)
                logsAdapter.setClickListener(object : LogsAdapter.ClickListener {

                    override fun onClickDetails(pos: Int, aView: View) {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, list[pos].message + "\n" + list[pos].method + "\n" + list[pos].extra)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                    }

                })
                adapter = logsAdapter

            } else {
                binding.appsettingsLogviewerRecyclerview.visibility = View.GONE
                binding.activityFailedDeliveriesNoLogs.visibility = View.VISIBLE
            }
        }

    }
}