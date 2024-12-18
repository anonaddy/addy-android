package host.stjin.anonaddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.Recipients

class RecipientAdapter(
    private val listWithRecipients: ArrayList<Recipients>
) :
    RecyclerView.Adapter<RecipientAdapter.ViewHolder>() {

    lateinit var onRecipientClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recipients_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle.text = listWithRecipients[position].email


        val aliases = listWithRecipients[position].aliases_count


        holder.mDescription.text = holder.mDescription.context.resources.getString(
            R.string.recipients_list_description,
            aliases
        )

        when {
            listWithRecipients[position].email_verified_at == null -> {
                holder.recipientsRecyclerviewListIcon.setImageResource(R.drawable.ic_alert_circle)
                holder.mDescription.text = holder.mDescription.context.resources.getString(R.string.not_verified)

                holder.recipientsRecyclerviewListDeleteButton.visibility = View.VISIBLE
                holder.recipientsRecyclerviewListResendButton.visibility = View.VISIBLE
                holder.recipientsRecyclerviewListSettingsButton.visibility = View.GONE
            }
            listWithRecipients[position].should_encrypt -> {
                holder.recipientsRecyclerviewListIcon.setImageResource(R.drawable.ic_mail_encrypted)

                holder.recipientsRecyclerviewListDeleteButton.visibility = View.VISIBLE
                holder.recipientsRecyclerviewListResendButton.visibility = View.GONE
                holder.recipientsRecyclerviewListSettingsButton.visibility = View.VISIBLE
            }
            else -> {
                holder.recipientsRecyclerviewListIcon.setImageResource(R.drawable.ic_mail)

                holder.recipientsRecyclerviewListDeleteButton.visibility = View.VISIBLE
                holder.recipientsRecyclerviewListResendButton.visibility = View.GONE
                holder.recipientsRecyclerviewListSettingsButton.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = listWithRecipients.size


    fun setClickListener(aClickListener: ClickListener) {
        onRecipientClicker = aClickListener
    }

    fun getList(): List<Recipients> {
        return listWithRecipients
    }


    interface ClickListener {
        fun onClickSettings(pos: Int, aView: View)
        fun onClickResend(pos: Int, aView: View)
        fun onClickDelete(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mCV: MaterialCardView = view.findViewById(R.id.recipients_recyclerview_list_CV)
        private var recipientsRecyclerviewListOptionLl: LinearLayout =
            view.findViewById(R.id.recipients_recyclerview_list_option_LL)
        private var mOptionsButton: LinearLayout =
            view.findViewById(R.id.recipients_recyclerview_list_expand_options)
        var mTitle: TextView = view.findViewById(R.id.recipients_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.recipients_recyclerview_list_description)
        var recipientsRecyclerviewListIcon: ImageView =
            view.findViewById(R.id.recipients_recyclerview_list_icon)
        var recipientsRecyclerviewListSettingsButton: MaterialButton =
            view.findViewById(R.id.recipients_recyclerview_list_settings_button)
        var recipientsRecyclerviewListResendButton: MaterialButton =
            view.findViewById(R.id.recipients_recyclerview_list_resend_button)
        var recipientsRecyclerviewListDeleteButton: MaterialButton =
            view.findViewById(R.id.recipients_recyclerview_list_delete_button)


        init {
            mOptionsButton.setOnClickListener(this)
            mCV.setOnClickListener(this)
            recipientsRecyclerviewListSettingsButton.setOnClickListener(this)
            recipientsRecyclerviewListResendButton.setOnClickListener(this)
            recipientsRecyclerviewListDeleteButton.setOnClickListener(this)

            checkForTabletLayout(recipientsRecyclerviewListDeleteButton.context)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.recipients_recyclerview_list_CV -> {
                    expandOptions()
                }
                R.id.recipients_recyclerview_list_expand_options -> {
                    expandOptions()
                }
                R.id.recipients_recyclerview_list_settings_button -> {
                    onRecipientClicker.onClickSettings(adapterPosition, p0)
                }
                R.id.recipients_recyclerview_list_resend_button -> {
                    onRecipientClicker.onClickResend(adapterPosition, p0)
                }
                R.id.recipients_recyclerview_list_delete_button -> {
                    onRecipientClicker.onClickDelete(adapterPosition, p0)
                }
            }
        }

        private fun expandOptions() {
            if (!recipientsRecyclerviewListOptionLl.context.resources.getBoolean(R.bool.isTablet)) {
                if (recipientsRecyclerviewListOptionLl.visibility == View.VISIBLE) {
                    recipientsRecyclerviewListOptionLl.visibility = View.GONE
                    mOptionsButton.rotation = 0f
                } else {
                    mOptionsButton.rotation = 180f
                    recipientsRecyclerviewListOptionLl.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                mOptionsButton.visibility = View.GONE
                recipientsRecyclerviewListOptionLl.visibility = View.VISIBLE
            }
        }

    }
}

