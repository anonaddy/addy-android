package host.stjin.anonaddy.ui.alias

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
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

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            fillSpinners(root, requireContext())
        }
        // 2. Setup a callback when the "Done" button is pressed on keyboard
        root.bs_addalias_alias_add_alias_button.setOnClickListener(this)
        root.bs_addalias_alias_desc_tiet.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addAlias(root, requireContext())
            }
            false
        }

        return root

    }

    private suspend fun fillSpinners(root: View, context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.getDomainOptions { result ->


            // Set domains and default format/domain
            if (result?.data != null) {
                val DOMAINS = result.data

                val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    DOMAINS
                )
                root.bs_addalias_domain_mact.setAdapter(domainAdapter)

                // Set default domain
                //root.bs_addalias_domain_mact.setText(result.defaultAliasDomain)

                // Set default format
                val FORMATS = listOf(
                    context.resources.getString(R.string.domains_format_uuid),
                    context.resources.getString(R.string.domains_format_random_words)
                )

                val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    FORMATS
                )
                root.bs_addalias_alias_format_mact.setAdapter(formatAdapter)
                // Set default format
                //root.bs_addalias_alias_format_mact.setText(result.defaultAliasFormat)
            }
        }

    }

    companion object {
        fun newInstance(): AddAliasBottomDialogFragment {
            return AddAliasBottomDialogFragment()
        }
    }

    private fun addAlias(root: View, context: Context) {
        root.bs_addalias_alias_add_alias_button.isEnabled = false
        root.bs_addalias_alias_progressbar.visibility = View.VISIBLE
        val domain = root.bs_addalias_domain_mact.text.toString()
        val description = root.bs_addalias_alias_desc_tiet.text.toString()
        val format = root.bs_addalias_alias_format_mact.text.toString()

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addAliasToAccount(root, context, domain, description, format)
        }
    }

    private suspend fun addAliasToAccount(
        root: View,
        context: Context,
        domain: String,
        description: String,
        format: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addAlias(domain, description, format) { result ->
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