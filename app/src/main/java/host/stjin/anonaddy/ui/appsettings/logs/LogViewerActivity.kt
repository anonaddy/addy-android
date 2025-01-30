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
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.utils.LoggingHelper

class LogViewerActivity : BaseActivity() {

    private lateinit var loggingHelper: LoggingHelper
    private lateinit var binding: ActivityLogViewerBinding
    private lateinit var logsAdapter: LogsAdapter
    private var oneTimeRecyclerViewActions: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogViewerBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.appsettingsLogviewerNSVLL)

        val view = binding.root
        setContentView(view)

        val filename = intent.getStringExtra("logfile")
        setupToolbar(
            if (filename == LoggingHelper.LOGFILES.WEAROS_LOGS.filename) R.string.logs_wearable else R.string.logs,
            binding.appsettingsLogviewerNSV,
            binding.appsettingsLogviewerToolbar,
            R.drawable.ic_file_alert
        )

        if (filename.isNullOrEmpty()) {
            Toast.makeText(this, this.resources.getString(R.string.no_logfile_selected), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setRefreshLayout()
        loggingHelper = LoggingHelper(this, LoggingHelper.LOGFILES.entries.first { it.filename == filename })
        getAllLogsAndSetRecyclerview()

        binding.appsettingsLogviewerEfab.setOnClickListener {
            SnackbarHelper.createSnackbar(this, this.resources.getString(R.string.logs_cleared), binding.appsettingsLogviewerCL).show()
            loggingHelper.clearLogs()
            getAllLogsAndSetRecyclerview()
        }
    }

    private fun setRefreshLayout() {
        binding.appsettingsLogviewerSwiperefresh.setOnRefreshListener {
            binding.appsettingsLogviewerSwiperefresh.isRefreshing = true

            getAllLogsAndSetRecyclerview()
        }
    }


    private fun getAllLogsAndSetRecyclerview() {
        binding.appsettingsLogviewerRecyclerview.apply {
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false
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
                if (list.isNotEmpty()) {
                    binding.appsettingsLogviewerNoLogs.visibility = View.GONE
                } else {
                    binding.appsettingsLogviewerNoLogs.visibility = View.VISIBLE
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
                binding.appsettingsLogviewerNoLogs.visibility = View.VISIBLE
            }

            binding.appsettingsLogviewerSwiperefresh.isRefreshing = false

        }

    }
}