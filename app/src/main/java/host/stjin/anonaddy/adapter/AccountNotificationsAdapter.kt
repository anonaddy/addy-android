package host.stjin.anonaddy.adapter

import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.utils.DateTimeUtils

class AccountNotificationsAdapter(
    private val listWithAccountNotifications: ArrayList<AccountNotifications>
) :
    RecyclerView.Adapter<AccountNotificationsAdapter.ViewHolder>() {

    lateinit var onAccountNotificationClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.account_notifications_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle.text = listWithAccountNotifications[position].title
        holder.mCreated.text = DateTimeUtils.turnStringIntoLocalString(listWithAccountNotifications[position].created_at)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.mText.text = Html.fromHtml(
                listWithAccountNotifications[position].text,
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            holder.mText.text =
                Html.fromHtml(listWithAccountNotifications[position].text)
        }
    }

    override fun getItemCount(): Int = listWithAccountNotifications.size


    fun setClickListener(aClickListener: ClickListener) {
        onAccountNotificationClicker = aClickListener
    }

    fun getList(): ArrayList<AccountNotifications> {
        return listWithAccountNotifications
    }


    interface ClickListener {
        fun onClickDetails(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mOptionsButton: MaterialButton =
            view.findViewById(R.id.account_notifications_recyclerview_list_details_button)
        var mTitle: TextView = view.findViewById(R.id.account_notifications_recyclerview_list_title)
        var mText: TextView =
            view.findViewById(R.id.account_notifications_recyclerview_list_text)
        var mCreated: TextView =
            view.findViewById(R.id.account_notifications_recyclerview_list_created)

        init {
            mOptionsButton.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.account_notifications_recyclerview_list_details_button -> {
                    onAccountNotificationClicker.onClickDetails(adapterPosition, p0)
                }

            }
        }
    }


}

