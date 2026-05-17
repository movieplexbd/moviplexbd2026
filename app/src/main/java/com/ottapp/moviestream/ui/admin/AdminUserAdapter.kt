package com.ottapp.moviestream.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.databinding.ItemAdminUserBinding
import com.ottapp.moviestream.util.Constants
import java.text.SimpleDateFormat
import java.util.*

class AdminUserAdapter(
    private val onBlock: (User) -> Unit,
    private val onMakePremium: (User) -> Unit,
    private val onRemovePremium: (User) -> Unit,
    private val onExtend: (User) -> Unit,
    private val onResetPassword: (User) -> Unit,
    private val onManageDevices: (User) -> Unit,
    private val onViewActivity: (User) -> Unit
) : ListAdapter<User, AdminUserAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemAdminUserBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(user: User) {
            b.tvName.text = user.displayName.ifEmpty { "নাম নেই" }
            b.tvEmail.text = user.email.ifEmpty { "ইমেইল নেই" }
            b.tvStatus.text = user.subscriptionStatus.uppercase()

            val statusColor = when (user.subscriptionStatus) {
                Constants.SUB_PREMIUM -> R.drawable.bg_premium_badge
                Constants.SUB_PENDING -> R.drawable.bg_offer_badge
                Constants.SUB_BLOCKED -> R.drawable.bg_close_btn
                else -> R.drawable.bg_badge_free
            }
            b.tvStatus.setBackgroundResource(statusColor)

            if (user.subscriptionExpiry > 0) {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                b.tvExpiry.text = "মেয়াদ: ${sdf.format(Date(user.subscriptionExpiry))}"
            } else {
                b.tvExpiry.text = "মেয়াদ: নেই"
            }

            b.btnBlock.text = if (user.subscriptionStatus == Constants.SUB_BLOCKED) "আনব্লক" else "ব্লক"
            b.btnBlock.setOnClickListener { onBlock(user) }

            // Show Make Premium or Remove Premium based on current status
            val isPremium = user.subscriptionStatus == Constants.SUB_PREMIUM
            if (isPremium) {
                b.btnMakePremium.text = "প্রিমিয়াম সরাও"
                b.btnMakePremium.setOnClickListener { onRemovePremium(user) }
            } else {
                b.btnMakePremium.text = "প্রিমিয়াম দাও"
                b.btnMakePremium.setOnClickListener { onMakePremium(user) }
            }

            b.btnExtend.visibility = if (isPremium) View.VISIBLE else View.GONE
            b.btnExtend.setOnClickListener { onExtend(user) }
            b.btnResetPassword.setOnClickListener { onResetPassword(user) }
            
            // New buttons for device and activity
            b.root.findViewById<View>(R.id.btn_manage_devices)?.setOnClickListener { onManageDevices(user) }
            b.root.findViewById<View>(R.id.btn_view_activity)?.setOnClickListener { onViewActivity(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(a: User, b: User) = a.uid == b.uid
        override fun areContentsTheSame(a: User, b: User) = a == b
    }
}
