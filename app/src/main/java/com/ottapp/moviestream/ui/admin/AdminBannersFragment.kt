package com.ottapp.moviestream.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ottapp.moviestream.data.model.Banner
import com.ottapp.moviestream.data.repository.BannerRepository
import com.ottapp.moviestream.databinding.FragmentAdminBannersBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AdminBannersFragment : Fragment() {

    private var _binding: FragmentAdminBannersBinding? = null
    private val binding get() = _binding!!
    private val repo = BannerRepository()
    private lateinit var adapter: AdminBannerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBannersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminBannerAdapter(
            onEdit   = { banner -> openAddEdit(banner) },
            onDelete = { banner -> confirmDelete(banner) }
        )
        binding.rvBanners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBanners.adapter = adapter

        binding.fabAdd.setOnClickListener { openAddEdit(null) }

        loadBanners()
    }

    private fun loadBanners() {
        _binding?.progressBar?.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val banners = repo.getAllBanners()
                if (_binding == null) return@launch
                adapter.submitList(banners)
            } catch (e: Exception) {
                context?.toast("লোড করতে সমস্যা: ${e.message}")
            } finally {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openAddEdit(banner: Banner?) {
        val intent = Intent(requireContext(), AddEditBannerActivity::class.java)
        if (banner != null) intent.putExtra("banner_id", banner.id)
        startActivity(intent)
    }

    private fun confirmDelete(banner: Banner) {
        AlertDialog.Builder(requireContext())
            .setTitle("ব্যানার মুছবেন?")
            .setMessage("ব্যানারটি মুছে ফেলা হবে। নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন") { _, _ -> deleteBanner(banner) }
            .setNegativeButton("না", null)
            .show()
    }

    private fun deleteBanner(banner: Banner) {
        lifecycleScope.launch {
            try {
                repo.deleteBanner(banner.id)
                context?.toast("মুছে ফেলা হয়েছে")
                if (_binding != null) loadBanners()
            } catch (e: Exception) {
                context?.toast("মুছতে সমস্যা: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBanners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
