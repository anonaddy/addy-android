package host.stjin.anonaddy.ui.search

import android.app.Dialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.SearchAdapter
import host.stjin.anonaddy.databinding.BottomsheetSearchBinding
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.models.Usernames
import kotlinx.coroutines.launch
import java.util.Locale


class SearchBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var listener: AddSearchBottomDialogListener
    private lateinit var networkHelper: NetworkHelper
    private lateinit var encryptedSettingsManager: SettingsManager

    // 1. Defines the listener interface with a method passing back data result.
    interface AddSearchBottomDialogListener {
        fun onSearch(
            filteredAliases: ArrayList<Aliases>,
            filteredRecipients: ArrayList<Recipients>,
            filteredDomains: ArrayList<Domains>,
            filteredUsernames: ArrayList<Usernames>,
            filteredRules: ArrayList<Rules>,
            filteredFailedDeliveries: ArrayList<FailedDeliveries>
        )
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetSearchBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSearchBinding.inflate(inflater, container, false)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        listener = activity as AddSearchBottomDialogListener
        networkHelper = NetworkHelper(requireContext())

        binding.bsSearchClearRecent.setOnClickListener(this)
        // Setup a callback when the "Done" button is pressed on keyboard
        binding.bsSearchTermTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchForTerm(requireContext())
            }
            false
        }

        getRecentSearchResults()

        return root
    }

    private fun View.showKeyboard() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Clear settings
            if (this.requestFocus()) {
                (activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(this, SHOW_IMPLICIT)
            }
        }, 200)
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private var hasSetItemDecoration = false
    private fun getRecentSearchResults() {
        val recentSearchesSet = encryptedSettingsManager.getStringSet(SettingsManager.PREFS.RECENT_SEARCHES)

        val recentSearches: ArrayList<String> = ArrayList()
        recentSearchesSet?.let { recentSearches.addAll(it) }

        binding.bsSearchRecyclerview.apply {

            layoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(context))
            if (!hasSetItemDecoration) {
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                hasSetItemDecoration = true
            }

            val recipientAdapter = SearchAdapter(recentSearches)
            recipientAdapter.setClickListener(object : SearchAdapter.ClickListener {

                override fun onClickSearchResult(pos: Int, aView: View) {
                    binding.bsSearchTermTiet.setText(recentSearches[pos])
                    searchForTerm(requireContext())
                }

            })
            adapter = recipientAdapter
        }
    }


    companion object {
        fun newInstance(): SearchBottomDialogFragment {
            return SearchBottomDialogFragment()
        }
    }

    private fun searchForTerm(context: Context) {
        if (binding.bsSearchTermTiet.text.toString().length >= 3) {
            // Set error to null if domain and alias is valid
            binding.bsSearchTermTil.error = null
            binding.bsSearchTermTil.isEnabled = false
            binding.bsSearchTitle.text = context.resources.getString(R.string.searching)

            // Add search to recent searches
            val recentSearchesSet = encryptedSettingsManager.getStringSet(SettingsManager.PREFS.RECENT_SEARCHES)

            val recentSearches: ArrayList<String> = ArrayList()
            recentSearchesSet?.let { recentSearches.addAll(it) }
            // Add search to list
            recentSearches.add(binding.bsSearchTermTiet.text.toString())
            // Grab last 5 and put them back
            encryptedSettingsManager.putStringSet(SettingsManager.PREFS.RECENT_SEARCHES, recentSearches.takeLast(5).toMutableSet())

            getAndReturnList(context)
        } else {
            binding.bsSearchTitle.text = context.resources.getString(R.string.search)
            binding.bsSearchTermTil.isEnabled = true
            binding.bsSearchTermTil.error =
                context.resources.getString(R.string.search_min_3_char_required)
            binding.bsSearchTermTiet.showKeyboard()
        }
    }

    var aliases: ArrayList<Aliases>? = null
    var recipients: ArrayList<Recipients>? = null
    var domains: ArrayList<Domains>? = null
    var usernames: ArrayList<Usernames>? = null
    var rules: ArrayList<Rules>? = null
    private var failedDeliveries: ArrayList<FailedDeliveries>? = null
    private var sourcesToSearch = 0
    private var sourcesSearched = 0


    private fun getAndReturnList(context: Context) {
        if (binding.bsSearchChipAliases.isChecked) {
            sourcesToSearch++

            viewLifecycleOwner.lifecycleScope.launch {
                networkHelper.getAliases(
                    { aliaslist, _ ->
                        aliases = aliaslist?.data
                        sourcesSearched++
                        performSearch(context)
                    },
                    aliasSortFilter = AliasSortFilter(
                        onlyActiveAliases = false,
                        onlyDeletedAliases = false,
                        onlyInactiveAliases = false,
                        onlyWatchedAliases = false,
                        sort = null,
                        sortDesc = true,
                        filter = binding.bsSearchTermTiet.text.toString().lowercase(Locale.getDefault())
                    ),
                    // This will return max 100 items
                    size = 100
                )
            }
        }

        if (binding.bsSearchChipRecipient.isChecked) {
            sourcesToSearch++

            viewLifecycleOwner.lifecycleScope.launch {
                networkHelper.getRecipients({ recipientlist, _ ->
                    recipients = recipientlist
                    sourcesSearched++
                    performSearch(context)
                }, false)
            }
        }

        if (binding.bsSearchChipDomains.isChecked) {
            sourcesToSearch++

            viewLifecycleOwner.lifecycleScope.launch {
                networkHelper.getAllDomains { domainlist, _ ->
                    domains = domainlist
                    sourcesSearched++
                    performSearch(context)
                }
            }
        }

        if (binding.bsSearchChipUsernames.isChecked) {
            sourcesToSearch++

            viewLifecycleOwner.lifecycleScope.launch {
                networkHelper.getAllUsernames { usernamelist, _ ->
                    usernames = usernamelist
                    sourcesSearched++
                    performSearch(context)
                }
            }
        }


        if (binding.bsSearchChipRules.isChecked) {
            sourcesToSearch++

            viewLifecycleOwner.lifecycleScope.launch {
                networkHelper.getAllRules({ rulesList, _ ->
                    rules = rulesList
                    sourcesSearched++
                    performSearch(context)
                })
            }
        }

        if (binding.bsSearchChipFailedDeliveries.isChecked) {
            sourcesToSearch++

            viewLifecycleOwner.lifecycleScope.launch {
                networkHelper.getAllFailedDeliveries { failedDeliveriesList, _ ->
                    failedDeliveries = failedDeliveriesList
                    sourcesSearched++
                    performSearch(context)
                }
            }
        }
    }


    private fun performSearch(context: Context) {
        if (sourcesSearched >= sourcesToSearch) {
            val filteredAliases = ArrayList<Aliases>()
            val filteredRecipients = ArrayList<Recipients>()
            val filteredDomains = ArrayList<Domains>()
            val filteredUsernames = ArrayList<Usernames>()
            val filteredRules = ArrayList<Rules>()
            val filteredFailedDeliveries = ArrayList<FailedDeliveries>()

            if (aliases != null) {
                for (alias in aliases!!) {
                    // Searching for aliases is being done in the API now, no need to do it again
                    filteredAliases.add(alias)
                }
            }

            if (recipients != null) {
                for (recipient in recipients!!) {
                    if (
                        recipient.email.lowercase(Locale.ROOT).contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT))) {
                        filteredRecipients.add(recipient)
                    }
                }
            }


            if (domains != null) {
                for (domain in domains!!) {
                    if (
                        domain.domain.lowercase(Locale.ROOT).contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT)) ||
                        domain.description?.lowercase(Locale.ROOT)
                            ?.contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT)) == true
                    ) {
                        filteredDomains.add(domain)
                    }
                }
            }


            if (usernames != null) {
                for (username in usernames!!) {
                    if (
                        username.username.lowercase(Locale.ROOT).contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT)) ||
                        username.description?.lowercase(Locale.ROOT)
                            ?.contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT)) == true
                    ) {
                        filteredUsernames.add(username)
                    }
                }
            }

            if (rules != null) {
                for (rule in rules!!) {
                    if (
                        rule.name.lowercase(Locale.ROOT).contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT))) {
                        filteredRules.add(rule)
                    }
                }
            }

            if (failedDeliveries != null) {
                for (failedDelivery in failedDeliveries!!) {
                    if (
                        failedDelivery.alias_email?.lowercase(Locale.ROOT)
                            ?.contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT)) == true ||
                        failedDelivery.recipient_email?.lowercase(Locale.ROOT)
                            ?.contains(binding.bsSearchTermTiet.text.toString().lowercase(Locale.ROOT)) == true
                    ) {
                        filteredFailedDeliveries.add(failedDelivery)
                    }
                }
            }

            if (filteredAliases.isEmpty &&
                filteredDomains.isEmpty &&
                filteredRecipients.isEmpty &&
                filteredUsernames.isEmpty &&
                filteredRules.isEmpty &&
                filteredFailedDeliveries.isEmpty
            ) {
                binding.bsSearchTitle.text = context.resources.getString(R.string.search)
                binding.bsSearchTermTil.isEnabled = true
                binding.bsSearchTermTil.error =
                    context.resources.getString(R.string.nothing_found)
                binding.bsSearchTermTiet.showKeyboard()
            } else {
                listener.onSearch(filteredAliases, filteredRecipients, filteredDomains, filteredUsernames, filteredRules, filteredFailedDeliveries)
            }
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_search_clear_recent) {
                encryptedSettingsManager.removeSetting(SettingsManager.PREFS.RECENT_SEARCHES)
                getRecentSearchResults()
            }
        }
    }

    override fun dismiss() {
        super.dismiss()
        binding.bsSearchTermTiet.hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}