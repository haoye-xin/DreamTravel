package com.dreamtravel.ui.todos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dreamtravel.data.model.RegionNode
import com.dreamtravel.databinding.ItemRegionBinding

class RegionAdapter(
    private val onItemClick: (RegionNode) -> Unit
) : ListAdapter<RegionNode, RegionAdapter.ViewHolder>(DiffCallback) {

    private var selectedCode: String? = null

    fun setSelected(code: String?) {
        val old = selectedCode
        selectedCode = code
        old?.let { notifyItemChangedByCode(it) }
        code?.let { notifyItemChangedByCode(it) }
    }

    private fun notifyItemChangedByCode(code: String) {
        val index = currentList.indexOfFirst { it.code == code }
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRegionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).code == selectedCode)
    }

    class ViewHolder(
        private val binding: ItemRegionBinding,
        private val onItemClick: (RegionNode) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: RegionNode? = null

        init {
            binding.root.setOnClickListener {
                currentItem?.let { onItemClick(it) }
            }
        }

        fun bind(item: RegionNode, isSelected: Boolean) {
            currentItem = item
            binding.tvRegionName.text = item.name
            binding.ivSelected.visibility = if (isSelected) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RegionNode>() {
        override fun areItemsTheSame(oldItem: RegionNode, newItem: RegionNode): Boolean =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: RegionNode, newItem: RegionNode): Boolean =
            oldItem == newItem
    }
}
