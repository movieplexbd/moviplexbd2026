package com.ottapp.moviestream.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ottapp.moviestream.data.model.Actor
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.Season
import com.ottapp.moviestream.data.model.Episode
import com.ottapp.moviestream.data.model.DownloadQuality
import com.ottapp.moviestream.data.repository.ActorRepository
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.databinding.ActivityAddEditMovieBinding
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.toast
import com.ottapp.moviestream.data.remote.TmdbService
import com.ottapp.moviestream.data.remote.TmdbMovie
import kotlinx.coroutines.launch

class AddEditMovieActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditMovieBinding
    private val repo = MovieRepository()
    private val actorRepo = ActorRepository()
    private val tmdbService = TmdbService()
    private var movieId: String? = null
    private var allActors = listOf<Actor>()
    private var selectedActorIds = mutableListOf<String>()

    private val categories = listOf("Bangla Dubbed", "Hindi Dubbed", "English")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter

        movieId = intent.getStringExtra("movie_id")

        if (movieId != null) {
            supportActionBar?.title = "মুভি এডিট করুন"
            loadExistingMovie(movieId!!)
        } else {
            supportActionBar?.title = "নতুন মুভি যোগ করুন"
        }

        binding.btnSave.setOnClickListener { saveMovie() }
        binding.btnSelectActors.setOnClickListener { showActorSelectionDialog() }
        binding.btnFetchMetadata.setOnClickListener { fetchMetadata() }
        loadAllActors()

        // Show hint about test movie limit
        binding.switchFree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toast("সতর্কতা: সর্বোচ্চ ${Constants.MAX_TEST_MOVIES}টি ফ্রি মুভি রাখা যাবে")
            }
        }
    }

    private fun loadExistingMovie(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val movie = repo.getMovieById(id)
                movie?.let { populateForm(it) }
            } catch (e: Exception) {
                toast("লোড করতে সমস্যা হয়েছে")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun populateForm(movie: Movie) {
        binding.etTitle.setText(movie.title)
        binding.etDescription.setText(movie.description)
        binding.etBannerUrl.setText(movie.bannerImageUrl)
        binding.etDetailThumbnailUrl.setText(movie.detailThumbnailUrl)
        binding.etVideoUrl.setText(movie.videoStreamUrl)
        try { binding.etTrailerUrl?.setText(movie.trailerUrl) } catch (e: Exception) {}
        try { binding.etGenre?.setText(movie.genre) } catch (e: Exception) {}
        try { binding.switchSeries?.isChecked = movie.isSeries } catch (e: Exception) {}
        binding.etYear.setText(if (movie.year > 0) movie.year.toString() else "")
        binding.etDuration.setText(movie.duration)
        binding.etRating.setText(if (movie.imdbRating > 0) movie.imdbRating.toString() else "")

        val idx = categories.indexOf(movie.category)
        if (idx >= 0) binding.spinnerCategory.setSelection(idx)

        binding.switchTrending.isChecked = movie.trending
        binding.switchFree.isChecked     = movie.testMovie
        
        selectedActorIds = movie.actorIds.toMutableList()
        updateSelectedActorsText()

        // Populate downloads
        movie.downloads.forEach { dl ->
            when (dl.quality) {
                "360p" -> {
                    binding.etUrl360.setText(dl.url)
                    binding.etSize360.setText(dl.size)
                }
                "480p" -> {
                    binding.etUrl480.setText(dl.url)
                    binding.etSize480.setText(dl.size)
                }
                "1080p" -> {
                    binding.etUrl1080.setText(dl.url)
                    binding.etSize1080.setText(dl.size)
                }
            }
        }
    }

    private fun loadAllActors() {
        lifecycleScope.launch {
            try {
                allActors = actorRepo.getAllActors()
                updateSelectedActorsText()
            } catch (e: Exception) {
                toast("অভিনেতা লোড করতে সমস্যা হয়েছে")
            }
        }
    }

    private fun showActorSelectionDialog() {
        if (allActors.isEmpty()) {
            toast("আগে অভিনেতা যোগ করুন")
            return
        }

        val actorNames = allActors.map { it.name }.toTypedArray()
        val checkedItems = allActors.map { selectedActorIds.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("অভিনেতা নির্বাচন করুন")
            .setMultiChoiceItems(actorNames, checkedItems) { _, which, isChecked ->
                val actorId = allActors[which].id
                if (isChecked) {
                    if (!selectedActorIds.contains(actorId)) selectedActorIds.add(actorId)
                } else {
                    selectedActorIds.remove(actorId)
                }
            }
            .setPositiveButton("ঠিক আছে") { _, _ -> updateSelectedActorsText() }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun updateSelectedActorsText() {
        if (selectedActorIds.isEmpty()) {
            binding.tvSelectedActors.text = "কোনো অভিনেতা সিলেক্ট করা নেই"
        } else {
            val names = allActors.filter { selectedActorIds.contains(it.id) }.map { it.name }
            binding.tvSelectedActors.text = if (names.isEmpty()) "সিলেক্ট করা হয়েছে (${selectedActorIds.size})" else names.joinToString(", ")
        }
    }

    private fun fetchMetadata() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.etTitle.error = "আগে মুভির নাম লিখুন"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnFetchMetadata.isEnabled = false

        lifecycleScope.launch {
            try {
                val tmdbMovie = tmdbService.searchMovie(title)
                if (tmdbMovie != null) {
                    applyMetadata(tmdbMovie)
                    toast("মেটাডাটা লোড হয়েছে ✓")
                } else {
                    toast("মুভি খুঁজে পাওয়া যায়নি")
                }
            } catch (e: Exception) {
                toast("ত্রুটি: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnFetchMetadata.isEnabled = true
            }
        }
    }

    private fun applyMetadata(tmdb: TmdbMovie) {
        binding.etTitle.setText(tmdb.title)
        binding.etDescription.setText(tmdb.description)
        binding.etBannerUrl.setText(tmdb.posterUrl)
        binding.etDetailThumbnailUrl.setText(tmdb.backdropUrl)
        binding.etYear.setText(tmdb.year.toString())
        binding.etRating.setText(tmdb.rating.toString())
        binding.etDuration.setText(tmdb.duration)
        
        // Auto-add actors if they exist in our database
        if (tmdb.cast.isNotEmpty()) {
            val newActorIds = mutableListOf<String>()
            tmdb.cast.forEach { castMember ->
                val existingActor = allActors.find { it.name.equals(castMember.name, ignoreCase = true) }
                if (existingActor != null) {
                    newActorIds.add(existingActor.id)
                }
            }
            if (newActorIds.isNotEmpty()) {
                selectedActorIds = newActorIds
                updateSelectedActorsText()
            }
        }
    }

    private fun saveMovie() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.etTitle.error = "শিরোনাম দিন"
            return
        }
        val videoUrl = binding.etVideoUrl.text.toString().trim()
        if (videoUrl.isEmpty()) {
            binding.etVideoUrl.error = "ভিডিও URL দিন"
            return
        }

        val category  = categories.getOrElse(binding.spinnerCategory.selectedItemPosition) { "" }
        val rating    = binding.etRating.text.toString().toDoubleOrNull() ?: 0.0
        val year      = binding.etYear.text.toString().toIntOrNull() ?: 0
        val isTestMov = binding.switchFree.isChecked

        // Collect downloads
        val downloads = mutableListOf<DownloadQuality>()
        val url360 = binding.etUrl360.text.toString().trim()
        if (url360.isNotEmpty()) downloads.add(DownloadQuality("360p", url360, binding.etSize360.text.toString().trim()))
        
        val url480 = binding.etUrl480.text.toString().trim()
        if (url480.isNotEmpty()) downloads.add(DownloadQuality("480p", url480, binding.etSize480.text.toString().trim()))
        
        val url1080 = binding.etUrl1080.text.toString().trim()
        if (url1080.isNotEmpty()) downloads.add(DownloadQuality("1080p", url1080, binding.etSize1080.text.toString().trim()))

        val trailerUrl = try { binding.etTrailerUrl?.text?.toString()?.trim() ?: "" } catch (e: Exception) { "" }
        val genre      = try { binding.etGenre?.text?.toString()?.trim() ?: "" } catch (e: Exception) { "" }
        val isSeries   = try { binding.switchSeries?.isChecked ?: false } catch (e: Exception) { false }

        val movie = Movie(
            id             = movieId ?: "",
            title          = title,
            description    = binding.etDescription.text.toString().trim(),
            bannerImageUrl = binding.etBannerUrl.text.toString().trim(),
            detailThumbnailUrl = binding.etDetailThumbnailUrl.text.toString().trim(),
            videoStreamUrl = videoUrl,
            downloadUrl    = downloads.firstOrNull()?.url ?: "",
            category       = category,
            imdbRating     = rating,
            year           = year,
            duration       = binding.etDuration.text.toString().trim(),
            trending       = binding.switchTrending.isChecked,
            testMovie      = isTestMov,
            actorIds       = selectedActorIds,
            downloads      = downloads,
            trailerUrl     = trailerUrl,
            genre          = genre,
            isSeries       = isSeries
        )

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // ─── Critical: enforce max 2 test movies ───────────────────
                if (isTestMov) {
                    val allMovies = repo.getAllMovies()
                    // Count test movies EXCLUDING the current movie being edited
                    val existingTestCount = allMovies.count { m ->
                        m.testMovie && m.id != (movieId ?: "")
                    }
                    if (existingTestCount >= Constants.MAX_TEST_MOVIES) {
                        toast("❌ সর্বোচ্চ ${Constants.MAX_TEST_MOVIES}টি ফ্রি (Test) মুভি রাখা যাবে। আগে অন্য একটি মুভির ফ্রি টগল বন্ধ করুন।")
                        binding.btnSave.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        return@launch
                    }
                }
                // ──────────────────────────────────────────────────────────

                if (movieId != null) {
                    repo.updateMovie(movie)
                    toast("মুভি আপডেট হয়েছে ✓")
                } else {
                    repo.addMovie(movie)
                    toast("মুভি যোগ হয়েছে ✓")
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
