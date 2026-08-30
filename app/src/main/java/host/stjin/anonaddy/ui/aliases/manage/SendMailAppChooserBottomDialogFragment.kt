package host.stjin.anonaddy.ui.aliases.manage

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.adapter.EmailClientAdapter
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.BottomsheetSendMailAppChooserBinding
import host.stjin.anonaddy_shared.managers.SettingsManager

class SendMailAppChooserBottomDialogFragment : BaseBottomSheetDialogFragment() {

    private var _binding: BottomsheetSendMailAppChooserBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager
    private var emailIntent: Intent? = null
    private var isAppSelected = false
    private var listener: SendMailAppChooserBottomDialogListener? = null

    interface SendMailAppChooserBottomDialogListener {
        fun onAppSelected()
        fun onChooserDismissed()
    }

    companion object {
        private const val ARG_INTENT = "arg_intent"

        fun newInstance(intent: Intent): SendMailAppChooserBottomDialogFragment {
            val fragment = SendMailAppChooserBottomDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_INTENT, intent)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (parentFragment as? SendMailAppChooserBottomDialogListener) ?: (context as? SendMailAppChooserBottomDialogListener) ?: (activity as? SendMailAppChooserBottomDialogListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emailIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_INTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSendMailAppChooserBinding.inflate(inflater, container, false)
        settingsManager = ServiceLocator.encryptedSettingsManager
        setupList()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isAppSelected) {
            listener?.onChooserDismissed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        val context = requireContext()
        val packageManager = context.packageManager
        val intent = emailIntent ?: return

        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val items = mutableListOf<EmailClientAdapter.EmailClientItem>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfoList) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg != context.packageName && seenPackages.add(pkg)) {
                val label = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
                items.add(
                    EmailClientAdapter.EmailClientItem(
                        packageName = pkg,
                        name = label,
                        icon = icon,
                        isSelected = false
                    )
                )
            }
        }

        binding.bsSendMailAppChooserRecyclerview.adapter = EmailClientAdapter(items, showSelection = false) { selectedItem ->
            val pkg = selectedItem.packageName ?: return@EmailClientAdapter
            if (binding.bsSendMailAppChooserCheckbox.isChecked) {
                settingsManager.putSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT, pkg)
            }

            val launchIntent = Intent(intent).apply {
                setPackage(pkg)
            }
            isAppSelected = true
            context.startActivity(launchIntent)
            dismissAllowingStateLoss()
            listener?.onAppSelected()
        }
    }
}
