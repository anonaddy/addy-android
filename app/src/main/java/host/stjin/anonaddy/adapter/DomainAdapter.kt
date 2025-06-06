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
import host.stjin.anonaddy_shared.models.Domains

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


        when (listWithDomains[position].domain_sending_verified_at) {
            null -> {
                holder.mDescription.text = holder.mDescription.context.resources.getString(
                    R.string.configuration_error
                )
                holder.domainsRecyclerviewListIcon.setImageResource(R.drawable.ic_alert_circle)
            }
            else -> {
                holder.mDescription.text = holder.mDescription.context.resources.getString(
                    R.string.domains_list_description,
                    listWithDomains[position].aliases_count
                )
                holder.domainsRecyclerviewListIcon.setImageResource(R.drawable.ic_dns)
            }
        }
    }

    override fun getItemCount(): Int = listWithDomains.size


    fun setClickListener(aClickListener: ClickListener) {
        onDomainClicker = aClickListener
    }

    fun getList(): ArrayList<Domains> {
        return listWithDomains
    }


    interface ClickListener {
        fun onClickSettings(pos: Int, aView: View)
        fun onClickDelete(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mCV: MaterialCardView = view.findViewById(R.id.domains_recyclerview_list_CV)
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
            mCV.setOnClickListener(this)
            domainsRecyclerviewListSettingsButton.setOnClickListener(this)
            domainsRecyclerviewListDeleteButton.setOnClickListener(this)

            checkForTabletLayout(domainsRecyclerviewListDeleteButton.context)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.domains_recyclerview_list_CV -> {
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
            if (!domainsRecyclerviewListOptionLl.context.resources.getBoolean(R.bool.isTablet)) {
                if (domainsRecyclerviewListOptionLl.visibility == View.VISIBLE) {
                    domainsRecyclerviewListOptionLl.visibility = View.GONE
                    mOptionsButton.rotation = 0f
                } else {
                    mOptionsButton.rotation = 180f
                    domainsRecyclerviewListOptionLl.visibility = View.VISIBLE
                }
            }
        }

        private fun checkForTabletLayout(context: Context) {
            if (context.resources.getBoolean(R.bool.isTablet)) {
                mOptionsButton.visibility = View.GONE
                domainsRecyclerviewListOptionLl.visibility = View.VISIBLE
            }
        }
    }


}

