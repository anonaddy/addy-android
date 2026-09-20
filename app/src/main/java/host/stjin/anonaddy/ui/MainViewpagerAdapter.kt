package host.stjin.anonaddy.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import host.stjin.anonaddy.ui.aliases.AliasesFragment
import host.stjin.anonaddy.ui.blocklist.BlocklistFragment
import host.stjin.anonaddy.ui.domains.DomainsFragment
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesFragment
import host.stjin.anonaddy.ui.home.HomeFragment
import host.stjin.anonaddy.ui.recipients.RecipientsFragment
import host.stjin.anonaddy.ui.rules.RulesFragment
import host.stjin.anonaddy.ui.usernames.UsernamesFragment

class MainViewpagerAdapter(
    fa: FragmentActivity,
    private val isTablet: Boolean
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = if (isTablet) 8 else 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment.newInstance()
            1 -> AliasesFragment.newInstance()
            2 -> RecipientsFragment.newInstance()
            3 -> UsernamesFragment.newInstance()
            4 -> DomainsFragment.newInstance()
            5 -> RulesFragment.newInstance()
            6 -> BlocklistFragment.newInstance()
            7 -> FailedDeliveriesFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}
