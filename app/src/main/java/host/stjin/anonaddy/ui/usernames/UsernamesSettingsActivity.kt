package host.stjin.anonaddy.ui.usernames

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.UsernameAdapter
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernamesActivity
import kotlinx.android.synthetic.main.activity_username_settings.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UsernamesSettingsActivity : BaseActivity(), AddUsernameBottomDialogFragment.AddUsernameBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true

    private val addUsernameFragment: AddUsernameBottomDialogFragment = AddUsernameBottomDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username_settings)
        setupToolbar(activity_username_settings_toolbar)

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        // Set stats right away, update later
        setStats()

        setOnClickListener()
        // Called on OnResume()
        // getDataFromWeb()
    }

    private fun setOnClickListener() {
        activity_username_settings_add_username.setOnClickListener {
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
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllUsernames()
            getUserResource()
        }
    }

    private suspend fun getUserResource() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                User.userResource = user
                setStats()
            } else {
                val snackbar =
                    Snackbar.make(
                        activity_username_settings_LL,
                        resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        Snackbar.LENGTH_SHORT
                    )

                if (SettingsManager(false, baseContext).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(baseContext, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }
    }

    private fun setStats() {
        activity_username_settings_RL_count_text.text =
            resources.getString(R.string.you_ve_used_d_out_of_d_usernames, User.userResource.username_count, User.userResource.username_limit)
        activity_username_settings_add_username.isEnabled = User.userResource.username_count < User.userResource.username_limit
    }

    private suspend fun getAllUsernames() {
        activity_username_settings_all_usernames_recyclerview.apply {

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
            layoutManager = LinearLayoutManager(context)
            // set the custom adapter to the RecyclerView

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                activity_username_settings_all_usernames_recyclerview.layoutAnimation = animation
            }


            networkHelper?.getAllUsernames { list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                if (list != null) {

                    if (list.size > 0) {
                        activity_username_settings_no_usernames.visibility = View.GONE
                    } else {
                        activity_username_settings_no_usernames.visibility = View.VISIBLE
                    }

                    val usernamesAdapter = UsernameAdapter(list)
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
                    activity_username_settings_all_usernames_recyclerview.hideShimmerAdapter()
                } else {
                    activity_username_settings_LL1.visibility = View.GONE
                    activity_username_settings_RL_lottieview.visibility = View.VISIBLE
                }
            }

        }

    }


    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun deleteUsername(id: String, context: Context) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = context.resources.getString(R.string.delete_username)
        customLayout.dialog_text.text = context.resources.getString(R.string.delete_username_desc_confirm)
        customLayout.dialog_positive_button.text =
            context.resources.getString(R.string.delete_username)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteUsernameHttpRequest(id, context)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteUsernameHttpRequest(id: String, context: Context) {
        networkHelper?.deleteUsername(id) { result ->
            if (result == "204") {
                dialog.dismiss()
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                    getAllUsernames()
                    getUserResource()
                }
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text =
                    context.resources.getString(R.string.error_deleting_username) + "\n" + result
            }
        }
    }

    override fun onAdded() {
        addUsernameFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllUsernames()
            getUserResource()
        }
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getDataFromWeb()
            getUserResource()
        }
    }
}