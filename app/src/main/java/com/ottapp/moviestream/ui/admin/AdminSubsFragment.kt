package com.ottapp.moviestream.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.repository.UserRepository
import com.ottapp.moviestream.databinding.FragmentAdminSubsBinding
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SubscriptionRequest(
    val uid: String,
    val transactionId: String,
    val deviceId: String,
    val status: String,
    val submittedAt: Long,
    val expiry: Long
)

class AdminSubsFragment : Fragment() {

    private var _binding: FragmentAdminSubsBinding? = null
    private val binding get() = _binding!!
    private val userRepo = UserRepository()
    private lateinit var adapter: AdminSubAdapter
    private var allSubs: List<SubscriptionRequest> = emptyList()
    private var currentFilter = SubFilter.PENDING

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSubsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminSubAdapter(
            onApprove = { sub -> confirmApprove(sub) },
            onReject = { sub -> confirmReject(sub) }
        )
        binding.rvSubs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSubs.adapter = adapter

        binding.btnFilterSubs.setOnClickListener { showFilterDialog() }
        binding.btnRefreshSubs.setOnClickListener { loadSubs() }
        binding.btnExportSubs.setOnClickListener { shareSubReport() }

        loadSubs()
    }

    private fun loadSubs() {
        _binding?.progressBar?.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase
                    .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                    .reference
                val snapshot = db.child(Constants.DB_SUBSCRIPTIONS).get().await()
                val subs = mutableListOf<SubscriptionRequest>()
                for (child in snapshot.children) {
                    val data = child.value as? Map<*, *> ?: continue
                    subs.add(SubscriptionRequest(
                        uid = child.key ?: "",
                        transactionId = data["transactionId"]?.toString() ?: "",
                        deviceId = data["deviceId"]?.toString() ?: "",
                        status = data["status"]?.toString() ?: "",
                        submittedAt = data["submittedAt"]?.toString()?.toLongOrNull() ?: 0L,
                        expiry = data["expiry"]?.toString()?.toLongOrNull() ?: 0L
                    ))
                }
                if (_binding == null) return@launch
                allSubs = subs.sortedByDescending { it.submittedAt }
                applyFilter()
            } catch (e: Exception) {
                context?.toast("লোড করতে সমস্যা: ${e.message}")
            } finally {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyFilter() {
        if (_binding == null) return
        val filtered = allSubs.filter { sub ->
            when (currentFilter) {
                SubFilter.ALL -> true
                SubFilter.PENDING -> sub.status.equals("PENDING", ignoreCase = true)
                SubFilter.APPROVED -> sub.status.equals("APPROVED", ignoreCase = true)
                SubFilter.REJECTED -> sub.status.equals("REJECTED", ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
        val pending = allSubs.count { it.status.equals("PENDING", ignoreCase = true) }
        val approved = allSubs.count { it.status.equals("APPROVED", ignoreCase = true) }
        val rejected = allSubs.count { it.status.equals("REJECTED", ignoreCase = true) }
        binding.tvTitle.text = "পেমেন্ট রিকোয়েস্ট • Pending $pending • Approved $approved • Rejected $rejected"
    }

    private fun showFilterDialog() {
        val labels = arrayOf("সব", "Pending", "Approved", "Rejected")
        AlertDialog.Builder(requireContext())
            .setTitle("পেমেন্ট ফিল্টার")
            .setItems(labels) { _, which ->
                currentFilter = SubFilter.values()[which]
                binding.btnFilterSubs.text = "ফিল্টার: ${labels[which]}"
                applyFilter()
            }
            .show()
    }

    private fun shareSubReport() {
        val report = buildString {
            appendLine("CineStream Payment Report")
            appendLine("Total Requests: ${allSubs.size}")
            appendLine("Pending: ${allSubs.count { it.status.equals("PENDING", ignoreCase = true) }}")
            appendLine("Approved: ${allSubs.count { it.status.equals("APPROVED", ignoreCase = true) }}")
            appendLine("Rejected: ${allSubs.count { it.status.equals("REJECTED", ignoreCase = true) }}")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CineStream Payment Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }, "রিপোর্ট শেয়ার করুন"))
    }

    private fun confirmApprove(sub: SubscriptionRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("অ্যাপ্রুভ করবেন?")
            .setMessage("ইউজারকে ৩০ দিনের প্রিমিয়াম এক্সেস দেওয়া হবে।")
            .setPositiveButton("হ্যাঁ") { _, _ -> approveSub(sub) }
            .setNegativeButton("না", null)
            .show()
    }

    private fun confirmReject(sub: SubscriptionRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("রিজেক্ট করবেন?")
            .setMessage("ইউজারকে ফ্রি প্ল্যানে ফেরত পাঠানো হবে।")
            .setPositiveButton("হ্যাঁ") { _, _ -> rejectSub(sub) }
            .setNegativeButton("না", null)
            .show()
    }

    private fun approveSub(sub: SubscriptionRequest) {
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase
                    .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                    .reference
                val expiry = System.currentTimeMillis() + Constants.SUBSCRIPTION_DURATION_MS
                userRepo.updateSubscription(sub.uid, Constants.SUB_PREMIUM, expiry)
                db.child(Constants.DB_SUBSCRIPTIONS).child(sub.uid).updateChildren(mapOf(
                    "status" to "APPROVED",
                    "expiry" to expiry
                )).await()
                context?.toast("অ্যাপ্রুভ সফল হয়েছে")
                if (_binding != null) loadSubs()
            } catch (e: Exception) {
                context?.toast("অ্যাপ্রুভ করতে সমস্যা: ${e.message}")
            }
        }
    }

    private fun rejectSub(sub: SubscriptionRequest) {
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase
                    .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                    .reference
                userRepo.updateSubscription(sub.uid, Constants.SUB_FREE, 0L)
                db.child(Constants.DB_SUBSCRIPTIONS).child(sub.uid).updateChildren(mapOf(
                    "status" to "REJECTED"
                )).await()
                context?.toast("রিজেক্ট সফল হয়েছে")
                if (_binding != null) loadSubs()
            } catch (e: Exception) {
                context?.toast("রিজেক্ট করতে সমস্যা: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class SubFilter {
        ALL,
        PENDING,
        APPROVED,
        REJECTED
    }
}
