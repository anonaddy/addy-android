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
import host.stjin.anonaddy.models.Usernames
import host.stjin.anonaddy.utils.DateTimeUtils

class UsernameAdapter(
    private val listWithUsernames: ArrayList<Usernames>
) :
    RecyclerView.Adapter<UsernameAdapter.Holder>() {

    lateinit var onUsernameClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.usernames_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.mTitle.text = listWithUsernames[position].username


        if (listWithUsernames[position].description != null) {
            holder.mDescription.text =
                "${listWithUsernames[position].description}\n${
                    (holder.mDescription.context).resources.getString(
                        R.string.created_at_s,
                        DateTimeUtils.turnStringIntoLocalString(listWithUsernames[position].created_at)
                    )
                }\n${
                    (holder.mDescription.context).resources.getString(
                        R.string.updated_at_s,
                        DateTimeUtils.turnStringIntoLocalString(listWithUsernames[position].updated_at)
                    )
                }"
        } else {
            holder.mDescription.text =
                "${
                    (holder.mDescription.context).resources.getString(
                        R.string.created_at_s,
                        DateTimeUtils.turnStringIntoLocalString(listWithUsernames[position].created_at)
                    )
                }\n${
                    (holder.mDescription.context).resources.getString(
                        R.string.updated_at_s,
                        DateTimeUtils.turnStringIntoLocalString(listWithUsernames[position].updated_at)
                    )
                }"
        }

        if (listWithUsernames[position].active) {
            holder.usernamesRecyclerviewListUser.setImageResource(R.drawable.ic_account_outline)
        } else {
            holder.usernamesRecyclerviewListUser.setImageResource(R.drawable.ic_account_off_outline)
        }
    }

    override fun getItemCount(): Int = listWithUsernames.size


    fun setClickListener(aClickListener: ClickListener) {
        onUsernameClicker = aClickListener
    }


    interface ClickListener {
        fun onClickSettings(pos: Int, aView: View)
        fun onClickDelete(pos: Int, aView: View)
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mLL: LinearLayout = view.findViewById(R.id.usernames_recyclerview_list_LL)
        private var usernamesRecyclerviewListOptionLl: LinearLayout =
            view.findViewById(R.id.usernames_recyclerview_list_option_LL)
        private var mOptionsButton: LinearLayout =
            view.findViewById(R.id.usernames_recyclerview_list_expand_options)
        var mTitle: TextView = view.findViewById(R.id.usernames_recyclerview_list_title)
        var mDescription: TextView =
            view.findViewById(R.id.usernames_recyclerview_list_description)
        var usernamesRecyclerviewListUser: ImageView =
            view.findViewById(R.id.usernames_recyclerview_list_user)
        private var usernamesRecyclerviewListSettingsButton: MaterialButton =
            view.findViewById(R.id.usernames_recyclerview_list_settings_button)
        private var usernamesRecyclerviewListDeleteButton: MaterialButton =
            view.findViewById(R.id.usernames_recyclerview_list_delete_button)


        init {
            mOptionsButton.setOnClickListener(this)
            mLL.setOnClickListener(this)
            usernamesRecyclerviewListSettingsButton.setOnClickListener(this)
            usernamesRecyclerviewListDeleteButton.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.usernames_recyclerview_list_LL -> {
                    expandOptions()
                }
                R.id.usernames_recyclerview_list_expand_options -> {
                    expandOptions()
                }
                R.id.usernames_recyclerview_list_settings_button -> {
                    onUsernameClicker.onClickSettings(adapterPosition, p0)
                }
                R.id.usernames_recyclerview_list_delete_button -> {
                    onUsernameClicker.onClickDelete(adapterPosition, p0)
                }
            }
        }

        private fun expandOptions() {
            if (usernamesRecyclerviewListOptionLl.visibility == View.VISIBLE) {
                usernamesRecyclerviewListOptionLl.visibility = View.GONE
                mOptionsButton.rotation = 0f
            } else {
                mOptionsButton.rotation = 180f
                usernamesRecyclerviewListOptionLl.visibility = View.VISIBLE
            }
        }

    }
}
