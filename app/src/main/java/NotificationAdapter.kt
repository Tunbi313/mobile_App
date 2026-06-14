package com.example.quanlyphongtro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    val items: MutableList<NotificationData>,
    private val onClick: (NotificationData) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    fun updateList(newItems: List<NotificationData>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card:     CardView  = view.findViewById(R.id.cvIcon) // reusing cvIcon as parent proxy
        val root:     CardView  = view as CardView
        val ivIcon:   ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle:  TextView  = view.findViewById(R.id.tvTitle)
        val tvTime:   TextView  = view.findViewById(R.id.tvTime)
        val tvBody:   TextView  = view.findViewById(R.id.tvBody)
        val dotUnread: View     = view.findViewById(R.id.dotUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]

        h.tvTitle.text = item.title
        h.tvBody.text  = item.body
        h.tvTime.text  = item.time_display ?: ""

        // Unread dot
        h.dotUnread.visibility = if (!item.is_read) View.VISIBLE else View.GONE

        // Card background: unread = white, read = gray
        h.root.setCardBackgroundColor(
            if (item.is_read) 0xFFE0E0E0.toInt() else 0xFFFFFFFF.toInt()
        )
        h.root.cardElevation = if (item.is_read) 0f else 4f

        // Icon + color based on type
        val (iconRes, iconColor, bgColor) = when (item.notif_type) {
            "payment_due"     -> Triple(R.drawable.ic_warning,  0xFFE53935.toInt(), 0xFFFFEBEE.toInt())
            "payment_success" -> Triple(R.drawable.ic_payment,  0xFF22C55E.toInt(), 0xFFE8F5E9.toInt())
            "new_invoice"     -> Triple(R.drawable.ic_payment,  0xFF1976D2.toInt(), 0xFFE3F2FD.toInt())
            "contract"        -> Triple(R.drawable.ic_manage,   0xFFFFFFFF.toInt(), 0xFF1565C0.toInt())
            else              -> Triple(R.drawable.ic_bell,     0xFFFFFFFF.toInt(), 0xFF1565C0.toInt())
        }
        h.ivIcon.setImageResource(iconRes)
        h.ivIcon.setColorFilter(iconColor)
        h.card.setCardBackgroundColor(bgColor)

        // Unread → bold title
        h.tvTitle.setTypeface(null,
            if (!item.is_read) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        h.root.setOnClickListener {
            if (!item.is_read) {
                items[position] = item.copy(is_read = true)
                notifyItemChanged(position)
            }
            onClick(item)
        }
    }
}
