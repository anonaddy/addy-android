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
import host.stjin.anonaddy.databinding.BottomsheetAddaliasBinding
import host.stjin.anonaddy.models.SUBSCRIPTIONS
import host.stjin.anonaddy.models.User
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


    private var _binding: BottomsheetAddaliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAddaliasBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = parentFragment as AddAliasBottomDialogListener

        // Sent the help text username accordingly
        binding.bsAddaliasDomainHelpTextview.text = requireContext().resources.getString(R.string.add_alias_desc, User.userResource.username)

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            fillSpinners(root, requireContext())
        }

        binding.bsAddaliasAliasAddAliasButton.setOnClickListener(this)
        spinnerChangeListener(root, requireContext())
        return root
    }

    /*
    the custom format is not available for shared domains
     */
    private fun spinnerChangeListener(root: View, context: Context) {
        binding.bsAddaliasAliasFormatMact.setOnItemClickListener { _, _, _, _ ->
            // Since the alias format changed, check if custom is available
            checkIfCustomIsAvailable(root, context)
            binding.bsAddaliasAliasFormatTil.error = null
        }

        binding.bsAddaliasDomainMact.setOnItemClickListener { _, _, _, _ ->
            binding.bsAddaliasDomainTil.error = null
        }
    }

    private fun checkIfCustomIsAvailable(root: View, context: Context) {
        // If the selected domain format is custom
        if (binding.bsAddaliasAliasFormatMact.text.toString() == context.resources.getString(R.string.domains_format_custom)) {
            binding.bsAddaliasAliasLocalPartTil.visibility = View.VISIBLE
        } else {
            binding.bsAddaliasAliasLocalPartTil.visibility = View.GONE
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
                binding.bsAddaliasDomainMact.setAdapter(domainAdapter)

                // Set default domain
                if (result.defaultAliasDomain != null) {
                    binding.bsAddaliasDomainMact.setText(result.defaultAliasDomain, false)
                }

                // Set default format
                FORMATS = context.resources.getStringArray(R.array.domains_formats_names).toList()
                val FORMATSID = context.resources.getStringArray(R.array.domains_formats).toList()

                val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    FORMATS
                )
                binding.bsAddaliasAliasFormatMact.setAdapter(formatAdapter)
                // Set default format
                if (result.defaultAliasFormat != null) {
                    binding.bsAddaliasAliasFormatMact.setText(
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

        if (!DOMAINS.contains(binding.bsAddaliasDomainMact.text.toString())) {
            binding.bsAddaliasDomainTil.error =
                context.resources.getString(R.string.not_a_valid_domain)
            return
        }

        if (!FORMATS.contains(binding.bsAddaliasAliasFormatMact.text.toString())) {
            binding.bsAddaliasAliasFormatTil.error =
                context.resources.getString(R.string.not_a_valid_alias_format)
            return
        }

        // If the selected domain format is random words
        if (binding.bsAddaliasAliasFormatMact.text.toString() == context.resources.getString(R.string.domains_format_random_words)) {
            // If the user has a free subscription
            if (User.userResource.subscription == SUBSCRIPTIONS.FREE.subscription) {
                binding.bsAddaliasAliasFormatTil.error =
                    context.resources.getString(R.string.domains_format_random_words_not_available_for_this_subscription)
                return
            }
        }

        // If the selected domain format is custom
        if (binding.bsAddaliasAliasFormatMact.text.toString() == context.resources.getString(R.string.domains_format_custom)) {
            // If the selected domain contains a shared domain disable the local part box
            if (context.resources.getStringArray(R.array.shared_domains).contains(binding.bsAddaliasDomainMact.text.toString())) {
                binding.bsAddaliasAliasFormatTil.error = context.resources.getString(R.string.domains_format_custom_not_available_for_this_domain)
                return
            }

            if (binding.bsAddaliasAliasLocalPartTiet.text.toString().isEmpty()) {
                binding.bsAddaliasAliasLocalPartTil.error = context.resources.getString(R.string.this_field_cannot_be_empty)
                return
            }
        }


        // Set error to null if domain and alias is valid
        binding.bsAddaliasDomainTil.error = null
        binding.bsAddaliasAliasFormatTil.error = null

        binding.bsAddaliasAliasAddAliasButton.isEnabled = false
        binding.bsAddaliasAliasProgressbar.visibility = View.VISIBLE
        val domain = binding.bsAddaliasDomainMact.text.toString()
        val description = binding.bsAddaliasAliasDescTiet.text.toString()
        val localPart = binding.bsAddaliasAliasLocalPartTiet.text.toString()
        val format =
            context.resources.getStringArray(R.array.domains_formats)[context.resources.getStringArray(
                R.array.domains_formats_names
            ).indexOf(binding.bsAddaliasAliasFormatMact.text.toString())]

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
        networkHelper.addAlias({ result ->
            if (result == "201") {
                listener.onAdded()
            } else {
                binding.bsAddaliasAliasAddAliasButton.isEnabled = true
                binding.bsAddaliasAliasProgressbar.visibility = View.INVISIBLE
                binding.bsAddaliasAliasDescTil.error =
                    context.resources.getString(R.string.error_adding_alias) + "\n" + result
            }
        }, domain, description, format, local_part)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addalias_alias_add_alias_button) {
                addAlias(requireView(), requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}