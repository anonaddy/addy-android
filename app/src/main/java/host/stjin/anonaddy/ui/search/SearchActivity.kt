package host.stjin.anonaddy.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.adapter.FailedDeliveryAdapter
import host.stjin.anonaddy.adapter.RecipientAdapter
import host.stjin.anonaddy.adapter.RulesAdapter
import host.stjin.anonaddy.adapter.UsernameAdapter
import host.stjin.anonaddy.databinding.ActivitySearchBinding
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveryDetailsBottomDialogFragment
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientsActivity
import host.stjin.anonaddy.ui.rules.CreateRuleActivity
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredAliases
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredDomains
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredFailedDeliveries
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredRecipients
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredRules
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredUsernames
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernamesActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.models.Usernames

class SearchActivity : BaseActivity(), FailedDeliveryDetailsBottomDialogFragment.AddFailedDeliveryBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null


    enum class SearchTargets(val activity: String) {
        ALIASES("aliases"),
        RECIPIENTS("recipients"),
        DOMAINS("domains"),
        USERNAMES("usernames"),
        RULES("rules"),
        FAILED_DELIVERIES("failed_deliveries")
    }

    object FilteredLists {
        var filteredAliases: ArrayList<Aliases>? = null
        var filteredRecipients: ArrayList<Recipients>? = null
        var filteredDomains: ArrayList<Domains>? = null
        var filteredUsernames: ArrayList<Usernames>? = null
        var filteredRules: ArrayList<Rules>? = null
        var filteredFailedDeliveries: ArrayList<FailedDeliveries>? = null
    }

    // TODO Get these lists through bundles?
    // Clear lists from memory when search is finished
    override fun onDestroy() {
        super.onDestroy()
        filteredAliases = null
        filteredRecipients = null
        filteredDomains = null
        filteredUsernames = null
        filteredRules = null
        filteredFailedDeliveries = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val filteredAliasesJson = gson.toJson(filteredAliases)
        outState.putString("filteredAliases", filteredAliasesJson)

        val filteredRecipientsJson = gson.toJson(filteredRecipients)
        outState.putString("filteredRecipients", filteredRecipientsJson)

        val filteredDomainsJson = gson.toJson(filteredDomains)
        outState.putString("filteredDomains", filteredDomainsJson)

        val filteredUsernamesJson = gson.toJson(filteredUsernames)
        outState.putString("filteredUsernames", filteredUsernamesJson)

        val filteredRulesJson = gson.toJson(filteredRules)
        outState.putString("filteredRules", filteredRulesJson)

        val filteredFailedDeliveriesJson = gson.toJson(filteredFailedDeliveries)
        outState.putString("filteredFailedDeliveries", filteredFailedDeliveriesJson)
    }

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        setupToolbar(
            R.string.search_result,
            binding.activitySearchNSV,
            binding.activitySearchToolbar,
            R.drawable.ic_search
        )

        encryptedSettingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)


        // In the event of a screen resize or orientation change, the filtered items will be stored in the
        // savedInstanceState, load them back when recreating the activity.
        if (savedInstanceState != null) {
            getFilteredResultsFromSavedInstance(savedInstanceState)
        }

        setSearchResults()
    }

    private fun getFilteredResultsFromSavedInstance(savedInstanceState: Bundle) {
        val filteredAliasesJson = savedInstanceState.getString("filteredAliases")
        if (!filteredAliasesJson.isNullOrEmpty() && filteredAliasesJson != "null") {
            val gson = Gson()
            val myType = object : TypeToken<ArrayList<Aliases>>() {}.type
            val filteredAliasesFromSavedInstance = gson.fromJson<ArrayList<Aliases>>(filteredAliasesJson, myType)
            filteredAliases = filteredAliasesFromSavedInstance
        }

        val filteredRecipientsJson = savedInstanceState.getString("filteredRecipients")
        if (!filteredRecipientsJson.isNullOrEmpty() && filteredRecipientsJson != "null") {
            val gson = Gson()
            val myType = object : TypeToken<ArrayList<Recipients>>() {}.type
            val filteredRecipientsFromSavedInstance = gson.fromJson<ArrayList<Recipients>>(filteredRecipientsJson, myType)
            filteredRecipients = filteredRecipientsFromSavedInstance
        }

        val filteredDomainsJson = savedInstanceState.getString("filteredDomains")
        if (!filteredDomainsJson.isNullOrEmpty() && filteredDomainsJson != "null") {
            val gson = Gson()
            val myType = object : TypeToken<ArrayList<Domains>>() {}.type
            val filteredDomainsFromSavedInstance = gson.fromJson<ArrayList<Domains>>(filteredDomainsJson, myType)
            filteredDomains = filteredDomainsFromSavedInstance
        }

        val filteredUsernamesJson = savedInstanceState.getString("filteredUsernames")
        if (!filteredUsernamesJson.isNullOrEmpty() && filteredUsernamesJson != "null") {
            val gson = Gson()
            val myType = object : TypeToken<ArrayList<Usernames>>() {}.type
            val filteredUsernamesFromSavedInstance = gson.fromJson<ArrayList<Usernames>>(filteredUsernamesJson, myType)
            filteredUsernames = filteredUsernamesFromSavedInstance
        }

        val filteredRulesJson = savedInstanceState.getString("filteredRules")
        if (!filteredRulesJson.isNullOrEmpty() && filteredRulesJson != "null") {
            val gson = Gson()
            val myType = object : TypeToken<ArrayList<Rules>>() {}.type
            val filteredRulesFromSavedInstance = gson.fromJson<ArrayList<Rules>>(filteredRulesJson, myType)
            filteredRules = filteredRulesFromSavedInstance
        }

        val filteredFailedDeliveriesJson = savedInstanceState.getString("filteredFailedDeliveries")
        if (!filteredFailedDeliveriesJson.isNullOrEmpty() && filteredFailedDeliveriesJson != "null") {
            val gson = Gson()
            val myType = object : TypeToken<ArrayList<FailedDeliveries>>() {}.type
            val filteredFailedDeliveriesFromSavedInstance = gson.fromJson<ArrayList<FailedDeliveries>>(filteredFailedDeliveriesJson, myType)
            filteredFailedDeliveries = filteredFailedDeliveriesFromSavedInstance
        }

    }

    private fun setSearchResults() {
        if ((filteredAliases?.size ?: 0) > 0) {
            binding.activitySearchAliasesLL.visibility = View.VISIBLE
            setAliases()
        }

        if ((filteredDomains?.size ?: 0) > 0) {
            binding.activitySearchDomainsLL.visibility = View.VISIBLE
            setDomains()
        }

        if ((filteredRecipients?.size ?: 0) > 0) {
            binding.activitySearchRecipientsLL.visibility = View.VISIBLE
            setRecipients()
        }

        if ((filteredUsernames?.size ?: 0) > 0) {
            binding.activitySearchUsernamesLL.visibility = View.VISIBLE
            setUsernames()
        }

        if ((filteredRules?.size ?: 0) > 0) {
            binding.activitySearchRulesLL.visibility = View.VISIBLE
            setRules()
        }

        if ((filteredFailedDeliveries?.size ?: 0) > 0) {
            binding.activitySearchFailedDeliveriesLL.visibility = View.VISIBLE
            setFailedDeliveries()
        }

        // No need to check if there are results, this is being done in the bottomsheet.
        /*        if (filteredAliases?.size ?: 0 == 0 &&
                    filteredDomains?.size ?: 0 == 0 &&
                    filteredRecipients?.size ?: 0 == 0 &&
                    filteredUsernames?.size ?: 0 == 0 &&
                    filteredRules?.size ?: 0 == 0 &&
                    filteredFailedDeliveries?.size ?: 0 == 0
                ) {
                }*/
    }

    /*
    No shimmer, data is already served on opening this activity
     */
    private fun setUsernames() {
        binding.activitySearchUsernamesRecyclerview.apply {

            layoutManager = GridLayoutManager(this@SearchActivity, ScreenSizeUtils.calculateNoOfColumns(context))


            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            val usernamesAdapter = UsernameAdapter(filteredUsernames!!)
            usernamesAdapter.setClickListener(object : UsernameAdapter.ClickListener {

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, ManageUsernamesActivity::class.java)
                    intent.putExtra("username_id", filteredUsernames!![pos].id)
                    startActivity(intent)
                }

                override fun onClickDelete(pos: Int, aView: View) {
                    val data = Intent()
                    data.putExtra("target", SearchTargets.USERNAMES.activity)
                    setResult(RESULT_OK, data)
                    finish()
                }

            })
            adapter = usernamesAdapter
        }

    }


    private fun setRules() {
        binding.activitySearchRulesRecyclerview.apply {

            layoutManager = GridLayoutManager(this@SearchActivity, ScreenSizeUtils.calculateNoOfColumns(context))
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            val rulesAdapter = RulesAdapter(filteredRules!!, null, false)
            rulesAdapter.setClickListener(object : RulesAdapter.ClickListener {
                override fun onClickActivate(pos: Int, aView: View) {
                    val data = Intent()
                    data.putExtra("target", SearchTargets.RULES.activity)
                    setResult(RESULT_OK, data)
                    finish()
                }

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, CreateRuleActivity::class.java)
                    intent.putExtra("rule_id", filteredRules!![pos].id)
                    startActivity(intent)
                }

                override fun onClickDelete(pos: Int, aView: View) {
                    val data = Intent()
                    data.putExtra("target", SearchTargets.RULES.activity)
                    setResult(RESULT_OK, data)
                    finish()
                }

                override fun onItemMove(fromPosition: Int, toPosition: Int) {
                    // Not used
                }

                override fun startDragging(viewHolder: RecyclerView.ViewHolder?) {
                    // Not used
                }

            })
            adapter = rulesAdapter
        }

    }


    private var failedDeliveryDetailsBottomDialogFragment: FailedDeliveryDetailsBottomDialogFragment? = null
    private fun setFailedDeliveries() {
        binding.activitySearchFailedDeliveriesRecyclerview.apply {

            layoutManager = GridLayoutManager(this@SearchActivity, ScreenSizeUtils.calculateNoOfColumns(context))
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            val failedDeliveryAdapter = FailedDeliveryAdapter(filteredFailedDeliveries!!)
            failedDeliveryAdapter.setClickListener(object : FailedDeliveryAdapter.ClickListener {
                override fun onClickDetails(pos: Int, aView: View) {
                    failedDeliveryDetailsBottomDialogFragment = FailedDeliveryDetailsBottomDialogFragment(
                        filteredFailedDeliveries!![pos].id,
                        filteredFailedDeliveries!![pos].created_at,
                        filteredFailedDeliveries!![pos].attempted_at,
                        filteredFailedDeliveries!![pos].alias_email,
                        filteredFailedDeliveries!![pos].recipient_email,
                        filteredFailedDeliveries!![pos].bounce_type,
                        filteredFailedDeliveries!![pos].remote_mta,
                        filteredFailedDeliveries!![pos].sender,
                        filteredFailedDeliveries!![pos].code
                    )
                    failedDeliveryDetailsBottomDialogFragment!!.show(
                        supportFragmentManager,
                        "failedDeliveryDetailsBottomDialogFragment"
                    )
                }

            })
            adapter = failedDeliveryAdapter
        }

    }


    private fun setAliases() {
        binding.activitySearchAliasesRecyclerview.apply {

            layoutManager = GridLayoutManager(this@SearchActivity, ScreenSizeUtils.calculateNoOfColumns(context))

            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            /**
             * Seperate the deleted and non-deleted aliases
             */


            val nonDeletedList: ArrayList<Aliases> = arrayListOf()
            val onlyDeletedList: ArrayList<Aliases> = arrayListOf()

            for (alias in filteredAliases!!) {
                if (alias.deleted_at == null) {
                    nonDeletedList.add(alias)
                } else {
                    onlyDeletedList.add(alias)
                }
            }

            val finalList = (nonDeletedList + onlyDeletedList)
            val aliasAdapter = AliasAdapter(finalList, context)
            aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.AliasInterface {
                override fun onClick(pos: Int) {
                    val intent = Intent(context, ManageAliasActivity::class.java)
                    // Pass data object in the bundle and populate details activity.
                    intent.putExtra("alias_id", finalList[pos].id)
                    startActivity(intent)
                }

                override fun onClickCopy(pos: Int, aView: View) {
                    val clipboard: ClipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val aliasEmailAddress = finalList[pos].email
                    val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                    clipboard.setPrimaryClip(clip)
                    SnackbarHelper.createSnackbar(context, context.resources.getString(R.string.copied_alias), binding.activitySearchCL).show()
                }
            })
            adapter = aliasAdapter
        }

    }


    private fun setRecipients() {
        binding.activitySearchRecipientsRecyclerview.apply {

            layoutManager = GridLayoutManager(this@SearchActivity, ScreenSizeUtils.calculateNoOfColumns(context))
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            val recipientAdapter = RecipientAdapter(filteredRecipients!!)
            recipientAdapter.setClickListener(object : RecipientAdapter.ClickListener {

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, ManageRecipientsActivity::class.java)
                    intent.putExtra("recipient_id", filteredRecipients!![pos].id)
                    intent.putExtra("recipient_email", filteredRecipients!![pos].email)
                    startActivity(intent)
                }

                override fun onClickResend(pos: Int, aView: View) {
                    val data = Intent()
                    data.putExtra("target", SearchTargets.RECIPIENTS.activity)
                    setResult(RESULT_OK, data)
                    finish()
                }

                override fun onClickDelete(pos: Int, aView: View) {
                    val data = Intent()
                    data.putExtra("target", SearchTargets.RECIPIENTS.activity)
                    setResult(RESULT_OK, data)
                    finish()
                }

            })
            adapter = recipientAdapter
        }

    }


    private fun setDomains() {
        binding.activitySearchDomainsRecyclerview.apply {

            layoutManager = GridLayoutManager(this@SearchActivity, ScreenSizeUtils.calculateNoOfColumns(context))
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

            val domainsAdapter = DomainAdapter(filteredDomains!!)
            domainsAdapter.setClickListener(object : DomainAdapter.ClickListener {

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, ManageDomainsActivity::class.java)
                    intent.putExtra("domain_id", filteredDomains!![pos].id)
                    startActivity(intent)
                }


                override fun onClickDelete(pos: Int, aView: View) {
                    val data = Intent()
                    data.putExtra("target", SearchTargets.DOMAINS.activity)
                    setResult(RESULT_OK, data)
                    finish()
                }

            })
            adapter = domainsAdapter
        }
    }

    override fun onDeleted(failedDeliveryId: String) {
        val position = filteredFailedDeliveries?.indexOfFirst { it.id == failedDeliveryId }
        position?.let { filteredFailedDeliveries?.removeAt(it) }
        position?.let { binding.activitySearchFailedDeliveriesRecyclerview.adapter?.notifyItemRemoved(it) }
        failedDeliveryDetailsBottomDialogFragment?.dismissAllowingStateLoss()
    }


}