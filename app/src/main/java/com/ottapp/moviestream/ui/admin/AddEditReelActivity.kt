package com.ottapp.moviestream.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.Reel
import com.ottapp.moviestream.data.repository.ReelRepository
import com.ottapp.moviestream.databinding.ActivityAddEditReelBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AddEditReelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditReelBinding
    private val repo = ReelRepository(FirebaseDatabase.getInstance(DB_URL).reference)
    private var reelId: String? = null

    companion object {
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditReelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        reelId = intent.getStringExtra("reel_id")

        if (reelId != null) {
            supportActionBar?.title = "রিল এডিট করুন"
            loadExistingReel(reelId!!)
        } else {
            supportActionBar?.title = "নতুন রিল যোগ করুন"
        }

        binding.btnSaveReel.setOnClickListener { saveReel() }
    }

    private fun loadExistingReel(id: String) {
        lifecycleScope.launch {
            try {
                val all = repo.getAllReels()
                val reel = all.find { it.id == id }
                reel?.let { populateForm(it) }
            } catch (e: Exception) {
                toast("লোড করতে সমস্যা হয়েছে")
            }
        }
    }

    private fun populateForm(reel: Reel) {
        binding.etReelTitle.setText(reel.title)
        binding.etReelVideoUrl.setText(reel.videoUrl)
        binding.etReelMovieTitle.setText(reel.movieTitle)
        // Note: We'll add movieId field to layout in next step, but for now we can use movieTitle as a fallback or handle it via code
    }

    private fun saveReel() {
        val title = binding.etReelTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.etReelTitle.error = "শিরোনাম দিন"
            return
        }
        var videoUrl = binding.etReelVideoUrl.text.toString().trim()
        if (videoUrl.isEmpty()) {
            binding.etReelVideoUrl.error = "ভিডিও URL দিন"
            return
        }

        // Normalize YouTube Shorts link if needed
        if (videoUrl.contains("youtube.com/shorts/")) {
            // Ensure it's a clean link
            val regex = Regex("youtube\\.com/shorts/([^?&/]+)")
            val match = regex.find(videoUrl)
            match?.groupValues?.getOrNull(1)?.let { id ->
                videoUrl = "https://www.youtube.com/shorts/$id"
            }
        }

        val reel = Reel(
            id         = reelId ?: "",
            title      = title,
            videoUrl   = videoUrl,
            movieTitle = binding.etReelMovieTitle.text.toString().trim(),
            movieId    = "" // This will be linked manually in DB or we can add a field
        )

        binding.btnSaveReel.isEnabled = false

        lifecycleScope.launch {
            try {
                if (reelId != null) {
                    repo.updateReel(reel)
                    toast("রিল আপডেট হয়েছে ✓")
                } else {
                    repo.addReel(reel)
                    toast("রিল যোগ হয়েছে ✓")
                }
                finish()
            } catch (e: Exception) {
                toast("সমস্যা হয়েছে: ${e.message}")
                binding.btnSaveReel.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
