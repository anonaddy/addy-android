package host.stjin.anonaddy.utils

import android.content.Context
import android.view.LayoutInflater
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import host.stjin.anonaddy.R


object MaterialDialogHelper {

    /*
    My custom MaterialAlertDialog implementation with easy support for things like inputfields and actions
     */
    fun showMaterialDialog(
        context: Context,
        title: String? = null,
        message: String? = null,
        icon: Int? = null,
        textInputHint: String? = null,
        getTextInput: ((text: String) -> Unit)? = null,
        neutralButtonText: String? = null,
        neutralButtonAction: (() -> Unit)? = null,
        positiveButtonText: String? = null,
        positiveButtonAction: (() -> Unit)? = null,
        negativeButtonText: String? = null,
        negativeButtonAction: (() -> Unit)? = null
    ): MaterialAlertDialogBuilder {
        val materialDialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
        title?.let { materialDialog.setTitle(it) }
        icon?.let { materialDialog.setIcon(it) }
        message?.let { materialDialog.setMessage(it) }

        if (getTextInput != null) {
            val materialAlertDialogInput = LayoutInflater.from(context).inflate(R.layout.material_alert_dialog_input, null)
            textInputHint?.let { materialAlertDialogInput.findViewById<TextInputLayout>(R.id.material_alert_dialog_input_til).hint = it }

            materialDialog.setView(materialAlertDialogInput)

            materialAlertDialogInput.findViewById<TextInputEditText>(R.id.material_alert_dialog_input_tiet).addTextChangedListener {
                getTextInput(it.toString())
            }
        }

        if (!neutralButtonText.isNullOrEmpty()) {
            materialDialog.setNeutralButton(neutralButtonText) { dialog, _ ->
                if (neutralButtonAction != null) {
                    neutralButtonAction()
                } else {
                    dialog.dismiss()
                }
            }
        }
        if (!positiveButtonText.isNullOrEmpty()) {
            materialDialog.setPositiveButton(positiveButtonText) { dialog, _ ->
                if (positiveButtonAction != null) {
                    positiveButtonAction()
                } else {
                    dialog.dismiss()
                }
            }
        }

        if (!negativeButtonText.isNullOrEmpty()) {
            materialDialog.setNegativeButton(negativeButtonText) { dialog, _ ->
                if (negativeButtonAction != null) {
                    negativeButtonAction()
                } else {
                    dialog.dismiss()
                }
            }
        }

        return materialDialog
    }

    fun aliasForgetDialog(context: Context, positiveButtonAction: (() -> Unit)) {
        showMaterialDialog(
            context = context,
            title = context.resources.getString(R.string.forget_alias),
            message = context.resources.getString(R.string.forget_alias_confirmation_desc),
            icon = R.drawable.ic_eraser,
            neutralButtonText = context.resources.getString(R.string.cancel),
            positiveButtonText = context.resources.getString(R.string.forget),
            positiveButtonAction = {
                positiveButtonAction()
            }
        ).show()
    }

    fun aliasDeleteDialog(context: Context, positiveButtonAction: (() -> Unit)) {
        showMaterialDialog(
            context = context,
            title = context.resources.getString(R.string.delete_alias),
            message = context.resources.getString(R.string.delete_alias_confirmation_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = context.resources.getString(R.string.cancel),
            positiveButtonText = context.resources.getString(R.string.delete),
            positiveButtonAction = {
                positiveButtonAction()
            }
        ).show()
    }

    fun aliasRestoreDialog(context: Context, positiveButtonAction: (() -> Unit)) {
        showMaterialDialog(
            context = context,
            title = context.resources.getString(R.string.restore_alias),
            message = context.resources.getString(R.string.restore_alias_confirmation_desc),
            icon = R.drawable.ic_trash_off,
            neutralButtonText = context.resources.getString(R.string.cancel),
            positiveButtonText = context.resources.getString(R.string.restore),
            positiveButtonAction = {
                positiveButtonAction()
            }
        ).show()
    }
}