package com.campingapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campingapp.databinding.ItemCampingBinding
import com.campingapp.model.Camping

class CampingAdapter(
    private val onItemClick: (Camping) -> Unit
) : ListAdapter<Camping, CampingAdapter.CampingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampingViewHolder {
        val binding = ItemCampingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CampingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CampingViewHolder(
        private val binding: ItemCampingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(camping: Camping) {
            binding.textName.text = camping.name
            binding.textMunicipality.text = camping.municipality
            binding.textCategory.text = camping.category
            binding.root.setOnClickListener { onItemClick(camping) }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<Camping>() {
        override fun areItemsTheSame(oldItem: Camping, newItem: Camping) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Camping, newItem: Camping) =
            oldItem == newItem
    }
}
