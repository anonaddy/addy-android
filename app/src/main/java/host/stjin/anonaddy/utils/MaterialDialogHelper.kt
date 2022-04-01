package host.stjin.anonaddy.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import host.stjin.anonaddy.R

object MaterialDialogHelper {
    fun showMaterialDialog(
        context: Context,
        title: String,
        message: String? = null,
        icon: Int? = null,
        neutralButtonText: String? = null,
        neutralButtonAction: (() -> Unit)? = null,
        positiveButtonText: String? = null,
        positiveButtonAction: (() -> Unit)? = null,
        negativeButtonText: String? = null,
        negativeButtonAction: (() -> Unit)? = null
    ): MaterialAlertDialogBuilder {
        val materialDialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(title)

        icon?.let { materialDialog.setIcon(it) }
        message?.let { materialDialog.setMessage(it) }

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
            materialDialog.setNegativeButton(positiveButtonText) { dialog, _ ->
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