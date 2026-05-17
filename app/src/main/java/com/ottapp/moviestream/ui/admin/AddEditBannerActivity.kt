package com.ottapp.moviestream.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ottapp.moviestream.data.model.Banner
import com.ottapp.moviestream.data.repository.BannerRepository
import com.ottapp.moviestream.databinding.ActivityAddEditBannerBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AddEditBannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBannerBinding
    private val repo = BannerRepository()
    private var bannerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bannerId = intent.getStringExtra("banner_id")

        if (bannerId != null) {
            supportActionBar?.title = "ব্যানার এডিট করুন"
            loadExistingBanner(bannerId!!)
        } else {
            supportActionBar?.title = "নতুন ব্যানার যোগ করুন"
        }

        binding.btnSave.setOnClickListener { saveBanner() }
    }

    private fun loadExistingBanner(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val banners = repo.getAllBanners()
                val banner = banners.find { it.id == id }
                banner?.let { populateForm(it) }
            } catch (e: Exception) {
                toast("লোড করতে সমস্যা হয়েছে")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun populateForm(banner: Banner) {
        binding.etTitle.setText(banner.title)
        binding.etImageUrl.setText(banner.imageUrl)
        binding.etCategory.setText(banner.category)
        binding.etRating.setText(if (banner.imdbRating > 0) banner.imdbRating.toString() else "")
        binding.etMovieId.setText(banner.movieId)
        binding.switchFree.isChecked = banner.testMovie
    }

    private fun saveBanner() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.etTitle.error = "শিরোনাম দিন"
            return
        }
        val imageUrl = binding.etImageUrl.text.toString().trim()
        if (imageUrl.isEmpty()) {
            binding.etImageUrl.error = "ছবির URL দিন"
            return
        }

        val rating = binding.etRating.text.toString().toDoubleOrNull() ?: 0.0
        val banner = Banner(
            id         = bannerId ?: "",
            title      = title,
            imageUrl   = imageUrl,
            category   = binding.etCategory.text.toString().trim(),
            imdbRating = rating,
            testMovie  = binding.switchFree.isChecked,
            movieId    = binding.etMovieId.text.toString().trim()
        )

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                if (bannerId != null) {
                    repo.updateBanner(banner)
                    toast("ব্যানার আপডেট হয়েছে ✓")
                } else {
                    repo.addBanner(banner)
                    toast("ব্যানার যোগ হয়েছে ✓")
                }
                finish()
            } catch (e: Exception) {
                toast("সমস্যা হয়েছে: ${e.message}")
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
