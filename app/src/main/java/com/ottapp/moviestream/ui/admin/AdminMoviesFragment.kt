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
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.model.UserActivity
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.databinding.FragmentAdminMoviesBinding
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminMoviesFragment : Fragment() {

    private var _binding: FragmentAdminMoviesBinding? = null
    private val binding get() = _binding!!
    private val repo = MovieRepository()
    private lateinit var adapter: AdminMovieAdapter
    private var allMovies: List<Movie> = emptyList()
    private var currentQuery = ""
    private var currentSort = SortMode.NEWEST

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminMovieAdapter(
            onEdit          = { movie -> openAddEdit(movie) },
            onDelete        = { movie -> confirmDelete(movie) },
            onManageSeries  = { movie -> openSeriesEditor(movie) }
        )
        binding.rvMovies.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovies.adapter = adapter

        binding.fabAdd.setOnClickListener { openAddEdit(null) }
        binding.btnRefresh.setOnClickListener { loadMovies() }
        binding.btnSort.setOnClickListener { showSortDialog() }
        binding.btnHealth.setOnClickListener { showHealthDialog() }
        binding.btnExport.setOnClickListener { shareMovieReport() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadMovies()
    }

    private fun loadMovies() {
        _binding?.progressBar?.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                allMovies = repo.getAllMovies()
                if (_binding == null) return@launch
                updateDashboard()
                applyFilters()

                try {
                    val db = FirebaseDatabase
                        .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                        .reference
                    val usersSnapshot = db.child(Constants.DB_USERS).get().await()
                    if (_binding == null) return@launch
                    binding.tvActiveUsers.text = usersSnapshot.childrenCount.toString()
                } catch (e: Exception) {
                    if (_binding != null) binding.tvActiveUsers.text = "—"
                }

            } catch (e: Exception) {
                context?.toast("লোড করতে সমস্যা: ${e.message}")
            } finally {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val q = currentQuery.lowercase().trim()
        val filtered = if (q.isEmpty()) allMovies else allMovies.filter {
            it.title.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.year.toString().contains(q)
        }
        val sorted = when (currentSort) {
            SortMode.NEWEST -> filtered.sortedByDescending { it.year }
            SortMode.RATING -> filtered.sortedByDescending { it.imdbRating }
            SortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortMode.FREE_FIRST -> filtered.sortedWith(compareByDescending<Movie> { it.testMovie }.thenBy { it.title.lowercase() })
            SortMode.PREMIUM_FIRST -> filtered.sortedWith(compareBy<Movie> { it.testMovie }.thenBy { it.title.lowercase() })
        }
        adapter.submitList(sorted)
        binding.tvCount.text = "মোট ${sorted.size}টি মুভি"
    }

    private fun updateDashboard() {
        if (_binding == null) return
        
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase.getInstance("https://movies-bee24-default-rtdb.firebaseio.com").reference
                
                // 1. Total Movies
                binding.tvTotalMovies.text = allMovies.size.toString()
                
                // 2. Revenue (Approximate from approved subscriptions)
                val subsSnapshot = db.child(Constants.DB_SUBSCRIPTIONS).get().await()
                var totalRevenue = 0
                for (child in subsSnapshot.children) {
                    val data = child.value as? Map<*, *> ?: continue
                    if (data["status"]?.toString()?.equals("APPROVED", true) == true) {
                        totalRevenue += 100 // Assuming 100 BDT per sub
                    }
                }
                binding.tvActiveUsers.text = "৳$totalRevenue"
                // Note: tv_active_users is used for revenue now

                // 3. Trending Content (Based on activity logs)
                val usersSnapshot = db.child(Constants.DB_USERS).get().await()
                val movieViews = mutableMapOf<String, Int>()
                for (userChild in usersSnapshot.children) {
                    val userData = userChild.value as? Map<*, *> ?: continue
                    val logs = userData["activityLogs"] as? Map<*, *> ?: continue
                    for (logEntry in logs.values) {
                        val log = logEntry as? Map<*, *> ?: continue
                        val title = log["movieTitle"]?.toString() ?: continue
                        movieViews[title] = movieViews.getOrDefault(title, 0) + 1
                    }
                }
                val topMovie = movieViews.maxByOrNull { it.value }?.key ?: allMovies.maxByOrNull { it.imdbRating }?.title ?: "N/A"
                binding.tvTrendingMovie.text = topMovie

                // 4. Search Insights (Missing Content)
                binding.btnHealth.text = "সার্চ ইনসাইট"
                binding.btnHealth.setOnClickListener { showSearchInsights() }

            } catch (e: Exception) {
                android.util.Log.e("AdminMovies", "Dashboard error: ${e.message}")
            }
        }
    }

    private fun showSearchInsights() {
        lifecycleScope.launch {
            try {
                val db = FirebaseDatabase.getInstance("https://movies-bee24-default-rtdb.firebaseio.com").reference
                val requestsSnapshot = db.child(Constants.DB_REQUESTS).get().await()
                val requests = mutableListOf<String>()
                for (child in requestsSnapshot.children) {
                    val title = child.child("title").value?.toString() ?: continue
                    val count = child.child("count").value?.toString()?.toIntOrNull() ?: 1
                    requests.add("$title (Requested $count times)")
                }

                if (requests.isEmpty()) {
                    requireContext().toast("কোনো সার্চ ডাটা নেই")
                    return@launch
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("ইউজাররা যা খুঁজছে (Search Insights)")
                    .setItems(requests.toTypedArray(), null)
                    .setPositiveButton("ঠিক আছে", null)
                    .show()
            } catch (e: Exception) {
                requireContext().toast("ত্রুটি: ${e.message}")
            }
        }
    }

    private fun showSortDialog() {
        val labels = arrayOf("নতুন আগে", "রেটিং বেশি আগে", "নাম A-Z", "Free আগে", "Premium আগে")
        AlertDialog.Builder(requireContext())
            .setTitle("মুভি সোর্ট করুন")
            .setItems(labels) { _, which ->
                currentSort = SortMode.values()[which]
                binding.btnSort.text = "সোর্ট: ${labels[which]}"
                applyFilters()
            }
            .show()
    }

    private fun showHealthDialog() {
        val missingVideo = allMovies.count { it.videoStreamUrl.isBlank() }
        val missingPoster = allMovies.count { it.bannerImageUrl.isBlank() && it.detailThumbnailUrl.isBlank() }
        val missingDownload = allMovies.count { it.downloadUrl.isBlank() && it.downloads.isEmpty() }
        val noCategory = allMovies.count { it.category.isBlank() }
        val lowRating = allMovies.count { it.imdbRating <= 0.0 }
        val message = """
            Missing video URL: $missingVideo
            Missing poster/banner: $missingPoster
            Missing download: $missingDownload
            Missing category: $noCategory
            Missing/zero rating: $lowRating
        """.trimIndent()
        AlertDialog.Builder(requireContext())
            .setTitle("ডাটা কোয়ালিটি রিপোর্ট")
            .setMessage(message)
            .setPositiveButton("ঠিক আছে", null)
            .show()
    }

    private fun shareMovieReport() {
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val categories = allMovies.groupingBy { it.category.ifBlank { "Unknown" } }.eachCount()
        val report = buildString {
            appendLine("CineStream Movie Report")
            appendLine("Generated: $generatedAt")
            appendLine("Total Movies: ${allMovies.size}")
            appendLine("Free Movies: ${allMovies.count { it.testMovie }}")
            appendLine("Premium Movies: ${allMovies.count { !it.testMovie }}")
            appendLine("Trending Movies: ${allMovies.count { it.trending }}")
            appendLine("Top Rated: ${allMovies.maxByOrNull { it.imdbRating }?.title ?: "N/A"}")
            appendLine()
            appendLine("Categories:")
            categories.forEach { (name, count) -> appendLine("- $name: $count") }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CineStream Movie Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }, "রিপোর্ট শেয়ার করুন"))
    }

    private fun openAddEdit(movie: Movie?) {
        val intent = Intent(requireContext(), AddEditMovieActivity::class.java)
        if (movie != null) intent.putExtra("movie_id", movie.id)
        startActivity(intent)
    }

    private fun confirmDelete(movie: Movie) {
        AlertDialog.Builder(requireContext())
            .setTitle("মুভি মুছবেন?")
            .setMessage("''${movie.title}'' মুছে ফেলা হবে। নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন") { _, _ -> deleteMovie(movie) }
            .setNegativeButton("না", null)
            .show()
    }

    private fun deleteMovie(movie: Movie) {
        lifecycleScope.launch {
            try {
                repo.deleteMovie(movie.id)
                context?.toast("মুছে ফেলা হয়েছে")
                if (_binding != null) loadMovies()
            } catch (e: Exception) {
                context?.toast("মুছতে সমস্যা: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadMovies()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class SortMode {
        NEWEST,
        RATING,
        TITLE,
        FREE_FIRST,
        PREMIUM_FIRST
    }

    private fun openSeriesEditor(movie: com.ottapp.moviestream.data.model.Movie) {
        val intent = Intent(requireContext(), AdminSeriesEditorActivity::class.java).apply {
            putExtra("movieId", movie.id)
        }
        startActivity(intent)
    }

}