package com.ottapp.moviestream.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Banner
import com.ottapp.moviestream.data.repository.BannerRepository
import com.ottapp.moviestream.databinding.ActivityAdminBannersBinding
import com.ottapp.moviestream.databinding.ItemAdminBannerBinding
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AdminBannersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBannersBinding
    private val repo = BannerRepository()
    private lateinit var adapter: AdminBannerAdapter
    private var allBanners: List<Banner> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBannersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "ব্যানার ম্যানেজমেন্ট"

        adapter = AdminBannerAdapter(
            onEdit   = { banner -> openAddEdit(banner) },
            onDelete = { banner -> confirmDelete(banner) }
        )
        binding.rvBanners.layoutManager = LinearLayoutManager(this)
        binding.rvBanners.adapter = adapter

        binding.fabAdd.setOnClickListener { openAddEdit(null) }

        loadBanners()
    }

    private fun loadBanners() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                allBanners = repo.getAllBanners()
                adapter.submitList(allBanners)
                binding.tvCount.text = "মোট ${allBanners.size}টি ব্যানার"
            } catch (e: Exception) {
                toast("লোড করতে সমস্যা: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openAddEdit(banner: Banner?) {
        val intent = Intent(this, AddEditBannerActivity::class.java)
        if (banner != null) intent.putExtra("banner_id", banner.id)
        startActivity(intent)
    }

    private fun confirmDelete(banner: Banner) {
        AlertDialog.Builder(this)
            .setTitle("ব্যানার মুছবেন?")
            .setMessage("''${banner.title}'' মুছে ফেলা হবে। নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন") { _, _ -> deleteBanner(banner) }
            .setNegativeButton("না", null)
            .show()
    }

    private fun deleteBanner(banner: Banner) {
        lifecycleScope.launch {
            try {
                repo.deleteBanner(banner.id)
                toast("মুছে ফেলা হয়েছে")
                loadBanners()
            } catch (e: Exception) {
                toast("মুছতে সমস্যা: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBanners()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

class AdminBannerAdapter(
    private val onEdit:   (Banner) -> Unit,
    private val onDelete: (Banner) -> Unit
) : ListAdapter<Banner, AdminBannerAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemAdminBannerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(banner: Banner) {
            b.tvTitle.text    = banner.title.ifEmpty { "শিরোনাম নেই" }
            b.tvCategory.text = banner.category.ifEmpty { "ক্যাটাগরি নেই" }
            b.tvRating.text   = "⭐ ${banner.imdbRating}"
            b.tvFree.text     = if (banner.testMovie) "ফ্রি" else "প্রিমিয়াম"
            b.tvFree.setBackgroundResource(
                if (banner.testMovie) com.ottapp.moviestream.R.drawable.bg_badge_free
                else com.ottapp.moviestream.R.drawable.bg_premium_badge
            )
            b.ivBanner.loadImage(banner.imageUrl)
            b.btnEdit.setOnClickListener   { onEdit(banner) }
            b.btnDelete.setOnClickListener { onDelete(banner) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAdminBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(a: Banner, b: Banner) = a.id == b.id
        override fun areContentsTheSame(a: Banner, b: Banner) = a == b
    }
}
