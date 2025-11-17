package com.example.radioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RadioStationAdapter(
    public var stations: List<RadioStation>,
    private val favoriteManager: FavoriteManager,
    private val onStationClick: (RadioStation) -> Unit,
    private val onFavoriteClick: (RadioStation) -> Unit
) : RecyclerView.Adapter<RadioStationAdapter.StationViewHolder>() {

    private var selectedPosition = -1

    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStationName: TextView = itemView.findViewById(R.id.tvStationName)
        val tvStationDesc: TextView = itemView.findViewById(R.id.tvStationDesc)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        val cardView: CardView = itemView as CardView

        fun bind(station: RadioStation, position: Int) {
            tvStationName.text = station.name
            tvStationDesc.text = station.description

            // Favori ikonunu güncelle
            updateFavoriteIcon(station)

            // Seçili istasyonu vurgula
            if (position == selectedPosition) {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.accent_light)
                )
            } else {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.primary_light)
                )
            }

            // İstasyona tıklama
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onStationClick(station)
            }

            // Favori butonuna tıklama
            btnFavorite.setOnClickListener {
                onFavoriteClick(station)
                updateFavoriteIcon(station)
            }
        }

        private fun updateFavoriteIcon(station: RadioStation) {
            if (favoriteManager.isFavorite(station.id)) {
                btnFavorite.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                btnFavorite.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_radio_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(stations[position], position)
    }

    override fun getItemCount(): Int = stations.size

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = -1
        notifyItemChanged(previousPosition)
    }

    fun setSelectedStation(stationId: Int) {
        val previousPosition = selectedPosition
        selectedPosition = stations.indexOfFirst { it.id == stationId }
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    // Listeyi güncelle (favoriler sıralaması için)
    fun updateStations(newStations: List<RadioStation>) {
        stations = newStations
        notifyDataSetChanged()
    }
}