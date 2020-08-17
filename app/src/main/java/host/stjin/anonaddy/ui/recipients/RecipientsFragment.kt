package host.stjin.anonaddy.ui.recipients

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.RecipientAdapter
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientsActivity
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.android.synthetic.main.fragment_recipients.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class RecipientsFragment : Fragment(),
    AddRecipientBottomDialogFragment.AddRecipientBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true

    private val addRecipientsFragment: AddRecipientBottomDialogFragment =
        AddRecipientBottomDialogFragment.newInstance()

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
        settingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        setOnClickListener(root, requireContext())

        getDataFromWeb(root)
        return root
    }

    private fun getDataFromWeb(root: View) {
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllRecipients(root)
        }
    }

    override fun onResume() {
        super.onResume()
        getDataFromWeb(requireView())
    }

    private fun setOnClickListener(root: View, context: Context) {
        root.recipients_add_recipients.setOnClickListener {
            addRecipientsFragment.show(
                childFragmentManager,
                "addRecipientsFragment"
            )
        }
    }


    private suspend fun getAllRecipients(root: View) {
        root.recipients_all_recipients_recyclerview.apply {

            if (itemDecorationCount > 0) {
                addItemDecoration(
                    DividerItemDecoration(
                        this.context,
                        (layoutManager as LinearLayoutManager).orientation
                    )
                )
            }
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                root.recipients_all_recipients_recyclerview.layoutAnimation = animation
            }


            networkHelper?.getRecipients({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                if (list != null) {

                    // There is always 1 recipient.

                    /*if (list.size > 0) {
                        root.recipients_no_recipients.visibility = View.GONE
                    } else {
                        root.recipients_no_recipients.visibility = View.VISIBLE
                    }*/

                    val recipientAdapter = RecipientAdapter(list)
                    recipientAdapter.setClickListener(object : RecipientAdapter.ClickListener {

                        override fun onClickSettings(pos: Int, aView: View) {
                            val intent = Intent(context, ManageRecipientsActivity::class.java)
                            intent.putExtra("recipient_id", list[pos].id)
                            intent.putExtra("recipient_email", list[pos].email)
                            startActivity(intent)
                        }

                        override fun onClickResend(pos: Int, aView: View) {
                            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                                resendConfirmationMailRecipient(list[pos].id, list[pos].email, context)
                            }
                        }

                        override fun onClickDelete(pos: Int, aView: View) {
                            deleteRecipient(list[pos].id, context)
                        }

                    })
                    adapter = recipientAdapter
                    root.recipients_all_recipients_recyclerview.hideShimmerAdapter()
                } else {
                    root.recipients_LL1.visibility = View.GONE
                    root.recipients_RL_lottieview.visibility = View.VISIBLE
                }
            }, verifiedOnly = false)

        }

    }

    private fun resendConfirmationMailRecipient(id: String, email: String, context: Context) {

        //TODO FIX

        //verificationEmailSentSnackbar(context)
    }

    private fun verificationEmailSentSnackbar(context: Context) {
        val bottomNavView: BottomNavigationView? =
            activity?.findViewById(R.id.nav_view)
        bottomNavView?.let {
            Snackbar.make(
                it,
                context.resources.getString(R.string.verification_email_has_been_sent),
                Snackbar.LENGTH_SHORT
            ).apply {
                anchorView = bottomNavView
            }.show()
        }
    }

    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun deleteRecipient(id: String, context: Context) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = context.resources.getString(R.string.delete_recipient)
        customLayout.dialog_text.text = context.resources.getString(R.string.delete_recipient_desc)
        customLayout.dialog_positive_button.text =
            context.resources.getString(R.string.delete_recipient)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteRecipientHttpRequest(id, context)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context) {
        networkHelper?.deleteRecipient(id) { result ->
            if (result == "204") {
                dialog.dismiss()
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                    getAllRecipients(requireView())
                }
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text =
                    context.resources.getString(R.string.error_deleting_recipient) + "\n" + result
            }
        }
    }

    override fun onAdded() {
        addRecipientsFragment.dismiss()
        verificationEmailSentSnackbar(requireContext())
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllRecipients(requireView())
        }
    }


}