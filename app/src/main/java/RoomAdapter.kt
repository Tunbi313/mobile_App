package com.example.quanlyphongtro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RoomAdapter(
    private val items: MutableList<RoomData>,
    private val onClick: (RoomData) -> Unit
) : RecyclerView.Adapter<RoomAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:      TextView  = view.findViewById(R.id.tvRoomName)
        val tvPrice:     TextView  = view.findViewById(R.id.tvPrice)
        val tvArea:      TextView  = view.findViewById(R.id.tvArea)
        val tvFloor:     TextView  = view.findViewById(R.id.tvFloor)
        val tvAmenities: TextView  = view.findViewById(R.id.tvAmenities)
        val ivRoomImage: ImageView = view.findViewById(R.id.ivRoomImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val room = items[position]
        h.tvName.text      = room.name
        h.tvPrice.text     = formatMoney(room.price)
        h.tvArea.text      = "${room.area.toInt()} m²"
        h.tvFloor.text     = room.floor ?: ""
        h.tvAmenities.text = room.amenities ?: room.description ?: ""

        if (!room.room_image_url.isNullOrEmpty()) {
            Glide.with(h.itemView.context)
                .load(room.room_image_url)
                .placeholder(R.drawable.ic_house)
                .error(R.drawable.ic_house)
                .centerCrop()
                .into(h.ivRoomImage)
        } else {
            h.ivRoomImage.setImageResource(R.drawable.ic_house)
        }

        h.itemView.setOnClickListener { onClick(room) }
    }

    fun updateList(newItems: List<RoomData>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
