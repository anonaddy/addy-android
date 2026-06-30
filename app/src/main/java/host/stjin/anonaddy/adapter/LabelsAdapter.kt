package host.stjin.anonaddy.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.models.Labels

class LabelsAdapter(
    private val listWithLabels: ArrayList<Labels>
) :
    RecyclerView.Adapter<LabelsAdapter.ViewHolder>() {

    lateinit var onManageLabelsClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.labels_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = listWithLabels[position]
        holder.mName.text = entry.name
        holder.mColorText.text = holder.mColorText.context.resources.getString(
            R.string.d_aliases,
            entry.aliases_count
        )

        try {
            holder.mColorIndicator.setColorFilter(Color.parseColor(entry.colour))
        } catch (e: Exception) {
            // fallback
        }
    }

    override fun getItemCount(): Int = listWithLabels.size

    fun setClickListener(aClickListener: ClickListener) {
        onManageLabelsClicker = aClickListener
    }

    fun getList(): ArrayList<Labels> {
        return listWithLabels
    }

    interface ClickListener {
        fun onClickDelete(pos: Int, aView: View, id: String)
        fun onClickEdit(pos: Int, aView: View, label: Labels)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        var mCV: MaterialCardView = view.findViewById(R.id.manage_labels_recyclerview_list_CV)
        private var mOptionsButton: MaterialButton =
            view.findViewById(R.id.manage_labels_recyclerview_list_delete_button)
        var mName: TextView = view.findViewById(R.id.manage_labels_recyclerview_list_name)
        var mColorIndicator: ImageView =
            view.findViewById(R.id.manage_labels_recyclerview_list_color_indicator)
        var mColorText: TextView =
            view.findViewById(R.id.manage_labels_recyclerview_list_color_text)

        init {
            mOptionsButton.setOnClickListener(this)
            mCV.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.manage_labels_recyclerview_list_delete_button -> {
                    onManageLabelsClicker.onClickDelete(adapterPosition, p0, listWithLabels[adapterPosition].id)
                }

                R.id.manage_labels_recyclerview_list_CV -> {
                    onManageLabelsClicker.onClickEdit(adapterPosition, p0, listWithLabels[adapterPosition])
                }
            }
        }
    }
}
