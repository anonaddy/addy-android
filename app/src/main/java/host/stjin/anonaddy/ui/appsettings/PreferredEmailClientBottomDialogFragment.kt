package host.stjin.anonaddy.ui.appsettings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.EmailClientAdapter
import host.stjin.anonaddy.databinding.BottomsheetPreferredEmailClientBinding
import host.stjin.anonaddy_shared.managers.SettingsManager

class PreferredEmailClientBottomDialogFragment : BaseBottomSheetDialogFragment() {

    interface PreferredEmailClientBottomDialogListener {
        fun onPreferredEmailClientSelected(packageName: String?, appName: String)
    }

    private var listener: PreferredEmailClientBottomDialogListener? = null
    private var _binding: BottomsheetPreferredEmailClientBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PreferredEmailClientBottomDialogListener) {
            listener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetPreferredEmailClientBinding.inflate(inflater, container, false)
        settingsManager = SettingsManager(true, requireContext())
        setupList()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        val context = requireContext()
        val packageManager = context.packageManager
        val currentPreferredPackage = settingsManager.getSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
        }

        val items = mutableListOf<EmailClientAdapter.EmailClientItem>()

        // 1. "Always ask" option
        val isAlwaysAskSelected = currentPreferredPackage.isNullOrEmpty()
        val alwaysAskIcon = ContextCompat.getDrawable(context, R.drawable.ic_dots_circle_horizontal)
            ?: ContextCompat.getDrawable(context, R.drawable.ic_mail)!!
        items.add(
            EmailClientAdapter.EmailClientItem(
                packageName = null,
                name = getString(R.string.always_ask),
                icon = alwaysAskIcon,
                isSelected = isAlwaysAskSelected
            )
        )

        // 2. Installed email clients
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfoList) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg != context.packageName && seenPackages.add(pkg)) {
                val label = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
                val isSelected = !currentPreferredPackage.isNullOrEmpty() && currentPreferredPackage == pkg
                items.add(
                    EmailClientAdapter.EmailClientItem(
                        packageName = pkg,
                        name = label,
                        icon = icon,
                        isSelected = isSelected
                    )
                )
            }
        }

        binding.bsPreferredEmailClientRecyclerview.adapter = EmailClientAdapter(context, items, showSelection = true) { selectedItem ->
            val pkgToSave = selectedItem.packageName ?: ""
            settingsManager.putSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT, pkgToSave)
            listener?.onPreferredEmailClientSelected(selectedItem.packageName, selectedItem.name)
            dismissAllowingStateLoss()
        }
    }
}
