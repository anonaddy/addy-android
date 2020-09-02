package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.R

class SearchAdapter(
    private val listWithRecentSearches: ArrayList<String>
) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    lateinit var onSearchResultClicker: ClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.search_result_recyclerview_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle.text = listWithRecentSearches[position]
    }

    override fun getItemCount(): Int = listWithRecentSearches.size


    fun setClickListener(aClickListener: ClickListener) {
        onSearchResultClicker = aClickListener
    }


    interface ClickListener {
        fun onClickSearchResult(pos: Int, aView: View)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private var mLL: LinearLayout = view.findViewById(R.id.search_result_recyclerview_list_LL)
        var mTitle: TextView = view.findViewById(R.id.search_result_recyclerview_list_title)

        init {
            mLL.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            when (p0.id) {
                R.id.search_result_recyclerview_list_LL -> {
                    onSearchResultClicker.onClickSearchResult(adapterPosition, p0)
                }
            }
        }

    }
}

