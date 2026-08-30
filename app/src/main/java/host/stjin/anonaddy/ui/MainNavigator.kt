package host.stjin.anonaddy.ui

import android.content.Intent
import androidx.viewpager2.widget.ViewPager2
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.domains.DomainsActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy.ui.rules.RulesActivity
import host.stjin.anonaddy.ui.usernames.UsernamesActivity

class MainNavigator(private val activity: MainActivity) {

    private val isTablet: Boolean
        get() = activity.resources.getBoolean(R.bool.isTablet)

    private val viewPager: ViewPager2
        get() = activity.viewPager

    fun navigateTo(itemId: Int) {
        when (itemId) {
            R.id.navigation_home -> viewPager.currentItem = 0
            R.id.navigation_alias -> viewPager.currentItem = 1
            R.id.navigation_recipients -> viewPager.currentItem = 2
            R.id.navigation_usernames -> {
                if (isTablet) {
                    viewPager.currentItem = 3
                } else {
                    activity.startActivity(Intent(activity, UsernamesActivity::class.java))
                }
            }
            R.id.navigation_domains -> {
                if (isTablet) {
                    viewPager.currentItem = 4
                } else {
                    activity.startActivity(Intent(activity, DomainsActivity::class.java))
                }
            }
            R.id.navigation_rules -> {
                if (isTablet) {
                    viewPager.currentItem = 5
                } else {
                    activity.startActivity(Intent(activity, RulesActivity::class.java))
                }
            }
            R.id.navigation_failed_deliveries -> {
                if (isTablet) {
                    viewPager.currentItem = 7
                } else {
                    activity.startActivity(Intent(activity, FailedDeliveriesActivity::class.java))
                }
            }
        }
    }
}
