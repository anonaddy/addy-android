package host.stjin.anonaddy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.ui.DialogActivity
import host.stjin.anonaddy.ui.setup.SetupActivity
import kotlinx.android.synthetic.main.main_top_bar_not_user.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Open setup
        val settingsManager = SettingsManager(true, this)
        if (settingsManager.getSettingsString("API_KEY") == null) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        checkForDarkModeAndSetFlags()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        navView.setupWithNavController(navController)

        changeTopBarNotification(true)
        initialiseMainAppBar()
    }

    private fun initialiseMainAppBar() {
        main_top_bar_user_initials.setOnClickListener {
            val i = Intent(Intent(this, DialogActivity::class.java))
            val options = ActivityOptions
                .makeSceneTransitionAnimation(
                    this as Activity?,
                    Pair.create(
                        findViewById(R.id.main_top_bar_user_initials),
                        "background_transition"
                    ),
                    Pair.create(
                        findViewById(R.id.main_top_bar_user_initials), "image_transition"
                    )
                )
            startActivity(i, options.toBundle())
        }
    }

    @SuppressLint("SwitchIntDef")
    fun checkForDarkModeAndSetFlags() {
        val settingsManager = SettingsManager(false, this)
        if (settingsManager.getSettingsBool("dark_mode")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun changeTopBarTitle(title: String) {
        main_top_bar_not_title.text = title
    }

    fun changeTopBarInitials(initials: String) {
        main_top_bar_user_initials.text = initials
    }

    private fun changeTopBarNotification(newNotifications: Boolean) {
        main_top_bar_not_new_icon.visibility = if (newNotifications) View.VISIBLE else View.GONE
    }
}