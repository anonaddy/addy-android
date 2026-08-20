package host.stjin.anonaddy.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R

class EmailClientAdapter(
    private val context: Context,
    private val items: List<EmailClientItem>,
    private val showSelection: Boolean = true,
    private val onItemClick: (EmailClientItem) -> Unit
) : RecyclerView.Adapter<EmailClientAdapter.ViewHolder>() {

    data class EmailClientItem(
        val packageName: String?,
        val name: String,
        val icon: Drawable,
        val isSelected: Boolean = false
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.email_client_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.icon.setImageDrawable(item.icon)
        holder.check.isVisible = showSelection && item.isSelected
        holder.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.email_client_item_root)
        val icon: ImageView = view.findViewById(R.id.email_client_item_icon)
        val name: TextView = view.findViewById(R.id.email_client_item_name)
        val check: ImageView = view.findViewById(R.id.email_client_item_check)
    }
}
