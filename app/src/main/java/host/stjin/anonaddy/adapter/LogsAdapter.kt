package host.stjin.anonaddy.adapter

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.Logs


class LogsAdapter(
    private val listWithLogs: ArrayList<Logs>
) :
    RecyclerView.Adapter<LogsAdapter.ViewHolder>() {

    lateinit var onLogLayoutClickListener: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.logs_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mDate.text = listWithLogs[position].dateTime
        holder.mMessage.text = listWithLogs[position].message

        when (listWithLogs[position].importance) {
            0 -> {
                for (drawable in holder.mMessage.compoundDrawablesRelative) {
                    if (drawable != null) {
                        drawable.colorFilter = PorterDuffColorFilter(
                            ContextCompat.getColor(holder.mMessage.context, R.color.logImportanceCritical),
                            PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }
            }
            1 -> {
                for (drawable in holder.mMessage.compoundDrawablesRelative) {
                    if (drawable != null) {
                        drawable.colorFilter = PorterDuffColorFilter(
                            ContextCompat.getColor(holder.mMessage.context, R.color.logImportanceWarning),
                            PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }
            }
            2 -> {
                for (drawable in holder.mMessage.compoundDrawablesRelative) {
                    if (drawable != null) {
                        drawable.colorFilter = PorterDuffColorFilter(
                            ContextCompat.getColor(holder.mMessage.context, R.color.logImportanceInfo),
                            PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int = listWithLogs.size


    fun setClickListener(aClickListener: ClickListener) {
        onLogLayoutClickListener = aClickListener
    }

    fun getList(): ArrayList<Logs> {
        return listWithLogs
    }


    interface ClickListener {
        fun onClickDetails(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mLayout: LinearLayout = view.findViewById(R.id.logs_recyclerview_ll)
        var mDate: TextView = view.findViewById(R.id.logs_recyclerview_date)
        var mMessage: TextView =
            view.findViewById(R.id.logs_recyclerview_message)

        init {
            mLayout.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.logs_recyclerview_ll -> {
                    onLogLayoutClickListener.onClickDetails(adapterPosition, p0)
                }

            }
        }
    }


}

