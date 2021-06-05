package host.stjin.anonaddy.ui.alias.manage

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
import host.stjin.anonaddy.databinding.BottomsheetEditRecipientsAliasBinding
import host.stjin.anonaddy.models.Recipients
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class EditAliasRecipientsBottomDialogFragment(
    private val aliasId: String?,
    private val recipients: List<Recipients>?
) :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {


    private lateinit var listener: AddEditAliasRecipientsBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasRecipientsBottomDialogListener {
        fun recipientsEdited()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetEditRecipientsAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditRecipientsAliasBinding.inflate(inflater, container, false)
        val root = binding.root


        // Check if aliasId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (aliasId != null) {
            listener = activity as AddEditAliasRecipientsBottomDialogListener

            // Set button listeners and current description
            binding.bsEditrecipientsSaveButton.setOnClickListener(this)

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                getAllRecipients(requireContext())
            }
        } else {
            dismiss()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsEditrecipientsRoot)
        }

        return root

    }

    private suspend fun getAllRecipients(context: Context) {
        val networkHelper = NetworkHelper(context)

        val recipientUnderThisAliasList = arrayListOf<String>()

        if (recipients != null) {
            for (recipient in recipients) {
                recipientUnderThisAliasList.add(recipient.email)
            }
        }

        networkHelper.getRecipients({ result ->
            if (result != null) {
                // Remove the default "Loading recipients" chip
                binding.bsEditrecipientsChipgroup.removeAllViewsInLayout()
                binding.bsEditrecipientsChipgroup.requestLayout()
                binding.bsEditrecipientsChipgroup.invalidate()

                for (recipient in result) {
                    val chip = Chip(ContextThemeWrapper(binding.bsEditrecipientsChipgroup.context, R.style.AnonAddyChip))
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

                    chip.isChecked = recipientUnderThisAliasList.contains(recipient.email)


                    binding.bsEditrecipientsChipgroup.addView(chip)
                }
            }

        }, true)
    }


    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)
    companion object {
        fun newInstance(
            id: String,
            recipients: List<Recipients>?
        ): EditAliasRecipientsBottomDialogFragment {
            return EditAliasRecipientsBottomDialogFragment(id, recipients)
        }
    }

    private fun editRecipients(context: Context) {

        // Animate the button to progress
        binding.bsEditrecipientsSaveButton.startAnimation()

        val recipients = arrayListOf<String>()
        val ids: List<Int> = binding.bsEditrecipientsChipgroup.checkedChipIds
        for (id in ids) {
            val chip: Chip = binding.bsEditrecipientsChipgroup.findViewById(id)
            recipients.add(chip.tag.toString())
        }


        // aliasId is never null at this point, hence the !!
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            editRecipientsHttp(context, aliasId!!, recipients)
        }
    }

    private suspend fun editRecipientsHttp(
        context: Context,
        aliasId: String,
        recipients: ArrayList<String>
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateRecipientsSpecificAlias({ result ->
            if (result == "200") {
                listener.recipientsEdited()
            } else {
                // Revert the button to normal
                binding.bsEditrecipientsSaveButton.revertAnimation()

                binding.bsEditrecipientsTil.error =
                    context.resources.getString(R.string.error_edit_recipients) + "\n" + result
            }
        }, aliasId, recipients)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editrecipients_save_button) {
                editRecipients(
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