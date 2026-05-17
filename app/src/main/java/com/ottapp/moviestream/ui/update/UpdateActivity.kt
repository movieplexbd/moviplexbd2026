package com.ottapp.moviestream.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ottapp.moviestream.R
import com.ottapp.moviestream.databinding.ActivityUpdateBinding
import com.ottapp.moviestream.util.GitHubUpdateManager
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

/**
 * UpdateActivity — GitHub Release থেকে update দেখায়
 *
 * Intent extras:
 *   title, message, changelog (ArrayList<String>), downloadLink,
 *   updateType (FORCE/SOFT), currentVersion, latestVersion
 *
 * FORCE update: back button disabled, "Later" hidden
 * SOFT  update: skip করা যায়
 */
class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title          = intent.getStringExtra("title")          ?: "নতুন আপডেট পাওয়া গেছে!"
        val message        = intent.getStringExtra("message")        ?: "Bug fixes & নতুন features"
        val changelog      = intent.getStringArrayListExtra("changelog") ?: arrayListOf()
        val downloadLink   = intent.getStringExtra("downloadLink")   ?: ""
        val updateType     = intent.getStringExtra("updateType")     ?: "SOFT"
        val currentVersion = intent.getStringExtra("currentVersion") ?: ""
        val latestVersion  = intent.getStringExtra("latestVersion")  ?: ""

        // ── UI setup ──────────────────────────────────────────────────────────
        binding.tvUpdateTitle.text = title
        binding.tvUpdateSubtitle.text = message

        if (currentVersion.isNotBlank() && latestVersion.isNotBlank()) {
            binding.tvVersionInfo.text = "আপনার version: v$currentVersion  →  নতুন: v$latestVersion"
            binding.tvVersionInfo.visibility = View.VISIBLE
        } else {
            binding.tvVersionInfo.visibility = View.GONE
        }

        // ── Changelog ─────────────────────────────────────────────────────────
        if (changelog.isNotEmpty()) {
            binding.cvChangelog.visibility = View.VISIBLE
            changelog.filter { it.isNotBlank() }.forEach { item ->
                val tv = TextView(this).apply {
                    text = "• $item"
                    setTextColor(resources.getColor(R.color.t1, null))
                    textSize = 13f
                    setPadding(0, 6, 0, 6)
                }
                binding.llChangelogContainer.addView(tv)
            }
        } else {
            binding.cvChangelog.visibility = View.GONE
        }

        // ── Force vs Soft ──────────────────────────────────────────────────────
        if (updateType == "FORCE") {
            binding.btnLater.visibility = View.GONE
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { /* blocked — FORCE update */ }
            })
        } else {
            binding.btnLater.visibility = View.VISIBLE
            binding.btnLater.text = "পরে করব"
            binding.btnLater.setOnClickListener { finish() }
        }

        // ── Download button ────────────────────────────────────────────────────
        binding.btnUpdateNow.text = "⬇ এখনই আপডেট করুন"
        binding.btnUpdateNow.setOnClickListener {
            if (downloadLink.isBlank()) {
                // downloadLink নেই — GitHub release page এ নিয়ে যাই
                openGitHubReleases()
                return@setOnClickListener
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                toast("ডাউনলোড শুরু হচ্ছে...")
                if (updateType == "FORCE") {
                    // Force update এ app close করি — user ইনস্টল করে আবার খুলবে
                    finish()
                }
            } catch (e: Exception) {
                toast("Browser খুলতে পারছে না। GitHub থেকে ডাউনলোড করুন।")
                openGitHubReleases()
            }
        }

        // ── Re-check button (manual refresh) ──────────────────────────────────
        try {
            binding.btnRecheck?.setOnClickListener {
                binding.btnRecheck.isEnabled = false
                binding.btnRecheck.text = "চেক করছি..."
                lifecycleScope.launch {
                    val mgr  = GitHubUpdateManager(this@UpdateActivity)
                    val info = mgr.checkForUpdate()
                    if (info == null) {
                        toast("আপডেট চেক করা যায়নি। Internet connection দেখুন।")
                    } else if (!info.isUpdateAvailable) {
                        toast("আপনার app সর্বশেষ version এ আছে ✓")
                        if (updateType != "FORCE") finish()
                    } else {
                        toast("v${info.latestVersion} পাওয়া গেছে। উপরের button দিয়ে ডাউনলোড করুন।")
                    }
                    binding.btnRecheck.isEnabled = true
                    binding.btnRecheck.text = "আবার চেক করুন"
                }
            }
        } catch (e: Exception) { /* optional button */ }
    }

    private fun openGitHubReleases() {
        val url = "https://github.com/${GitHubUpdateManager.GITHUB_OWNER}/${GitHubUpdateManager.GITHUB_REPO}/releases/latest"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            toast("Browser পাওয়া যাচ্ছে না")
        }
    }
}
