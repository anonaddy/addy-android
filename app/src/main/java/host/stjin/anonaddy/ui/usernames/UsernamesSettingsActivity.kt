package host.stjin.anonaddy.ui.usernames

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.UsernameAdapter
import host.stjin.anonaddy.databinding.ActivityUsernameSettingsBinding
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernamesActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class UsernamesSettingsActivity : BaseActivity(), AddUsernameBottomDialogFragment.AddUsernameBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private val addUsernameFragment: AddUsernameBottomDialogFragment = AddUsernameBottomDialogFragment.newInstance()

    private lateinit var binding: ActivityUsernameSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityUsernameSettingsLL1)
        )

        setupToolbar(
            R.string.manage_usernames,
            binding.activityUsernameSettingsNSV,
            binding.activityUsernameSettingsToolbar,
            R.drawable.ic_user_menu
        )

        encryptedSettingsManager = SettingsManager(true, this)
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
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getAllUsernamesAndSetView()
            getUserResource()
        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (this.application as AddyIoApp).userResource = user
                // Update stats
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

    private fun setStats() {
        binding.activityUsernameSettingsRLCountText.text =
            resources.getString(
                R.string.you_ve_used_d_out_of_d_usernames,
                (this.application as AddyIoApp).userResource.username_count,
                if ((this.application as AddyIoApp).userResource.subscription != null) (this.application as AddyIoApp).userResource.username_limit else this.resources.getString(
                    R.string.unlimited
                )
            )

        // If userResource.subscription == null, that means that the user has no subscription (thus a self-hosted instance without limits)
        if ((this.application as AddyIoApp).userResource.subscription != null) {
            binding.activityUsernameSettingsAddUsername.isEnabled =
                (this.application as AddyIoApp).userResource.username_count < (this.application as AddyIoApp).userResource.username_limit
        } else {
            binding.activityUsernameSettingsAddUsername.isEnabled = true
        }
    }

    private lateinit var usernamesAdapter: UsernameAdapter
    private suspend fun getAllUsernamesAndSetView() {
        binding.activityUsernameSettingsAllUsernamesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false

                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USERNAME_COUNT, 2) ?: 2
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
            networkHelper?.getAllUsernames { list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new usernames since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::usernamesAdapter.isInitialized && list == usernamesAdapter.getList()) {
                    return@getAllUsernames
                }

                if (list != null) {
                    if (list.size > 0) {
                        binding.activityUsernameSettingsNoUsernames.visibility = View.GONE
                    } else {
                        binding.activityUsernameSettingsNoUsernames.visibility = View.VISIBLE
                    }

                    // Set the count of aliases so that the shimmerview looks better next time
                    encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USERNAME_COUNT, list.size)


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

                    binding.animationFragment.stopAnimation()
                    //binding.activityUsernameSettingsNSV.animate().alpha(1.0f) -> Do not animate as there is a shimmerview
                } else {
                    SnackbarHelper.createSnackbar(
                        this@UsernamesSettingsActivity,
                        this@UsernamesSettingsActivity.resources.getString(R.string.error_obtaining_usernames) + "\n" + error,
                        binding.activityUsernameSettingsCL
                    ).show()

                    // Show error animations
                    binding.activityUsernameSettingsLL1.visibility = View.GONE
                    binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                }
                hideShimmer()
            }

        }

    }


    private lateinit var deleteUsernameSnackbar: Snackbar
    private fun deleteUsername(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.delete_username),
            message = resources.getString(R.string.delete_username_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteUsernameSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_username),
                    binding.activityUsernameSettingsCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteUsernameSnackbar.show()
                lifecycleScope.launch {
                    deleteUsernameHttpRequest(id, context)
                }
            }
        ).show()
    }

    private suspend fun deleteUsernameHttpRequest(id: String, context: Context) {
        networkHelper?.deleteUsername({ result ->
            if (result == "204") {
                deleteUsernameSnackbar.dismiss()
                getDataFromWeb()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(R.string.s_s, context.resources.getString(R.string.error_deleting_username), result),
                    binding.activityUsernameSettingsCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    override fun onAdded() {
        addUsernameFragment.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }
}