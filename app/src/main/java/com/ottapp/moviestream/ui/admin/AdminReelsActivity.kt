package com.ottapp.moviestream.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.Reel
import com.ottapp.moviestream.data.repository.ReelRepository
import com.ottapp.moviestream.databinding.ActivityAdminReelsBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AdminReelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminReelsBinding
    private val repo = ReelRepository(FirebaseDatabase.getInstance(DB_URL).reference)
    private lateinit var adapter: AdminReelsAdapter

    companion object {
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = AdminReelsAdapter(
            onEdit = { reel ->
                val intent = Intent(this, AddEditReelActivity::class.java)
                intent.putExtra("reel_id", reel.id)
                startActivity(intent)
            },
            onDelete = { reel ->
                showDeleteDialog(reel)
            }
        )

        binding.rvReels.layoutManager = LinearLayoutManager(this)
        binding.rvReels.adapter = adapter

        binding.fabAddReel.setOnClickListener {
            startActivity(Intent(this, AddEditReelActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadReels()
    }

    private fun loadReels() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val reels = repo.getAllReels()
                adapter.submitList(reels)
            } catch (e: Exception) {
                toast("লোড করতে সমস্যা হয়েছে")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDeleteDialog(reel: Reel) {
        AlertDialog.Builder(this)
            .setTitle("রিল ডিলিট")
            .setMessage("আপনি কি নিশ্চিত যে এই রিলটি ডিলিট করতে চান?")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.deleteReel(reel.id)
                        toast("ডিলিট হয়েছে")
                        loadReels()
                    } catch (e: Exception) {
                        toast("ডিলিট করতে সমস্যা হয়েছে")
                    }
                }
            }
            .setNegativeButton("না", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class AdminReelsAdapter(
    private val onEdit: (Reel) -> Unit,
    private val onDelete: (Reel) -> Unit
) : RecyclerView.Adapter<AdminReelsAdapter.ViewHolder>() {

    private var items = listOf<Reel>()

    fun submitList(newList: List<Reel>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reel = items[position]
        holder.text1.text = reel.title
        holder.text2.text = reel.movieTitle
        holder.itemView.setOnClickListener { onEdit(reel) }
        holder.itemView.setOnLongClickListener {
            onDelete(reel)
            true
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val text1: TextView = v.findViewById(android.R.id.text1)
        val text2: TextView = v.findViewById(android.R.id.text2)
    }
}
