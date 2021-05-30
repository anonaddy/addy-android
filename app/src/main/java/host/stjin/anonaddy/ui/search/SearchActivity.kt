package host.stjin.anonaddy.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.*
import host.stjin.anonaddy.databinding.ActivitySearchBinding
import host.stjin.anonaddy.models.*
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientsActivity
import host.stjin.anonaddy.ui.rules.CreateRuleActivity
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredAliases
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredDomains
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredRecipients
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredRules
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredUsernames
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernamesActivity
import host.stjin.anonaddy.utils.MarginItemDecoration

class SearchActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true


    enum class SearchTargets(val activity: String) {
        ALIASES("aliases"),
        RECIPIENTS("recipients"),
        DOMAINS("domains"),
        USERNAMES("usernames"),
        RULES("rules")
    }

    object FilteredLists {
        var filteredAliases: ArrayList<Aliases>? = null
        var filteredRecipients: ArrayList<Recipients>? = null
        var filteredDomains: ArrayList<Domains>? = null
        var filteredUsernames: ArrayList<Usernames>? = null
        var filteredRules: ArrayList<Rules>? = null
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
    }

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupToolbar(binding.activitySearchToolbar.customToolbarOneHandedMaterialtoolbar, R.string.search_result)

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        setSearchResults()
    }

    private fun setSearchResults() {
        binding.activitySearchRLLottieview.visibility = View.GONE


        if (filteredAliases?.size ?: 0 > 0) {
            binding.activitySearchAliasesLL.visibility = View.VISIBLE
            setAliases()
        }

        if (filteredDomains?.size ?: 0 > 0) {
            binding.activitySearchDomainsLL.visibility = View.VISIBLE
            setDomains()
        }

        if (filteredRecipients?.size ?: 0 > 0) {
            binding.activitySearchRecipientsLL.visibility = View.VISIBLE
            setRecipients()
        }

        if (filteredUsernames?.size ?: 0 > 0) {
            binding.activitySearchUsernamesLL.visibility = View.VISIBLE
            setUsernames()
        }

        if (filteredRules?.size ?: 0 > 0) {
            binding.activitySearchRulesLL.visibility = View.VISIBLE
            setRules()
        }

        if (filteredAliases?.size ?: 0 == 0 && filteredDomains?.size ?: 0 == 0 && filteredRecipients?.size ?: 0 == 0 && filteredUsernames?.size ?: 0 == 0 && filteredRules?.size ?: 0 == 0) {
            binding.activitySearchRLLottieview.visibility = View.VISIBLE
        }
    }

    private fun setUsernames() {
        binding.activitySearchUsernamesRecyclerview.apply {

            layoutManager = if (this@SearchActivity.resources.getBoolean(R.bool.isTablet)) {
                // set a GridLayoutManager for tablets
                GridLayoutManager(this@SearchActivity, 2)
            } else {
                LinearLayoutManager(this@SearchActivity)
            }
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))


            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                binding.activitySearchUsernamesRecyclerview.layoutAnimation = animation
            }

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
            binding.activitySearchUsernamesRecyclerview.hideShimmerAdapter()
        }

    }


    private fun setRules() {
        binding.activitySearchRulesRecyclerview.apply {

            layoutManager = if (this@SearchActivity.resources.getBoolean(R.bool.isTablet)) {
                // set a GridLayoutManager for tablets
                GridLayoutManager(this@SearchActivity, 2)
            } else {
                LinearLayoutManager(this@SearchActivity)
            }
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))


            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                binding.activitySearchRulesRecyclerview.layoutAnimation = animation
            }

            val rulesAdapter = RulesAdapter(filteredRules!!, false)
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
            binding.activitySearchRulesRecyclerview.hideShimmerAdapter()
        }

    }


    private fun setAliases() {
        binding.activitySearchAliasesRecyclerview.apply {

            layoutManager = if (this@SearchActivity.resources.getBoolean(R.bool.isTablet)) {
                // set a GridLayoutManager for tablets
                GridLayoutManager(this@SearchActivity, 2)
            } else {
                LinearLayoutManager(this@SearchActivity)
            }
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))


            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                binding.activitySearchAliasesRecyclerview.layoutAnimation = animation
            }


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
            aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
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

                    Snackbar.make(
                        binding.activitySearchCL,
                        context.resources.getString(R.string.copied_alias),
                        Snackbar.LENGTH_SHORT
                    ).show()

                }

            })
            adapter = aliasAdapter
            binding.activitySearchAliasesRecyclerview.hideShimmerAdapter()
        }

    }


    private fun setRecipients() {
        binding.activitySearchRecipientsRecyclerview.apply {

            layoutManager = if (this@SearchActivity.resources.getBoolean(R.bool.isTablet)) {
                // set a GridLayoutManager for tablets
                GridLayoutManager(this@SearchActivity, 2)
            } else {
                LinearLayoutManager(this@SearchActivity)
            }
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))


            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                binding.activitySearchRecipientsRecyclerview.layoutAnimation = animation
            }

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
            binding.activitySearchRecipientsRecyclerview.hideShimmerAdapter()
        }

    }


    private fun setDomains() {
        binding.activitySearchDomainsRecyclerview.apply {

            layoutManager = if (this@SearchActivity.resources.getBoolean(R.bool.isTablet)) {
                // set a GridLayoutManager for tablets
                GridLayoutManager(this@SearchActivity, 2)
            } else {
                LinearLayoutManager(this@SearchActivity)
            }
            addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))


            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                binding.activitySearchDomainsRecyclerview.layoutAnimation = animation
            }

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
            binding.activitySearchDomainsRecyclerview.hideShimmerAdapter()
        }
    }


}