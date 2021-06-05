package host.stjin.anonaddy.ui.usernames.manage

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.shape.ShapeAppearanceModel
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditRecipientUsernameBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class EditUsernameRecipientBottomDialogFragment(
    private val usernameId: String?,
    private val defaultRecipient: String?
) :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {


    private lateinit var listener: AddEditUsernameRecipientBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameRecipientBottomDialogListener {
        fun recipientEdited()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetEditRecipientUsernameBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditRecipientUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if usernameId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (usernameId != null) {
            listener = activity as AddEditUsernameRecipientBottomDialogListener

            // Set button listeners and current description
            binding.bsEditrecipientSaveButton.setOnClickListener(this)

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                getAllRecipients(requireContext())
            }
        } else {
            dismiss()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsEditrecipientRoot)
        }

        return root
    }

    private suspend fun getAllRecipients(context: Context) {
        val networkHelper = NetworkHelper(context)

        networkHelper.getRecipients({ result ->
            if (result != null) {
                // Remove the default "Loading recipients" chip
                binding.bsEditrecipientChipgroup.removeAllViewsInLayout()
                binding.bsEditrecipientChipgroup.requestLayout()
                binding.bsEditrecipientChipgroup.invalidate()

                for (recipient in result) {
                    val chip = Chip(ContextThemeWrapper(binding.bsEditrecipientChipgroup.context, R.style.AnonAddyChip))
                    chip.text = recipient.email
                    chip.tag = recipient.id
                    chip.isClickable = true
                    chip.isCheckable = true
                    chip.shapeAppearanceModel =
                        ShapeAppearanceModel().toBuilder().setAllCornerSizes(context.resources.getDimension(R.dimen.corner_radius_chips)).build()
                    chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.transparent))
                    chip.checkedIcon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_outline_check_24, null)
                    chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.shimmerGray))
                    chip.chipStrokeWidth = context.resources.getDimension(R.dimen.chip_stroke_width)
                    chip.isChecked = defaultRecipient.equals(recipient.email)

                    binding.bsEditrecipientChipgroup.addView(chip)
                }
            }
        }, true)
    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    companion object {
        fun newInstance(
            id: String,
            recipient: String?
        ): EditUsernameRecipientBottomDialogFragment {
            return EditUsernameRecipientBottomDialogFragment(id, recipient)
        }
    }

    private fun editRecipient(context: Context) {
        // Animate the button to progress
        binding.bsEditrecipientSaveButton.startAnimation()

        var recipient = ""
        val ids: List<Int> = binding.bsEditrecipientChipgroup.checkedChipIds
        for (id in ids) {
            val chip: Chip = binding.bsEditrecipientChipgroup.findViewById(id)
            recipient = chip.tag.toString()
        }


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            // usernameId is never null at this point, hence the !!
            editRecipientHttp(context, usernameId!!, recipient)
        }
    }

    private suspend fun editRecipientHttp(
        context: Context,
        aliasId: String,
        recipient: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDefaultRecipientForSpecificUsername({ result ->
            if (result == "200") {
                listener.recipientEdited()
            } else {
                // Revert the button to normal
                binding.bsEditrecipientSaveButton.revertAnimation()

                binding.bsEditrecipientTil.error =
                    context.resources.getString(R.string.error_edit_recipient) + "\n" + result
            }
        }, aliasId, recipient)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editrecipient_save_button) {
                editRecipient(
                    requireContext()
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}