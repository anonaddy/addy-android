package host.stjin.anonaddy.adapter

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.ItemNatoBinding
import host.stjin.anonaddy.databinding.ItemNatoLandBinding
import host.stjin.anonaddy.utils.NatoAlphabet

class NatoDiffCallback : DiffUtil.ItemCallback<NatoAlphabet.NatoItem>() {
    override fun areItemsTheSame(oldItem: NatoAlphabet.NatoItem, newItem: NatoAlphabet.NatoItem): Boolean =
        oldItem.character == newItem.character

    override fun areContentsTheSame(oldItem: NatoAlphabet.NatoItem, newItem: NatoAlphabet.NatoItem): Boolean =
        oldItem == newItem
}

class NatoAdapter(
    items: List<NatoAlphabet.NatoItem> = emptyList(),
    private val orientation: Int
) : ListAdapter<NatoAlphabet.NatoItem, RecyclerView.ViewHolder>(NatoDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_PORTRAIT = 0
        private const val VIEW_TYPE_LANDSCAPE = 1
    }

    init {
        if (items.isNotEmpty()) {
            submitList(items)
        }
    }

    class PortraitViewHolder(val binding: ItemNatoBinding) : RecyclerView.ViewHolder(binding.root)
    class LandscapeViewHolder(val binding: ItemNatoLandBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) VIEW_TYPE_LANDSCAPE else VIEW_TYPE_PORTRAIT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LANDSCAPE -> {
                val binding = ItemNatoLandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LandscapeViewHolder(binding)
            }

            else -> { // VIEW_TYPE_PORTRAIT
                val binding = ItemNatoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PortraitViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder.itemViewType) {
            VIEW_TYPE_LANDSCAPE -> {
                val landscapeHolder = holder as LandscapeViewHolder
                landscapeHolder.binding.itemNatoCharacter.text = item.character.toString()
                landscapeHolder.binding.itemNatoWord.text = item.word

                // Set a different shade of gray for each item
                val gray = 220 - (position * 10) % 100 // More varied shades
                landscapeHolder.binding.root.setCardBackgroundColor(
                    Color.rgb(
                        gray,
                        gray,
                        gray
                    )
                )

            }

            VIEW_TYPE_PORTRAIT -> {
                val portraitHolder = holder as PortraitViewHolder
                portraitHolder.binding.itemNatoCharacter.text = item.character.toString()
                portraitHolder.binding.itemNatoWord.text = item.word
            }
        }
    }
}
