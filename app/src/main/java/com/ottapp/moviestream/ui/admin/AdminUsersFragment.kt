package com.ottapp.moviestream.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.model.UserDevice
import com.ottapp.moviestream.data.model.UserActivity
import com.ottapp.moviestream.data.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.*
import com.ottapp.moviestream.databinding.FragmentAdminUsersBinding
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!
    private val userRepo = UserRepository()
    private lateinit var adapter: AdminUserAdapter
    private var allUsers: List<User> = emptyList()
    private var currentQuery = ""
    private var currentFilter = UserFilter.ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminUserAdapter(
            onBlock = { user -> confirmBlock(user) },
            onMakePremium = { user -> confirmPremium(user) },
            onRemovePremium = { user -> confirmRemovePremium(user) },
            onExtend = { user -> showExtendDialog(user) },
            onResetPassword = { user -> confirmResetPassword(user) },
            onManageDevices = { user -> showDeviceManagement(user) },
            onViewActivity = { user -> showUserActivity(user) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnRefreshUsers.setOnClickListener { loadUsers() }
        binding.btnFilterUsers.setOnClickListener { showFilterDialog() }
        binding.btnExportUsers.setOnClickListener { shareUserReport() }

        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase.getInstance("https://movies-bee24-default-rtdb.firebaseio.com").reference
                val snapshot = db.child(Constants.DB_USERS).get().await()
                val users = mutableListOf<User>()
                for (child in snapshot.children) {
                    val data = child.value as? Map<*, *> ?: continue
                    users.add(User(
                        uid = child.key ?: "",
                        email = data["email"]?.toString() ?: "",
                        displayName = data["displayName"]?.toString() ?: "",
                        photoUrl = data["photoUrl"]?.toString() ?: "",
                        subscriptionStatus = data["subscriptionStatus"]?.toString() ?: User.PLAN_FREE,
                        subscriptionExpiry = data["subscriptionExpiry"]?.toString()?.toLongOrNull() ?: 0L,
                        devices = (data["devices"] as? Map<String, Any>)?.mapValues { entry ->
                            val d = entry.value as Map<*, *>
                            UserDevice(
                                deviceId = d["deviceId"]?.toString() ?: "",
                                deviceName = d["deviceName"]?.toString() ?: "",
                                lastLogin = d["lastLogin"]?.toString()?.toLongOrNull() ?: 0L,
                                isActive = d["isActive"]?.toString()?.toBoolean() ?: true
                            )
                        } ?: emptyMap(),
                        activityLogs = (data["activityLogs"] as? Map<String, Any>)?.mapValues { entry ->
                            val a = entry.value as Map<*, *>
                            UserActivity(
                                id = entry.key,
                                movieId = a["movieId"]?.toString() ?: "",
                                movieTitle = a["movieTitle"]?.toString() ?: "",
                                timestamp = a["timestamp"]?.toString()?.toLongOrNull() ?: 0L,
                                durationWatched = a["durationWatched"]?.toString()?.toLongOrNull() ?: 0L,
                                action = a["action"]?.toString() ?: "watch"
                            )
                        } ?: emptyMap()
                    ))
                }
                allUsers = users.sortedBy { it.email.lowercase() }
                updateDashboard()
                applyFilters()
            } catch (e: Exception) {
                requireContext().toast("লোড করতে সমস্যা: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val q = currentQuery.lowercase().trim()
        val searched = if (q.isEmpty()) allUsers else allUsers.filter {
            it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
        val now = System.currentTimeMillis()
        val filtered = searched.filter { user ->
            when (currentFilter) {
                UserFilter.ALL -> true
                UserFilter.PREMIUM -> user.subscriptionStatus == Constants.SUB_PREMIUM
                UserFilter.FREE -> user.subscriptionStatus == Constants.SUB_FREE
                UserFilter.BLOCKED -> user.subscriptionStatus == Constants.SUB_BLOCKED
                UserFilter.EXPIRED -> user.subscriptionStatus == Constants.SUB_PREMIUM && user.subscriptionExpiry in 1 until now
            }
        }
        adapter.submitList(filtered)
        binding.tvCount.text = "মোট ${filtered.size} জন ইউজার"
    }

    private fun updateDashboard() {
        if (_binding == null) return
        val now = System.currentTimeMillis()
        val premium = allUsers.count { it.subscriptionStatus == Constants.SUB_PREMIUM && it.subscriptionExpiry > now }
        val blocked = allUsers.count { it.subscriptionStatus == Constants.SUB_BLOCKED }
        val expired = allUsers.count { it.subscriptionStatus == Constants.SUB_PREMIUM && it.subscriptionExpiry in 1 until now }
        binding.tvTotalUsers.text = allUsers.size.toString()
        binding.tvPremiumUsers.text = premium.toString()
        binding.tvRiskUsers.text = (blocked + expired).toString()
    }

    private fun showFilterDialog() {
        val labels = arrayOf("সব", "Premium", "Free", "Blocked", "Expired")
        AlertDialog.Builder(requireContext())
            .setTitle("ইউজার ফিল্টার")
            .setItems(labels) { _, which ->
                currentFilter = UserFilter.values()[which]
                binding.btnFilterUsers.text = "ফিল্টার: ${labels[which]}"
                applyFilters()
            }
            .show()
    }

    private fun shareUserReport() {
        val now = System.currentTimeMillis()
        val report = buildString {
            appendLine("CineStream User Report")
            appendLine("Total Users: ${allUsers.size}")
            appendLine("Premium Active: ${allUsers.count { it.subscriptionStatus == Constants.SUB_PREMIUM && it.subscriptionExpiry > now }}")
            appendLine("Free Users: ${allUsers.count { it.subscriptionStatus == Constants.SUB_FREE }}")
            appendLine("Blocked Users: ${allUsers.count { it.subscriptionStatus == Constants.SUB_BLOCKED }}")
            appendLine("Expired Premium: ${allUsers.count { it.subscriptionStatus == Constants.SUB_PREMIUM && it.subscriptionExpiry in 1 until now }}")
            appendLine("Pending Users: ${allUsers.count { it.subscriptionStatus == Constants.SUB_PENDING }}")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CineStream User Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }, "রিপোর্ট শেয়ার করুন"))
    }

    private fun confirmBlock(user: User) {
        val isBlocked = user.subscriptionStatus == Constants.SUB_BLOCKED
        val title = if (isBlocked) "আনব্লক করবেন?" else "ব্লক করবেন?"
        val msg = if (isBlocked) "ইউজারকে আনব্লক করা হবে।" else "ইউজারকে ব্লক করা হবে।"

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("হ্যাঁ") { _, _ ->
                updateUserStatus(user.uid, if (isBlocked) Constants.SUB_FREE else Constants.SUB_BLOCKED, 0L)
            }
            .setNegativeButton("না", null)
            .show()
    }

    private fun confirmPremium(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("প্রিমিয়াম করবেন?")
            .setMessage("ইউজারকে ৩০ দিনের প্রিমিয়াম এক্সেস দেওয়া হবে।")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                val expiry = System.currentTimeMillis() + Constants.SUBSCRIPTION_DURATION_MS
                updateUserStatus(user.uid, Constants.SUB_PREMIUM, expiry)
            }
            .setNegativeButton("না", null)
            .show()
    }

    private fun confirmRemovePremium(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("প্রিমিয়াম সরাবেন?")
            .setMessage("ইউজারের প্রিমিয়াম সাবস্ক্রিপশন বাতিল করা হবে এবং ফ্রি প্ল্যানে নামিয়ে আনা হবে।")
            .setPositiveButton("হ্যাঁ, সরাও") { _, _ ->
                updateUserStatus(user.uid, Constants.SUB_FREE, 0L)
            }
            .setNegativeButton("না", null)
            .show()
    }

    private fun showExtendDialog(user: User) {
        val options = arrayOf("7 দিন", "30 দিন", "90 দিন", "365 দিন")
        val days = longArrayOf(7, 30, 90, 365)

        AlertDialog.Builder(requireContext())
            .setTitle("সাবস্ক্রিপশন বাড়ান")
            .setItems(options) { _, which ->
                val currentExpiry = if (user.subscriptionExpiry > System.currentTimeMillis()) user.subscriptionExpiry else System.currentTimeMillis()
                val newExpiry = currentExpiry + (days[which] * 24 * 60 * 60 * 1000L)
                updateUserStatus(user.uid, Constants.SUB_PREMIUM, newExpiry)
            }
            .show()
    }

    private fun confirmResetPassword(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("পাসওয়ার্ড রিসেট?")
            .setMessage("${user.email} এ রিসেট ইমেইল পাঠানো হবে।")
            .setPositiveButton("পাঠাও") { _, _ ->
                FirebaseAuth.getInstance().sendPasswordResetEmail(user.email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) requireContext().toast("রিসেট ইমেইল পাঠানো হয়েছে")
                        else requireContext().toast("ব্যর্থ: ${task.exception?.message}")
                    }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun showDeviceManagement(user: User) {
        if (user.devices.isEmpty()) {
            requireContext().toast("কোনো ডিভাইস লগইন নেই")
            return
        }

        val deviceList = user.devices.values.toList()
        val deviceNames = deviceList.map { "${it.deviceName} (${if (it.isActive) "Active" else "Logged Out"})" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("ডিভাইস ম্যানেজমেন্ট")
            .setItems(deviceNames) { _, which ->
                val device = deviceList[which]
                if (device.isActive) {
                    confirmLogoutDevice(user.uid, device)
                } else {
                    requireContext().toast("এই ডিভাইসটি অলরেডি লগআউট করা")
                }
            }
            .setNegativeButton("বন্ধ করুন", null)
            .show()
    }

    private fun confirmLogoutDevice(uid: String, device: UserDevice) {
        AlertDialog.Builder(requireContext())
            .setTitle("লগআউট করবেন?")
            .setMessage("${device.deviceName} থেকে ইউজারকে লগআউট করে দেওয়া হবে।")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                lifecycleScope.launch {
                    try {
                        userRepo.logoutDevice(uid, device.deviceId)
                        requireContext().toast("লগআউট সফল হয়েছে")
                        loadUsers()
                    } catch (e: Exception) {
                        requireContext().toast("ব্যর্থ: ${e.message}")
                    }
                }
            }
            .setNegativeButton("না", null)
            .show()
    }

    private fun showUserActivity(user: User) {
        if (user.activityLogs.isEmpty()) {
            requireContext().toast("কোনো অ্যাক্টিভিটি নেই")
            return
        }

        val logs = user.activityLogs.values.sortedByDescending { it.timestamp }
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val logStrings = logs.map { 
            "${it.movieTitle}\n${sdf.format(Date(it.timestamp))} | ${it.durationWatched / 1000 / 60} min"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("${user.displayName} এর অ্যাক্টিভিটি")
            .setItems(logStrings, null)
            .setPositiveButton("ঠিক আছে", null)
            .show()
    }

    private fun updateUserStatus(uid: String, status: String, expiry: Long) {
        lifecycleScope.launch {
            try {
                userRepo.updateSubscription(uid, status, expiry)
                requireContext().toast("আপডেট সফল হয়েছে")
                loadUsers()
            } catch (e: Exception) {
                requireContext().toast("আপডেট করতে সমস্যা: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class UserFilter {
        ALL,
        PREMIUM,
        FREE,
        BLOCKED,
        EXPIRED
    }
}
