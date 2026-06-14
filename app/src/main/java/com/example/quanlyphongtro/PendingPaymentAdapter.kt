

package com.example.quanlyphongtro

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlyphongtro.network.PendingPaymentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PendingPaymentAdapter(
    private var items: List<PendingPaymentData>,
    private val onApprove: (PendingPaymentData) -> Unit,
    private val onReject: (PendingPaymentData) -> Unit,
    private val onViewProof: (String) -> Unit
) : RecyclerView.Adapter<PendingPaymentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomTenant: TextView     = view.findViewById(R.id.tvRoomTenant)
        val tvMonth: TextView          = view.findViewById(R.id.tvMonth)
        val tvAmount: TextView         = view.findViewById(R.id.tvAmount)
        val ivProof: ImageView         = view.findViewById(R.id.ivProof)
        val llPlaceholder: LinearLayout = view.findViewById(R.id.llProofPlaceholder)
        val btnViewProof: LinearLayout  = view.findViewById(R.id.btnViewProof)
        val btnApprove: Button         = view.findViewById(R.id.btnApprove)
        val btnReject: Button          = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_payment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvRoomTenant.text = "${item.room_name} – ${item.tenant_name}"
        holder.tvMonth.text      = "Tháng ${item.month}/${item.year}"
        holder.tvAmount.text     = formatPendingMoney(item.grand_total)

        if (!item.proof_url.isNullOrEmpty()) {
            loadImage(item.proof_url, holder.ivProof, holder.llPlaceholder, holder.btnViewProof)
            holder.btnViewProof.setOnClickListener { onViewProof(item.proof_url) }
        } else {
            holder.llPlaceholder.visibility = View.VISIBLE
            holder.btnViewProof.visibility  = View.GONE
        }

        holder.btnApprove.setOnClickListener { onApprove(item) }
        holder.btnReject.setOnClickListener  { onReject(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<PendingPaymentData>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun loadImage(url: String, ivProof: ImageView, llPlaceholder: LinearLayout, btnView: LinearLayout) {
        llPlaceholder.visibility = View.VISIBLE
        btnView.visibility       = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        ivProof.setImageBitmap(bitmap)
                        llPlaceholder.visibility = View.GONE
                        btnView.visibility       = View.VISIBLE
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    llPlaceholder.visibility = View.VISIBLE
                    btnView.visibility       = View.GONE
                }
            }
        }
    }
}

private fun formatPendingMoney(amount: String?): String {
    if (amount.isNullOrEmpty()) return "0"
    return try {
        val value = amount.toBigDecimal().toLong()
        "%,d".format(value).replace(',', '.')
    } catch (_: Exception) { amount }
}
