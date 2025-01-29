package host.stjin.anonaddy.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import host.stjin.anonaddy.ui.alias.AliasFragment
import host.stjin.anonaddy.ui.domains.DomainSettingsFragment
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesFragment
import host.stjin.anonaddy.ui.home.HomeFragment
import host.stjin.anonaddy.ui.recipients.RecipientsFragment
import host.stjin.anonaddy.ui.rules.RulesSettingsFragment
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsFragment

class MainViewpagerAdapter(fa: FragmentActivity, private val fragments: ArrayList<Fragment>) : FragmentStateAdapter(fa) {

    // Map to keep track of tags and their corresponding fragment indices
    private val tagToIndex = mutableMapOf<String, Int>()

    init {
        fragments.forEachIndexed { index, fragment ->
            val tag = fragment.javaClass.simpleName
            tagToIndex[tag] = index
        }
    }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    // Method to get fragment by tag
    fun getFragmentByTag(tag: String): Fragment? {
        val index = tagToIndex[tag]
        return if (index != null && index < fragments.size) {
            fragments[index]
        } else {
            null
        }
    }

    // Method to get position by tag
    fun getPositionByTag(tag: String): Int? = tagToIndex[tag]
}