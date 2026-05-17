package com.ottapp.moviestream.ui.admin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.tabs.TabLayout
import com.ottapp.moviestream.R
import com.ottapp.moviestream.databinding.ActivityAdminBinding
import com.ottapp.moviestream.util.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val fragmentCache = mutableMapOf<Int, Fragment>()

    data class TabInfo(val index: Int, val title: String, val iconRes: Int, val fragmentId: Int)

    private val tabs = listOf(
        TabInfo(0, "মুভি",      R.drawable.ic_nav_movies,  0),
        TabInfo(1, "ব্যানার",   R.drawable.ic_nav_home,    1),
        TabInfo(2, "রিলস",      R.drawable.ic_nav_reels,   2),
        TabInfo(3, "ইউজার",     R.drawable.ic_nav_profile, 3),
        TabInfo(4, "পেমেন্ট",  R.drawable.ic_nav_download,4),
        TabInfo(5, "অভিনেতা",  R.drawable.ic_nav_profile, 5),
        TabInfo(6, "আপডেট",    R.drawable.ic_nav_home,    6)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "মাস্টার কন্ট্রোল"
        } catch (e: Exception) {
            Log.w("AdminActivity", "Toolbar: ${e.message}")
        }

        tabs.forEach { tab ->
            val t = binding.adminTabLayout.newTab()
            t.text = tab.title
            try { t.setIcon(tab.iconRes) } catch (e: Exception) { /* icon optional */ }
            binding.adminTabLayout.addTab(t)
        }

        binding.adminTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val index = tab?.position ?: 0
                loadFragment(index)
                val title = tabs.getOrNull(index)?.title ?: "মাস্টার কন্ট্রোল"
                supportActionBar?.title = title
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        if (savedInstanceState == null) {
            loadFragment(0)
        }
        refreshTabBadges()
    }

    private fun loadFragment(index: Int) {
        val target: Fragment = fragmentCache.getOrPut(index) {
            when (index) {
                0 -> AdminMoviesFragment()
                1 -> AdminBannersFragment()
                2 -> AdminReelsFragment()
                3 -> AdminUsersFragment()
                4 -> AdminSubsFragment()
                5 -> AdminActorsFragment()
                6 -> AdminUpdateFragment()
                else -> AdminMoviesFragment()
            }
        }

        try {
            val tx = supportFragmentManager.beginTransaction()
            if (!target.isAdded) {
                fragmentCache.values.forEach { f ->
                    if (f !== target && f.isAdded && !f.isHidden) tx.hide(f)
                }
                tx.add(R.id.admin_fragment_container, target, "admin_frag_$index")
            } else {
                fragmentCache.values.forEach { f ->
                    if (f !== target && f.isAdded && !f.isHidden) tx.hide(f)
                }
                if (target.isHidden) tx.show(target)
            }
            tx.commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Fragment error: ${e.message}", e)
            Toast.makeText(this, "সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        refreshTabBadges()
    }

    private fun refreshTabBadges() {
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase
                    .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                    .reference
                val moviesCount = db.child(Constants.DB_MOVIES).get().await().childrenCount
                val bannersCount = db.child(Constants.DB_BANNERS).get().await().childrenCount
                val reelsCount = db.child(Constants.DB_REELS).get().await().childrenCount
                val usersSnapshot = db.child(Constants.DB_USERS).get().await()
                val premiumCount = usersSnapshot.children.count {
                    val data = it.value as? Map<*, *> ?: return@count false
                    data["subscriptionStatus"]?.toString() == Constants.SUB_PREMIUM
                }
                val pendingPayments = db.child(Constants.DB_SUBSCRIPTIONS).get().await().children.count {
                    val data = it.value as? Map<*, *> ?: return@count false
                    data["status"]?.toString()?.equals("PENDING", ignoreCase = true) == true
                }

                binding.adminTabLayout.getTabAt(0)?.text = "মুভি ($moviesCount)"
                binding.adminTabLayout.getTabAt(1)?.text = "ব্যানার ($bannersCount)"
                binding.adminTabLayout.getTabAt(2)?.text = "রিলস ($reelsCount)"
                binding.adminTabLayout.getTabAt(3)?.text = "ইউজার (${usersSnapshot.childrenCount})"
                binding.adminTabLayout.getTabAt(4)?.text = "পেমেন্ট ($pendingPayments)"
                binding.adminTabLayout.getTabAt(5)?.text = "অভিনেতা"
                if (premiumCount > 0) {
                    supportActionBar?.subtitle = "Premium users: $premiumCount"
                }
            } catch (e: Exception) {
                Log.w("AdminActivity", "Tab badge refresh failed: ${e.message}")
            }
        }
    }
}
