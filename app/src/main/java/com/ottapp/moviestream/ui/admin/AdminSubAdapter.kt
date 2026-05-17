package com.ottapp.moviestream.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.databinding.ItemAdminSubBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminSubAdapter(
    private val onApprove: (SubscriptionRequest) -> Unit,
    private val onReject: (SubscriptionRequest) -> Unit
) : ListAdapter<SubscriptionRequest, AdminSubAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemAdminSubBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(sub: SubscriptionRequest) {
            b.tvTransactionId.text = "Transaction ID: ${sub.transactionId}"
            b.tvUid.text = "UID: ${sub.uid}"
            b.tvStatus.text = sub.status.uppercase()
            
            if (sub.submittedAt > 0) {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                b.tvSubmittedAt.text = "Submitted At: ${sdf.format(Date(sub.submittedAt))}"
            } else {
                b.tvSubmittedAt.text = "Submitted At: Unknown"
            }

            val isPending = sub.status.equals("PENDING", ignoreCase = true)
            b.btnApprove.visibility = if (isPending) View.VISIBLE else View.GONE
            b.btnReject.visibility = if (isPending) View.VISIBLE else View.GONE
            b.btnApprove.setOnClickListener { onApprove(sub) }
            b.btnReject.setOnClickListener { onReject(sub) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAdminSubBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<SubscriptionRequest>() {
        override fun areItemsTheSame(a: SubscriptionRequest, b: SubscriptionRequest) = a.uid == b.uid
        override fun areContentsTheSame(a: SubscriptionRequest, b: SubscriptionRequest) = a == b
    }
}
