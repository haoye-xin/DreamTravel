package com.dreamtravel.ui.places

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dreamtravel.data.model.Place

class PlaceAdapter(
    private var places: List<Place>,
    private val onItemClick: (Place) -> Unit,
    private val onDeleteClick: (Place) -> Unit,
    private val onToggleActive: (Place, Boolean) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]
        holder.bind(place)
    }

    override fun getItemCount() = places.size

    fun updateData(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val view: android.view.View
    ) : RecyclerView.ViewHolder(view) {

        fun bind(place: Place) {
            val text1 = view.findViewById<android.widget.TextView>(android.R.id.text1)
            val text2 = view.findViewById<android.widget.TextView>(android.R.id.text2)

            text1.text = place.name
            text2.text = "驻留 ${place.dwellMinutes} 分钟 | ${place.pendingCount}/${place.totalCount} 待办"

            view.setOnClickListener { onItemClick(place) }
            view.setOnLongClickListener {
                onDeleteClick(place)
                true
            }
        }
    }
}
