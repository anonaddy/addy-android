package host.stjin.anonaddy.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.adapter.RecipientAdapter
import host.stjin.anonaddy.adapter.UsernameAdapter
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.Domains
import host.stjin.anonaddy.models.Recipients
import host.stjin.anonaddy.models.Usernames
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.domains.manage.ManageDomainsActivity
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientsActivity
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredAliases
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredDomains
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredRecipients
import host.stjin.anonaddy.ui.search.SearchActivity.FilteredLists.filteredUsernames
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernamesActivity
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true


    enum class SearchTargets(val activity: String) {
        ALIASES("aliases"),
        RECIPIENTS("recipients"),
        DOMAINS("domains"),
        USERNAMES("usernames")
    }

    object FilteredLists {
        var filteredAliases: ArrayList<Aliases>? = null
        var filteredRecipients: ArrayList<Recipients>? = null
        var filteredDomains: ArrayList<Domains>? = null
        var filteredUsernames: ArrayList<Usernames>? = null
    }

    // TODO Get these lists through bundles?
    // Clear lists from memory when search is finished
    override fun onDestroy() {
        super.onDestroy()
        filteredAliases = null
        filteredRecipients = null
        filteredDomains = null
        filteredUsernames = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setupToolbar(activity_search_toolbar)

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        getDataFromWeb()
    }

    private fun getDataFromWeb() {

        if (filteredAliases?.size ?: 0 > 0) {
            activity_search_aliases_LL.visibility = View.VISIBLE
            setAliases()
        }

        if (filteredDomains?.size ?: 0 > 0) {
            activity_search_domains_LL.visibility = View.VISIBLE
            setDomains()
        }

        if (filteredRecipients?.size ?: 0 > 0) {
            activity_search_recipients_LL.visibility = View.VISIBLE
            setRecipients()
        }

        if (filteredUsernames?.size ?: 0 > 0) {
            activity_search_usernames_LL.visibility = View.VISIBLE
            setUsernames()
        }

        if (filteredAliases?.size ?: 0 == 0 && filteredDomains?.size ?: 0 == 0 && filteredRecipients?.size ?: 0 == 0 && filteredUsernames?.size ?: 0 == 0) {
            activity_search_RL_lottieview.visibility = View.VISIBLE
        }


    }

    private fun setUsernames() {
        activity_search_usernames_recyclerview.apply {

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
                activity_search_usernames_recyclerview.layoutAnimation = animation
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
            activity_search_usernames_recyclerview.hideShimmerAdapter()
        }

    }


    private fun setAliases() {
        activity_search_aliases_recyclerview.apply {
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
                activity_search_aliases_recyclerview.layoutAnimation = animation
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

            val finalList = nonDeletedList + onlyDeletedList
            val aliasAdapter = AliasAdapter(finalList, true)
            aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                override fun onClick(pos: Int, aView: View) {
                    val intent = Intent(context, ManageAliasActivity::class.java)
                    // Pass data object in the bundle and populate details activity.
                    intent.putExtra("alias_id", finalList[pos].id)
                    intent.putExtra("alias_forward_count", finalList[pos].emails_forwarded)
                    intent.putExtra("alias_replied_sent_count", finalList[pos].emails_replied)

                    val options: ActivityOptionsCompat =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this@SearchActivity,
                            aView,
                            finalList[pos].id
                        )

                    startActivity(intent, options.toBundle())
                }

                override fun onClickCopy(pos: Int, aView: View) {
                    val clipboard: ClipboardManager? =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val aliasEmailAddress = finalList[pos].email
                    val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                    clipboard?.setPrimaryClip(clip)

                    Snackbar.make(
                        activity_search_LL,
                        context.resources.getString(R.string.copied_alias),
                        Snackbar.LENGTH_SHORT
                    ).show()

                }

            })
            adapter = aliasAdapter
            activity_search_aliases_recyclerview.hideShimmerAdapter()
        }

    }


    private fun setRecipients() {
        activity_search_recipients_recyclerview.apply {

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
                activity_search_recipients_recyclerview.layoutAnimation = animation
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
            activity_search_recipients_recyclerview.hideShimmerAdapter()
        }

    }


    private fun setDomains() {
        activity_search_domains_recyclerview.apply {

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
                activity_search_domains_recyclerview.layoutAnimation = animation
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
            activity_search_domains_recyclerview.hideShimmerAdapter()
        }
    }


}