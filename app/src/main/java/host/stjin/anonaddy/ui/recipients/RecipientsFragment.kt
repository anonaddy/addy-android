package host.stjin.anonaddy.ui.recipients

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.RecipientAdapter
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.databinding.FragmentRecipientsBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientsActivity
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch


class RecipientsFragment : Fragment(),
    AddRecipientBottomDialogFragment.AddRecipientBottomDialogListener {

    companion object {
        fun newInstance() = RecipientsFragment()
    }

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private val addRecipientsFragment: AddRecipientBottomDialogFragment =
        AddRecipientBottomDialogFragment.newInstance()

    private var _binding: FragmentRecipientsBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipientsBinding.inflate(inflater, container, false)
        val root = binding.root
        settingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())


        // Set stats right away, update later
        setStats()

        setOnClickListener()

        // Called on OnResume() as well, call this in onCreateView so the viewpager can serve loaded fragments
        getDataFromWeb()
        return root
    }

    private fun getDataFromWeb() {
        binding.recipientsLL1.visibility = View.VISIBLE
        binding.recipientsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        viewLifecycleOwner.lifecycleScope.launch {
            getUserResource(requireContext())
            getAllRecipients()
        }
    }

    private fun setStats(currentCount: Int? = null) {
        binding.activityRecipientSettingsLLCount.text = requireContext().resources.getString(
            R.string.you_ve_used_d_out_of_d_recipients,
            currentCount ?: User.userResource.recipient_count,
            if (User.userResource.subscription != null) User.userResource.recipient_limit else this.resources.getString(R.string.unlimited)
        )

        // If userResource.subscription == null, that means that the user has no subscription (thus a self-hosted instance without limits)
        if (User.userResource.subscription != null) {
            binding.recipientsAddRecipients.isEnabled = User.userResource.recipient_count < User.userResource.recipient_limit
        } else {
            binding.recipientsAddRecipients.isEnabled = true
        }
    }

    private suspend fun getUserResource(context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                User.userResource = user
            } else {
                val bottomNavView: BottomNavigationView? =
                    activity?.findViewById(R.id.nav_view)
                bottomNavView?.let {
                    SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        it,
                        LoggingHelper.LOGFILES.DEFAULT
                    )
                        .apply {
                            anchorView = bottomNavView
                        }.show()
                }

            }
        }
    }

    // Update the recipients list when coming back
    override fun onResume() {
        super.onResume()
        getDataFromWeb()
    }

    private fun setOnClickListener() {
        binding.recipientsAddRecipients.setOnClickListener {
            if (!addRecipientsFragment.isAdded) {
                addRecipientsFragment.show(
                    childFragmentManager,
                    "addRecipientsFragment"
                )
            }
        }
    }


    private lateinit var recipientAdapter: RecipientAdapter
    private suspend fun getAllRecipients() {
        binding.recipientsAllRecipientsRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false

                shimmerItemCount = settingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT, 2) ?: 2
                shimmerLayoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                layoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
                showShimmer()
            }
            networkHelper?.getRecipients({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new recipients since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::recipientAdapter.isInitialized && list == recipientAdapter.getList()) {
                    return@getRecipients
                }

                if (list != null) {
                    // Update stats
                    setStats(list.size)

                    // There is always 1 recipient.

                    /*if (list.size > 0) {
                        root.recipients_no_recipients.visibility = View.GONE
                    } else {
                        root.recipients_no_recipients.visibility = View.VISIBLE
                    }*/

                    // Set the count of aliases so that the shimmerview looks better next time
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT, list.size)

                    recipientAdapter = RecipientAdapter(list)
                    recipientAdapter.setClickListener(object : RecipientAdapter.ClickListener {

                        override fun onClickSettings(pos: Int, aView: View) {
                            val intent = Intent(context, ManageRecipientsActivity::class.java)
                            intent.putExtra("recipient_id", list[pos].id)
                            intent.putExtra("recipient_email", list[pos].email)
                            startActivity(intent)
                        }

                        override fun onClickResend(pos: Int, aView: View) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                resendConfirmationMailRecipient(list[pos].id, context)
                            }
                        }

                        override fun onClickDelete(pos: Int, aView: View) {
                            deleteRecipient(list[pos].id, context)
                        }

                    })
                    adapter = recipientAdapter
                } else {
                    binding.recipientsLL1.visibility = View.GONE
                    binding.recipientsRLLottieview.visibility = View.VISIBLE
                }
                hideShimmer()
            }, verifiedOnly = false)

        }

    }

    private suspend fun resendConfirmationMailRecipient(id: String, context: Context) {
        networkHelper?.resendVerificationEmail({ result ->
            if (result == "200") {
                verificationEmailSentSnackbar(context)
            } else {
                val bottomNavView: BottomNavigationView? =
                    activity?.findViewById(R.id.nav_view)

                bottomNavView?.let {
                    SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(R.string.error_resend_verification) + "\n" + result,
                        it,
                        LoggingHelper.LOGFILES.DEFAULT
                    )
                        .apply {
                            anchorView = bottomNavView
                        }.show()
                }

            }
        }, id)

        //verificationEmailSentSnackbar(context)
    }


    private fun verificationEmailSentSnackbar(context: Context) {
        val bottomNavView: BottomNavigationView? =
            activity?.findViewById(R.id.nav_view)
        bottomNavView?.let {
            SnackbarHelper.createSnackbar(context, context.resources.getString(R.string.verification_email_has_been_sent), it).apply {
                anchorView = bottomNavView
            }.show()
        }
    }

    lateinit var dialog: AlertDialog
    private fun deleteRecipient(id: String, context: Context) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(context), null, false)
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setView(anonaddyCustomDialogBinding.root)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = context.resources.getString(R.string.delete_recipient)
        anonaddyCustomDialogBinding.dialogText.text = context.resources.getString(R.string.delete_recipient_desc)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            context.resources.getString(R.string.delete)
        anonaddyCustomDialogBinding.dialogPositiveButton.drawableBackground.setColorFilter(
            AttributeHelper.getValueByAttr(context, R.attr.colorError),
            PorterDuff.Mode.SRC_ATOP
        )
        anonaddyCustomDialogBinding.dialogPositiveButton.setTextColor(AttributeHelper.getValueByAttr(context, R.attr.colorOnError))
        anonaddyCustomDialogBinding.dialogPositiveButton.spinningBarColor = AttributeHelper.getValueByAttr(context, R.attr.colorOnError)

        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            // Animate the button to progress
            anonaddyCustomDialogBinding.dialogPositiveButton.startAnimation()

            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                deleteRecipientHttpRequest(id, context, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper?.deleteRecipient({ result ->
            if (result == "204") {
                dialog.dismiss()
                getDataFromWeb()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_recipient), result
                )
            }
        }, id)
    }

    override fun onAdded() {
        addRecipientsFragment.dismiss()
        verificationEmailSentSnackbar(requireContext())
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}