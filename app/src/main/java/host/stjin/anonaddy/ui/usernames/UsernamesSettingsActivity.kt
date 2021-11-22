package host.stjin.anonaddy.ui.usernames

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.UsernameAdapter
import host.stjin.anonaddy.databinding.ActivityUsernameSettingsBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernamesActivity
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch

class UsernamesSettingsActivity : BaseActivity(), AddUsernameBottomDialogFragment.AddUsernameBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private val addUsernameFragment: AddUsernameBottomDialogFragment = AddUsernameBottomDialogFragment.newInstance()

    private lateinit var binding: ActivityUsernameSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.activityUsernameSettingsNSVRL)

        setupToolbar(
            binding.activityUsernameSettingsToolbar.customToolbarOneHandedMaterialtoolbar, R.string.manage_usernames,
            binding.activityUsernameSettingsToolbar.customToolbarOneHandedImage,
            R.drawable.ic_user_menu
        )

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        // Set stats right away, update later
        setStats()

        setOnClickListener()
        // Called on OnResume()
        // getDataFromWeb()
    }

    private fun setOnClickListener() {
        binding.activityUsernameSettingsAddUsername.setOnClickListener {
            if (!addUsernameFragment.isAdded) {
                addUsernameFragment.show(
                    supportFragmentManager,
                    "addUsernameFragment"
                )
            }
        }
    }

    private fun getDataFromWeb() {
        binding.activityUsernameSettingsLL1.visibility = View.VISIBLE
        binding.activityUsernameSettingsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getUserResource()
            getAllUsernamesAndSetView()
        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                setStats()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    resources.getString(R.string.error_obtaining_user) + "\n" + result,
                    binding.activityUsernameSettingsCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private fun setStats(currentCount: Int? = null) {
        binding.activityUsernameSettingsRLCountText.text =
            resources.getString(
                R.string.you_ve_used_d_out_of_d_usernames,
                currentCount ?: User.userResource.username_count,
                if (User.userResource.subscription != null) User.userResource.username_limit else this.resources.getString(R.string.unlimited)
            )

        // If userResource.subscription == null, that means that the user has no subscription (thus a self-hosted instance without limits)
        if (User.userResource.subscription != null) {
            binding.activityUsernameSettingsAddUsername.isEnabled = User.userResource.username_count < User.userResource.username_limit
        } else {
            binding.activityUsernameSettingsAddUsername.isEnabled = true
        }
    }

    private lateinit var usernamesAdapter: UsernameAdapter
    private suspend fun getAllUsernamesAndSetView() {

        binding.activityUsernameSettingsAllUsernamesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false

                shimmerItemCount = settingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USERNAME_COUNT, 2) ?: 2
                shimmerLayoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(this@UsernamesSettingsActivity, 2)
                } else {
                    LinearLayoutManager(this@UsernamesSettingsActivity)
                }

                layoutManager = if (this@UsernamesSettingsActivity.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(this@UsernamesSettingsActivity, 2)
                } else {
                    LinearLayoutManager(this@UsernamesSettingsActivity)
                }


                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
            networkHelper?.getAllUsernames { list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new usernames since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::usernamesAdapter.isInitialized && list == usernamesAdapter.getList()) {
                    return@getAllUsernames
                }

                if (list != null) {
                    // Update stats
                    setStats(list.size)

                    if (list.size > 0) {
                        binding.activityUsernameSettingsNoUsernames.visibility = View.GONE
                    } else {
                        binding.activityUsernameSettingsNoUsernames.visibility = View.VISIBLE
                    }

                    // Set the count of aliases so that the shimmerview looks better next time
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USERNAME_COUNT, list.size)


                    usernamesAdapter = UsernameAdapter(list)
                    usernamesAdapter.setClickListener(object : UsernameAdapter.ClickListener {

                        override fun onClickSettings(pos: Int, aView: View) {
                            val intent = Intent(context, ManageUsernamesActivity::class.java)
                            intent.putExtra("username_id", list[pos].id)
                            startActivity(intent)
                        }


                        override fun onClickDelete(pos: Int, aView: View) {
                            deleteUsername(list[pos].id, context)
                        }

                    })
                    adapter = usernamesAdapter
                } else {
                    binding.activityUsernameSettingsLL1.visibility = View.GONE
                    binding.activityUsernameSettingsRLLottieview.visibility = View.VISIBLE
                }
                hideShimmer()
            }

        }

    }


    lateinit var dialog: AlertDialog
    private fun deleteUsername(id: String, context: Context) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)

        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = context.resources.getString(R.string.delete_username)
        anonaddyCustomDialogBinding.dialogText.text = context.resources.getString(R.string.delete_username_desc_confirm)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            context.resources.getString(R.string.delete)
        anonaddyCustomDialogBinding.dialogPositiveButton.drawableBackground.setColorFilter(
            AttributeHelper.getValueByAttr(this, R.attr.colorError),
            PorterDuff.Mode.SRC_ATOP
        )
        anonaddyCustomDialogBinding.dialogPositiveButton.setTextColor(AttributeHelper.getValueByAttr(this, R.attr.colorOnError))
        anonaddyCustomDialogBinding.dialogPositiveButton.spinningBarColor = AttributeHelper.getValueByAttr(this, R.attr.colorOnError)

        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            // Animate the button to progress
            anonaddyCustomDialogBinding.dialogPositiveButton.startAnimation()

            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            lifecycleScope.launch {
                deleteUsernameHttpRequest(id, context, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteUsernameHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper?.deleteUsername({ result ->
            if (result == "204") {
                dialog.dismiss()
                getDataFromWeb()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text =
                    context.resources.getString(R.string.s_s, context.resources.getString(R.string.error_deleting_username), result)
            }
        }, id)
    }

    override fun onAdded() {
        addUsernameFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }
}