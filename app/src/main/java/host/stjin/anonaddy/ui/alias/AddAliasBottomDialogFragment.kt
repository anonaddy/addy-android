package host.stjin.anonaddy.ui.alias

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAddaliasBinding
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class AddAliasBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddAliasBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddAliasBottomDialogListener {
        fun onAdded()
        fun onCancel()
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


        // Listener only works when called from fragment (this sheet can be called from widget)
        if (parentFragment != null) {
            listener = parentFragment as AddAliasBottomDialogListener
        } else if (activity != null) {
            listener = activity as AddAliasBottomDialogListener
        }

        // Sent the help text username accordingly
        binding.bsAddaliasDomainHelpTextview.text =
            requireContext().resources.getString(R.string.add_alias_desc, (activity?.application as AddyIoApp).userResource.username)

        viewLifecycleOwner.lifecycleScope.launch {
            fillSpinners(requireContext())
            getAllRecipients(requireContext())
        }

        binding.bsAddaliasAliasAddAliasButton.setOnClickListener(this)
        spinnerChangeListener(requireContext())

        return root
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancel()
    }

    private suspend fun getAllRecipients(context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.getRecipients({ result, _ ->
            if (result != null) {
                // Remove the default "Loading recipients" chip
                binding.bsAddaliasRecipientsChipgroup.removeAllViewsInLayout()
                binding.bsAddaliasRecipientsChipgroup.requestLayout()
                binding.bsAddaliasRecipientsChipgroup.invalidate()
                for (recipient in result) {

                    val chip = layoutInflater.inflate(R.layout.chip_view, binding.bsAddaliasRecipientsChipgroup, false) as Chip
                    chip.text = recipient.email
                    chip.tag = recipient.id

                    binding.bsAddaliasRecipientsChipgroup.addView(chip)
                }
            }

        }, true)

        // TODO what if null?
    }


    /*
    the custom format is not available for shared domains
     */
    private fun spinnerChangeListener(context: Context) {
        binding.bsAddaliasAliasFormatMact.setOnItemClickListener { _, _, _, _ ->
            // Since the alias format changed, check if custom is available
            checkIfCustomIsAvailable(context)
            binding.bsAddaliasAliasFormatTil.error = null
        }

        binding.bsAddaliasDomainMact.setOnItemClickListener { _, _, _, _ ->
            binding.bsAddaliasDomainTil.error = null
        }
    }

    private fun checkIfCustomIsAvailable(context: Context) {
        // If the selected domain format is custom
        if (binding.bsAddaliasAliasFormatMact.text.toString() == context.resources.getString(R.string.domains_format_custom)) {
            binding.bsAddaliasAliasLocalPartTil.visibility = View.VISIBLE
        } else {
            binding.bsAddaliasAliasLocalPartTil.visibility = View.GONE
        }
    }


    private var DOMAINS: List<String> = listOf()
    private var sharedDomains: List<String> = listOf()
    private var FORMATS: List<String> = listOf()
    private suspend fun fillSpinners(context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.getDomainOptions { domainOptions, _ ->
            // Set domains and default format/domain
            if (domainOptions?.data != null) {
                DOMAINS = domainOptions.data
                sharedDomains = domainOptions.sharedDomains

                val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    DOMAINS
                )
                binding.bsAddaliasDomainMact.setAdapter(domainAdapter)

                // Set default domain
                binding.bsAddaliasDomainMact.setText(domainOptions.defaultAliasDomain, false)

                // Set default format
                // Get all formats
                FORMATS = context.resources.getStringArray(R.array.domains_formats_names).toList()
                // Get all format ids
                val FORMATSID = context.resources.getStringArray(R.array.domains_formats).toList()

                val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    FORMATS
                )
                binding.bsAddaliasAliasFormatMact.setAdapter(formatAdapter)

                // Set default format
                try {
                    binding.bsAddaliasAliasFormatMact.setText(
                        FORMATS[FORMATSID.indexOf(domainOptions.defaultAliasFormat)],
                        false
                    )
                } catch (e: Exception) {
                    // The default alias format does not exist in the formats array, perhaps it was just added?
                    // To prevent a crash from the ArrayIndexOutOfBoundsException log the error and just continue without filling the spinner
                    val ex = e.message
                    Log.e("AFA", ex.toString())
                    LoggingHelper(context).addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "fillSpinners", null)
                }
            }

            // Since the alias format has been set, check if custom is available
            checkIfCustomIsAvailable(context)
        }

    }

    companion object {
        fun newInstance(): AddAliasBottomDialogFragment {
            return AddAliasBottomDialogFragment()
        }
    }

    private fun addAlias(context: Context) {

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
            if ((activity?.application as AddyIoApp).userResource.hasUserFreeSubscription) {
                binding.bsAddaliasAliasFormatTil.error =
                    context.resources.getString(R.string.domains_format_random_words_not_available_for_this_subscription)
                return
            }
        }

        // If the selected domain format is custom
        if (binding.bsAddaliasAliasFormatMact.text.toString() == context.resources.getString(R.string.domains_format_custom)) {
            // If the selected domain contains a shared domain disable the local part box

            // Only check on hosted instance
            if (AddyIo.isUsingHostedInstance) {
                if (sharedDomains.contains(binding.bsAddaliasDomainMact.text.toString())) {
                    binding.bsAddaliasAliasFormatTil.error = context.resources.getString(R.string.domains_format_custom_not_available_for_this_domain)
                    return
                }
            }

            if (binding.bsAddaliasAliasLocalPartTiet.text.toString().isEmpty()) {
                binding.bsAddaliasAliasLocalPartTil.error = context.resources.getString(R.string.this_field_cannot_be_empty)
                return
            }
        }


        // Set error to null if domain and alias is valid
        binding.bsAddaliasDomainTil.error = null
        binding.bsAddaliasAliasFormatTil.error = null
        binding.bsAddaliasAliasLocalPartTil.error = null

        // Animate the button to progress
        binding.bsAddaliasAliasAddAliasButton.startAnimation()


        val recipients = arrayListOf<String>()
        for (child in binding.bsAddaliasRecipientsChipgroup.children) {
            val chip: Chip = child as Chip
            if (chip.isChecked) recipients.add(chip.tag.toString())
        }

        val domain = binding.bsAddaliasDomainMact.text.toString()
        val description = binding.bsAddaliasAliasDescTiet.text.toString()
        val localPart = binding.bsAddaliasAliasLocalPartTiet.text.toString()
        val format =
            context.resources.getStringArray(R.array.domains_formats)[context.resources.getStringArray(
                R.array.domains_formats_names
            ).indexOf(binding.bsAddaliasAliasFormatMact.text.toString())]

        viewLifecycleOwner.lifecycleScope.launch {
            addAliasToAccount(context, domain, description, format, localPart, recipients)
        }
    }

    private suspend fun addAliasToAccount(
        context: Context,
        domain: String,
        description: String,
        format: String,
        local_part: String,
        recipients: ArrayList<String>
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addAlias({ alias, error ->
            if (alias != null) {
                val clipboard: ClipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("alias", alias.email)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.resources.getString(R.string.copied_alias), Toast.LENGTH_LONG).show()

                listener.onAdded()
            } else {
                // Revert the button to normal
                binding.bsAddaliasAliasAddAliasButton.revertAnimation()

                binding.bsAddaliasAliasDescTil.error =
                    context.resources.getString(R.string.error_adding_alias) + "\n" + error
            }
        }, domain, description, format, local_part, recipients)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addalias_alias_add_alias_button) {
                addAlias(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}