package host.stjin.anonaddy.ui.search

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.SearchAdapter
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.Domains
import host.stjin.anonaddy.models.Recipients
import host.stjin.anonaddy.models.Usernames
import kotlinx.android.synthetic.main.bottomsheet_search.*
import kotlinx.android.synthetic.main.bottomsheet_search.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class SearchBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var listener: AddSearchBottomDialogListener
    private lateinit var networkHelper: NetworkHelper
    private lateinit var settingsManager: SettingsManager

    // 1. Defines the listener interface with a method passing back data result.
    interface AddSearchBottomDialogListener {
        fun onSearch(
            filteredAliases: ArrayList<Aliases>,
            filteredRecipients: ArrayList<Recipients>,
            filteredDomains: ArrayList<Domains>,
            filteredUsernames: ArrayList<Usernames>
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_search, container,
            false
        )

        settingsManager = SettingsManager(true, requireContext())
        listener = activity as AddSearchBottomDialogListener
        networkHelper = NetworkHelper(requireContext())

        root.bs_search_clear_recent.setOnClickListener(this)
        // Setup a callback when the "Done" button is pressed on keyboard
        root.bs_search_term_tiet.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                searchForTerm(root, requireContext())
            }
            false
        }

        getRecentSearchResults(root)

        return root

    }

    private fun getRecentSearchResults(root: View) {
        val recentSearchesSet = settingsManager.getStringSet(SettingsManager.PREFS.RECENT_SEARCHES)

        val recentSearches: ArrayList<String> = ArrayList()
        recentSearchesSet?.let { recentSearches.addAll(it) }

        root.bs_search_recyclerview.apply {

            if (itemDecorationCount > 0) {
                addItemDecoration(
                    DividerItemDecoration(
                        this.context,
                        (layoutManager as LinearLayoutManager).orientation
                    )
                )
            }

            layoutManager = LinearLayoutManager(activity)

            val recipientAdapter = SearchAdapter(recentSearches)
            recipientAdapter.setClickListener(object : SearchAdapter.ClickListener {

                override fun onClickSearchResult(pos: Int, aView: View) {
                    root.bs_search_term_tiet.setText(recentSearches[pos])
                    searchForTerm(root, requireContext())
                }

            })
            adapter = recipientAdapter
            root.bs_search_recyclerview.hideShimmerAdapter()
        }
    }


    companion object {
        fun newInstance(): SearchBottomDialogFragment {
            return SearchBottomDialogFragment()
        }
    }

    private fun searchForTerm(root: View, context: Context) {
        // Set error to null if domain and alias is valid
        root.bs_search_term_til.error = null
        root.bs_search_term_til.isEnabled = false
        root.bs_search_title.text = context.resources.getString(R.string.searching)

        // Add search to recent searches
        val recentSearchesSet = settingsManager.getStringSet(SettingsManager.PREFS.RECENT_SEARCHES)

        val recentSearches: ArrayList<String> = ArrayList()
        recentSearchesSet?.let { recentSearches.addAll(it) }
        // Add search to list
        recentSearches.add(bs_search_term_tiet.text.toString())
        // Grab last 5 and put them back
        settingsManager.putStringSet(SettingsManager.PREFS.RECENT_SEARCHES, recentSearches.takeLast(5).toMutableSet())


        getAndReturnList(root, context)
    }

    var aliases: ArrayList<Aliases>? = null
    var recipients: ArrayList<Recipients>? = null
    var domains: ArrayList<Domains>? = null
    var usernames: ArrayList<Usernames>? = null
    private var sourcesToSearch = 0
    private var sourcesSearched = 0


    private fun getAndReturnList(root: View, context: Context) {
        if (bs_search_chip_aliases.isChecked) {
            sourcesToSearch++

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                networkHelper.getAliases({ aliaslist ->
                    aliases = aliaslist
                    sourcesSearched++
                    performSearch(root, context)
                }, activeOnly = false, includeDeleted = true)
            }
        }

        if (bs_search_chip_recipient.isChecked) {
            sourcesToSearch++

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                networkHelper.getRecipients({ recipientlist ->
                    recipients = recipientlist
                    sourcesSearched++
                    performSearch(root, context)
                }, false)
            }
        }

        if (bs_search_chip_domains.isChecked) {
            sourcesToSearch++

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                networkHelper.getAllDomains { domainlist ->
                    domains = domainlist
                    sourcesSearched++
                    performSearch(root, context)
                }
            }
        }

        if (bs_search_chip_usernames.isChecked) {
            sourcesToSearch++

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                networkHelper.getAllUsernames { usernamelist ->
                    usernames = usernamelist
                    sourcesSearched++
                    performSearch(root, context)
                }
            }
        }
    }


    private fun performSearch(root: View, context: Context) {
        if (sourcesSearched >= sourcesToSearch) {
            val filteredAliases = ArrayList<Aliases>()
            val filteredRecipients = ArrayList<Recipients>()
            val filteredDomains = ArrayList<Domains>()
            val filteredUsernames = ArrayList<Usernames>()

            if (aliases != null) {
                for (alias in aliases!!) {
                    if (alias.email.contains(bs_search_term_tiet.text.toString()) || alias.description?.contains(bs_search_term_tiet.text.toString()) == true) {
                        filteredAliases.add(alias)
                    }
                }
            }

            if (recipients != null) {
                for (recipient in recipients!!) {
                    if (recipient.email.contains(bs_search_term_tiet.text.toString())) {
                        filteredRecipients.add(recipient)
                    }
                }
            }


            if (domains != null) {
                for (domain in domains!!) {
                    if (domain.domain.contains(bs_search_term_tiet.text.toString()) || domain.description?.contains(bs_search_term_tiet.text.toString()) == true) {
                        filteredDomains.add(domain)
                    }
                }
            }


            if (usernames != null) {
                for (username in usernames!!) {
                    if (username.username.contains(bs_search_term_tiet.text.toString()) || username.description?.contains(bs_search_term_tiet.text.toString()) == true) {
                        filteredUsernames.add(username)
                    }
                }
            }

            if (filteredAliases.size == 0 && filteredDomains.size == 0 && filteredRecipients.size == 0 && filteredUsernames.size == 0) {
                root.bs_search_title.text = context.resources.getString(R.string.search)
                root.bs_search_term_til.isEnabled = true
                root.bs_search_term_til.error =
                    context.resources.getString(R.string.nothing_found)
            } else {
                listener.onSearch(filteredAliases, filteredRecipients, filteredDomains, filteredUsernames)
            }
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_search_clear_recent) {
                settingsManager.removeSetting(SettingsManager.PREFS.RECENT_SEARCHES)
                getRecentSearchResults(requireView())
            }
        }
    }
}