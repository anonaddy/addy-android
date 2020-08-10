package host.stjin.anonaddy.ui.recipients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.RecipientAdapter
import kotlinx.android.synthetic.main.fragment_recipients.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RecipientsFragment : Fragment() {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_recipients, container, false)
        val context = this.context
        if (context != null) {
            settingsManager = SettingsManager(true, context)
            networkHelper = NetworkHelper(context)


            // Get the latest data in the background, and update the values when loaded
            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                getAllRecipients(root)
            }

        }
        return root
    }


    private suspend fun getAllRecipients(root: View) {
        root.recipients_all_recipients_recyclerview.apply {

            addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView

            networkHelper?.getRecipients({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                val recipientAdapter = list?.let { RecipientAdapter(it) }
                recipientAdapter?.setClickListener(object : RecipientAdapter.ClickListener {
                    override fun onClick(pos: Int, aView: View) {
                        TODO("Not yet implemented")
                    }

                    override fun onClickSettings(pos: Int, aView: View) {
                        TODO("Not yet implemented")
                    }

                    override fun onClickResend(pos: Int, aView: View) {
                        TODO("Not yet implemented")
                    }

                    override fun onClickDelete(pos: Int, aView: View) {
                        TODO("Not yet implemented")
                    }

                })
                adapter = recipientAdapter
                root.recipients_all_recipients_recyclerview.hideShimmerAdapter()
            }, verifiedOnly = false)

        }

    }


}