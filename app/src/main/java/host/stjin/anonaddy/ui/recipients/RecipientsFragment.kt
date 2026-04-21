package host.stjin.anonaddy.ui.recipients

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.RecipientAdapter
import host.stjin.anonaddy.databinding.FragmentRecipientsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientsActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class RecipientsFragment : Fragment(), AddRecipientBottomDialogFragment.AddRecipientBottomDialogListener, Refreshable {

    private var recipients: ArrayList<Recipients>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private val addRecipientsFragment: AddRecipientBottomDialogFragment = AddRecipientBottomDialogFragment.newInstance()
    private var _binding: FragmentRecipientsBinding? = null
    private val binding get() = _binding!!

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null)
            }
        }
    }

    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.recipientsNSV.post { binding.recipientsNSV.smoothScrollTo(0, 0) }
        }
    }

    private lateinit var recipientAdapter: RecipientAdapter
    private lateinit var deleteRecipientSnackbar: Snackbar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipientsBinding.inflate(inflater, container, false)

        val root = binding.root
        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        setStats()
        setOnClickListener()
        setNsvListener()
        setRecipientRecyclerView()
        getDataFromWeb(savedInstanceState)

        return root
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"), Context.RECEIVER_EXPORTED)
        } else {
            activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(mScrollUpBroadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(recipients)
        outState.putString("recipients", json)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun setNsvListener() {
        binding.recipientsNSV.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ -> setHasReachedTopOfNsv() })
    }

    private fun setRecipientRecyclerView() {
        binding.recipientsAllRecipientsRecyclerview.apply {
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(context))
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()

                binding.recipientsChipgroup.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (checkedIds.isNotEmpty()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            getAllRecipients()
                        }
                    }
                }
            }
        }
    }

    private fun setRecipientAdapter(list: ArrayList<Recipients>) {
        binding.recipientsAllRecipientsRecyclerview.apply {
            recipients = list

            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT, list.size)

            recipientAdapter = RecipientAdapter(list)
            recipientAdapter.setClickListener(object : RecipientAdapter.ClickListener {

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, ManageRecipientsActivity::class.java)
                    intent.putExtra("recipient_id", list[pos].id)
                    resultLauncher.launch(intent)
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
            hideShimmer()
            adapter = recipientAdapter
        }
    }

    fun getDataFromWeb(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (savedInstanceState != null) {
                setStats()
                val recipientsJson = savedInstanceState.getString("recipients")
                if (!recipientsJson.isNullOrEmpty() && recipientsJson != "null") {
                    val gson = Gson()
                    val myType = object : TypeToken<ArrayList<Recipients>>() {}.type
                    val list = gson.fromJson<ArrayList<Recipients>>(recipientsJson, myType)
                    setRecipientAdapter(list)
                } else {
                    getUserResource()
                    getAllRecipients()
                }
            } else {
                getUserResource()
                getAllRecipients()
            }
        }
    }

    private fun getSelectedFilter(): Boolean {
        return binding.recipientsChipgroup.checkedChipId == R.id.recipients_chip_verified_only
    }

    private fun setHasReachedTopOfNsv() {
        (activity as MainActivity).hasReachedTopOfNsv = !binding.recipientsNSV.canScrollVertically(-1)
    }

    private fun setStats() {
        binding.activityRecipientSettingsLLCount.text = requireContext().resources.getString(
            R.string.you_ve_used_d_out_of_d_recipients,
            (activity?.application as AddyIoApp).userResource.recipient_count,
            if ((activity?.application as AddyIoApp).userResource.subscription != null) (activity?.application as AddyIoApp).userResource.recipient_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        if ((activity?.application as AddyIoApp).userResource.subscription != null) {
            binding.recipientsAddRecipients.isEnabled =
                (activity?.application as AddyIoApp).userResource.recipient_count < (activity?.application as AddyIoApp).userResource.recipient_limit!!
        } else {
            binding.recipientsAddRecipients.isEnabled = true
        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AddyIoApp).userResource = user
                setStats()
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_obtaining_user) + "\\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)
                    bottomNavView?.let {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.error_obtaining_user) + "\\n" + result,
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
    }

    private suspend fun getAllRecipients() {
        binding.recipientsAllRecipientsRecyclerview.showShimmer()

        if (!getSelectedFilter()) {
            binding.recipientsAllRecipientsTitle.text = getString(R.string.recipients)
        } else {
            binding.recipientsAllRecipientsTitle.text = getString(R.string.recipients_filtered)
        }

        networkHelper?.getRecipients({ list, result ->
            if (::recipientAdapter.isInitialized && list == recipientAdapter.getList()) {
                binding.recipientsAllRecipientsRecyclerview.hideShimmer()
                return@getRecipients
            }

            if (list != null) {
                setRecipientAdapter(list)
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_obtaining_recipients) + "\\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)
                    bottomNavView?.let {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.error_obtaining_recipients) + "\\n" + result,
                            it,
                            LoggingHelper.LOGFILES.DEFAULT
                        )
                            .apply {
                                anchorView = bottomNavView
                            }.show()
                    }
                }
            }
        }, verifiedOnly = getSelectedFilter())
    }

    private suspend fun resendConfirmationMailRecipient(id: String, context: Context) {
        networkHelper?.resendVerificationEmail({ result ->
            if (result == "200") {
                verificationEmailSentSnackbar(context)
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(R.string.error_resend_verification) + "\\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)

                    bottomNavView?.let {
                        SnackbarHelper.createSnackbar(
                            context,
                            context.resources.getString(R.string.error_resend_verification) + "\\n" + result,
                            it,
                            LoggingHelper.LOGFILES.DEFAULT
                        )
                            .apply {
                                anchorView = bottomNavView
                            }.show()
                    }
                }
            }
        }, id)
    }

    private fun verificationEmailSentSnackbar(context: Context) {
        if (requireContext().resources.getBoolean(R.bool.isTablet)) {
            SnackbarHelper.createSnackbar(
                context,
                context.resources.getString(R.string.verification_email_has_been_sent),
                (activity as MainActivity).findViewById(R.id.main_container)
            ).show()
        } else {
            val bottomNavView: BottomNavigationView? =
                activity?.findViewById(R.id.nav_view)
            bottomNavView?.let {
                SnackbarHelper.createSnackbar(context, context.resources.getString(R.string.verification_email_has_been_sent), it).apply {
                    anchorView = bottomNavView
                }.show()
            }
        }
    }

    private fun deleteRecipient(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = context,
            title = resources.getString(R.string.delete_recipient),
            message = resources.getString(R.string.delete_recipient_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                        context,
                        this.resources.getString(R.string.deleting_recipient),
                        (activity as MainActivity).findViewById(R.id.main_container),
                        length = Snackbar.LENGTH_INDEFINITE
                    )
                    deleteRecipientSnackbar.show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)
                    bottomNavView?.let {
                        deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                            context,
                            this.resources.getString(R.string.deleting_recipient),
                            it,
                            length = Snackbar.LENGTH_INDEFINITE
                        ).apply {
                            anchorView = bottomNavView
                        }
                        deleteRecipientSnackbar.show()
                    }
                }

                lifecycleScope.launch {
                    deleteRecipientHttpRequest(id, context)
                }
            }
        ).show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context) {
        networkHelper?.deleteRecipient({ result ->
            if (result == "204") {
                deleteRecipientSnackbar.dismiss()
                getDataFromWeb(null)
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_recipient), result
                        ),
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    )
                    deleteRecipientSnackbar.show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)
                    bottomNavView?.let {
                        deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                            context,
                            context.resources.getString(
                                R.string.s_s,
                                context.resources.getString(R.string.error_deleting_recipient), result
                            ),
                            it,
                            LoggingHelper.LOGFILES.DEFAULT
                        ).apply {
                            anchorView = bottomNavView
                        }
                        deleteRecipientSnackbar.show()
                    }
                }
            }
        }, id)
    }

    override fun onAdded() {
        addRecipientsFragment.dismissAllowingStateLoss()
        verificationEmailSentSnackbar(requireContext())
        getDataFromWeb(null)
    }

    override fun onRefreshData() {
        if (!isAdded) return
        try {
            viewLifecycleOwner.lifecycleScope.launch {
                getDataFromWeb(null)
            }
        } catch (e: IllegalStateException) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "RecipientsFragment",
                null
            )
        }
    }

    companion object {
        fun newInstance() = RecipientsFragment()
    }
}
