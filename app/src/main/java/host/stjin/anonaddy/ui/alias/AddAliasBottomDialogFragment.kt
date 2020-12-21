package host.stjin.anonaddy.ui.alias

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.SUBSCRIPTIONS
import host.stjin.anonaddy.models.User
import kotlinx.android.synthetic.main.bottomsheet_addalias.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddAliasBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddAliasBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddAliasBottomDialogListener {
        fun onAdded()
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
            R.layout.bottomsheet_addalias, container,
            false
        )
        listener = parentFragment as AddAliasBottomDialogListener

        // Sent the help text username accordingly
        root.bs_addalias_domain_help_textview.text = requireContext().resources.getString(R.string.add_alias_desc, User.userResource.username)

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            fillSpinners(root, requireContext())
        }

        root.bs_addalias_alias_add_alias_button.setOnClickListener(this)
        spinnerChangeListener(root, requireContext())
        return root
    }

    /*
    the custom format is not available for shared domains
     */
    private fun spinnerChangeListener(root: View, context: Context) {
        root.bs_addalias_alias_format_mact.setOnItemClickListener { _, _, _, _ ->
            // Since the alias format changed, check if custom is available
            checkIfCustomIsAvailable(root, context)
            root.bs_addalias_alias_format_til.error = null
        }

        root.bs_addalias_domain_mact.setOnItemClickListener { _, _, _, _ ->
            root.bs_addalias_domain_til.error = null
        }
    }

    private fun checkIfCustomIsAvailable(root: View, context: Context) {
        // If the selected domain format is custom
        if (root.bs_addalias_alias_format_mact.text.toString() == context.resources.getString(R.string.domains_format_custom)) {
            root.bs_addalias_alias_local_part_til.visibility = View.VISIBLE
        } else {
            root.bs_addalias_alias_local_part_til.visibility = View.GONE
        }
    }


    private var DOMAINS: List<String> = listOf()
    private var FORMATS: List<String> = listOf()
    private suspend fun fillSpinners(root: View, context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.getDomainOptions { result ->


            // Set domains and default format/domain
            if (result?.data != null) {
                DOMAINS = result.data

                val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    DOMAINS
                )
                root.bs_addalias_domain_mact.setAdapter(domainAdapter)

                // Set default domain
                if (result.defaultAliasDomain != null) {
                    root.bs_addalias_domain_mact.setText(result.defaultAliasDomain, false)
                }

                // Set default format
                FORMATS = context.resources.getStringArray(R.array.domains_formats_names).toList()
                val FORMATSID = context.resources.getStringArray(R.array.domains_formats).toList()

                val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    FORMATS
                )
                root.bs_addalias_alias_format_mact.setAdapter(formatAdapter)
                // Set default format
                if (result.defaultAliasFormat != null) {
                    root.bs_addalias_alias_format_mact.setText(
                        FORMATS[FORMATSID.indexOf(result.defaultAliasFormat)],
                        false
                    )
                }
            }

            // Since the alias format has been set, check if custom is available
            checkIfCustomIsAvailable(root, context)
        }

    }

    companion object {
        fun newInstance(): AddAliasBottomDialogFragment {
            return AddAliasBottomDialogFragment()
        }
    }

    private fun addAlias(root: View, context: Context) {

        if (!DOMAINS.contains(root.bs_addalias_domain_mact.text.toString())) {
            root.bs_addalias_domain_til.error =
                context.resources.getString(R.string.not_a_valid_domain)
            return
        }

        if (!FORMATS.contains(root.bs_addalias_alias_format_mact.text.toString())) {
            root.bs_addalias_alias_format_til.error =
                context.resources.getString(R.string.not_a_valid_alias_format)
            return
        }

        // If the selected domain format is random words
        if (root.bs_addalias_alias_format_mact.text.toString() == context.resources.getString(R.string.domains_format_random_words)) {
            // If the user has a free subscription
            if (User.userResource.subscription == SUBSCRIPTIONS.FREE.subscription) {
                root.bs_addalias_alias_format_til.error =
                    context.resources.getString(R.string.domains_format_random_words_not_available_for_this_subscription)
                return
            }
        }

        // If the selected domain format is custom
        if (root.bs_addalias_alias_format_mact.text.toString() == context.resources.getString(R.string.domains_format_custom)) {
            // If the selected domain contains a shared domain disable the local part box
            if (context.resources.getStringArray(R.array.shared_domains).contains(root.bs_addalias_domain_mact.text.toString())) {
                root.bs_addalias_alias_format_til.error = context.resources.getString(R.string.domains_format_custom_not_available_for_this_domain)
                return
            }

            if (root.bs_addalias_alias_local_part_tiet.text.toString().isEmpty()) {
                root.bs_addalias_alias_local_part_til.error = context.resources.getString(R.string.this_field_cannot_be_empty)
                return
            }
        }



        // Set error to null if domain and alias is valid
        root.bs_addalias_domain_til.error = null
        root.bs_addalias_alias_format_til.error = null

        root.bs_addalias_alias_add_alias_button.isEnabled = false
        root.bs_addalias_alias_progressbar.visibility = View.VISIBLE
        val domain = root.bs_addalias_domain_mact.text.toString()
        val description = root.bs_addalias_alias_desc_tiet.text.toString()
        val localPart = root.bs_addalias_alias_local_part_tiet.text.toString()
        val format =
            context.resources.getStringArray(R.array.domains_formats)[context.resources.getStringArray(
                R.array.domains_formats_names
            ).indexOf(root.bs_addalias_alias_format_mact.text.toString())]

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addAliasToAccount(root, context, domain, description, format, localPart)
        }
    }

    private suspend fun addAliasToAccount(
        root: View,
        context: Context,
        domain: String,
        description: String,
        format: String,
        local_part: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addAlias(domain, description, format, local_part) { result ->
            if (result == "201") {
                listener.onAdded()
            } else {
                root.bs_addalias_alias_add_alias_button.isEnabled = true
                root.bs_addalias_alias_progressbar.visibility = View.INVISIBLE
                root.bs_addalias_alias_desc_til.error =
                    context.resources.getString(R.string.error_adding_alias) + "\n" + result
            }
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addalias_alias_add_alias_button) {
                addAlias(requireView(), requireContext())
            }
        }
    }
}