package com.ottapp.moviestream.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.Reel
import com.ottapp.moviestream.data.repository.ReelRepository
import com.ottapp.moviestream.databinding.FragmentAdminReelsBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AdminReelsFragment : Fragment() {

    private var _binding: FragmentAdminReelsBinding? = null
    private val binding get() = _binding!!
    private val repo = ReelRepository(FirebaseDatabase.getInstance(DB_URL).reference)
    private lateinit var adapter: AdminReelAdapter

    companion object {
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminReelAdapter(
            onEdit   = { reel -> openAddEdit(reel) },
            onDelete = { reel -> confirmDelete(reel) }
        )
        binding.rvReels.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReels.adapter = adapter

        binding.fabAdd.setOnClickListener { openAddEdit(null) }

        loadReels()
    }

    private fun loadReels() {
        _binding?.progressBar?.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val reels = repo.getAllReels()
                if (_binding == null) return@launch
                adapter.submitList(reels)
            } catch (e: Exception) {
                context?.toast("লোড করতে সমস্যা: ${e.message}")
            } finally {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openAddEdit(reel: Reel?) {
        val intent = Intent(requireContext(), AddEditReelActivity::class.java)
        if (reel != null) intent.putExtra("reel_id", reel.id)
        startActivity(intent)
    }

    private fun confirmDelete(reel: Reel) {
        AlertDialog.Builder(requireContext())
            .setTitle("রিলস মুছবেন?")
            .setMessage("রিলসটি মুছে ফেলা হবে। নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন") { _, _ -> deleteReel(reel) }
            .setNegativeButton("না", null)
            .show()
    }

    private fun deleteReel(reel: Reel) {
        lifecycleScope.launch {
            try {
                repo.deleteReel(reel.id)
                context?.toast("মুছে ফেলা হয়েছে")
                if (_binding != null) loadReels()
            } catch (e: Exception) {
                context?.toast("মুছতে সমস্যা: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadReels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
