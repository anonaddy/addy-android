package host.stjin.anonaddy.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

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

    fun getAllFragments(): ArrayList<Fragment> {
        return fragments // Assuming 'fragmentList' is the list of fragments you use to create the adapter
    }
}