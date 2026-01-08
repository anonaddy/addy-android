package host.stjin.anonaddy.adapter

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.databinding.ItemNatoBinding
import host.stjin.anonaddy.databinding.ItemNatoLandBinding
import host.stjin.anonaddy.utils.NatoAlphabet

class NatoAdapter(private val dataSet: ArrayList<NatoAlphabet.NatoItem>, private val orientation: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_PORTRAIT = 0
        private const val VIEW_TYPE_LANDSCAPE = 1
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
        val item = dataSet[position]
        when (holder.itemViewType) {
            VIEW_TYPE_LANDSCAPE -> {
                val landscapeHolder = holder as LandscapeViewHolder
                landscapeHolder.binding.itemNatoCharacter.text = item.character.toString()
                landscapeHolder.binding.itemNatoWord.text = item.word

                // Set a different shade of gray for each item
                val gray = 220 - (position * 10) % 100 // More varied shades
                (landscapeHolder.binding.root as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(Color.rgb(gray, gray, gray))

            }
            VIEW_TYPE_PORTRAIT -> {
                val portraitHolder = holder as PortraitViewHolder
                portraitHolder.binding.itemNatoCharacter.text = item.character.toString()
                portraitHolder.binding.itemNatoWord.text = item.word
            }
        }
    }

    override fun getItemCount() = dataSet.size

}
