package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.Domains

class DomainAdapter(
    private val listWithDomains: ArrayList<Domains>
) :
    RecyclerView.Adapter<DomainAdapter.ViewHolder>() {

    lateinit var onDomainClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.domains_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle.text = listWithDomains[position].domain


        val aliases = listWithDomains[position].aliases?.size ?: 0
        var forwardedEmails = 0

        // Count the total amount of forwarded emails for all aliases
        // can only check the forwarded emails if the aliases are more than 0
        if (aliases > 0) {
            for (alias in listWithDomains[position].aliases!!) {
                forwardedEmails += alias.emails_forwarded
            }
        }


        when (listWithDomains[position].domain_sending_verified_at) {
            null -> {
                holder.mDescription.text = holder.mDescription.context.resources.getString(
                    R.string.configuration_error
                )
                holder.domainsRecyclerviewListIcon.setImageResource(R.drawable.ic_round_error_outline_24)
            }
            else -> {
                holder.mDescription.text = holder.mDescription.context.resources.getString(
                    R.string.domains_list_description,
                    aliases,
                    forwardedEmails
                )
                holder.domainsRecyclerviewListIcon.setImageResource(R.drawable.ic_outline_dns_24)
            }
        }
    }

    override fun getItemCount(): Int = listWithDomains.size


    fun setClickListener(aClickListener: ClickListener) {
        onDomainClicker = aClickListener
    }


    interface ClickListener {
        fun onClickSettings(pos: Int, aView: View)
        fun onClickDelete(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mLL: LinearLayout = view.findViewById(R.id.domains_recyclerview_list_LL)
        private var domainsRecyclerviewListOptionLl: LinearLayout =
            view.findViewById(R.id.domains_recyclerview_list_option_LL)
        private var mOptionsButton: LinearLayout =
            view.findViewById(R.id.domains_recyclerview_list_expand_options)
        var mTitle: TextView = view.findViewById(R.id.domains_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.domains_recyclerview_list_description)
        var domainsRecyclerviewListIcon: ImageView =
            view.findViewById(R.id.domains_recyclerview_list_icon)
        private var domainsRecyclerviewListSettingsButton: MaterialButton =
            view.findViewById(R.id.domains_recyclerview_list_settings_button)
        private var domainsRecyclerviewListDeleteButton: MaterialButton =
            view.findViewById(R.id.domains_recyclerview_list_delete_button)


        init {
            mOptionsButton.setOnClickListener(this)
            mLL.setOnClickListener(this)
            domainsRecyclerviewListSettingsButton.setOnClickListener(this)
            domainsRecyclerviewListDeleteButton.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.domains_recyclerview_list_LL -> {
                    expandOptions()
                }
                R.id.domains_recyclerview_list_expand_options -> {
                    expandOptions()
                }
                R.id.domains_recyclerview_list_settings_button -> {
                    onDomainClicker.onClickSettings(adapterPosition, p0)
                }
                R.id.domains_recyclerview_list_delete_button -> {
                    onDomainClicker.onClickDelete(adapterPosition, p0)
                }
            }
        }

        private fun expandOptions() {
            if (domainsRecyclerviewListOptionLl.visibility == View.VISIBLE) {
                domainsRecyclerviewListOptionLl.visibility = View.GONE
                mOptionsButton.rotation = 0f
            } else {
                mOptionsButton.rotation = 180f
                domainsRecyclerviewListOptionLl.visibility = View.VISIBLE
            }
        }

    }
}

