package host.stjin.anonaddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.SearchResultRecyclerviewListItemBinding

class SearchDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}

class SearchAdapter(
    listWithRecentSearches: List<String> = emptyList()
) : ListAdapter<String, SearchAdapter.ViewHolder>(SearchDiffCallback()) {

    lateinit var onSearchResultClicker: ClickListener

    init {
        if (listWithRecentSearches.isNotEmpty()) {
            submitList(listWithRecentSearches)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SearchResultRecyclerviewListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.searchResultRecyclerviewListTitle.text = getItem(position)
    }

    fun setClickListener(aClickListener: ClickListener) {
        onSearchResultClicker = aClickListener
    }

    interface ClickListener {
        fun onClickSearchResult(pos: Int, aView: View)
    }

    inner class ViewHolder(val binding: SearchResultRecyclerviewListItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.searchResultRecyclerviewListLL.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return

            if (p0.id == binding.searchResultRecyclerviewListLL.id) {
                onSearchResultClicker.onClickSearchResult(pos, p0)
            }
        }
    }
}
