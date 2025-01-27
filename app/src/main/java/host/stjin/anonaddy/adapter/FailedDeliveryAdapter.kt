package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.utils.DateTimeUtils

class FailedDeliveryAdapter(
    private val listWithFailedDeliveries: ArrayList<FailedDeliveries>
) :
    RecyclerView.Adapter<FailedDeliveryAdapter.ViewHolder>() {

    lateinit var onFailedDeliveryClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.failed_deliveries_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mAlias.text = listWithFailedDeliveries[position].alias_id
        holder.mCreated.text = DateTimeUtils.convertStringToLocalTimeZoneString(listWithFailedDeliveries[position].created_at)
        holder.mCode.text = listWithFailedDeliveries[position].code

    }

    override fun getItemCount(): Int = listWithFailedDeliveries.size


    fun setClickListener(aClickListener: ClickListener) {
        onFailedDeliveryClicker = aClickListener
    }

    fun getList(): ArrayList<FailedDeliveries> {
        return listWithFailedDeliveries
    }


    interface ClickListener {
        fun onClickDetails(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mOptionsButton: MaterialButton =
            view.findViewById(R.id.failed_deliveries_recyclerview_list_details_button)
        var mAlias: TextView = view.findViewById(R.id.failed_deliveries_recyclerview_list_alias)
        var mCode: TextView =
            view.findViewById(R.id.failed_deliveries_recyclerview_list_code)
        var mCreated: TextView =
            view.findViewById(R.id.failed_deliveries_recyclerview_list_created)

        init {
            mOptionsButton.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.failed_deliveries_recyclerview_list_details_button -> {
                    onFailedDeliveryClicker.onClickDetails(adapterPosition, p0)
                }

            }
        }
    }


}

