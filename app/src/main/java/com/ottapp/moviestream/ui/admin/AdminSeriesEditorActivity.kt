package com.ottapp.moviestream.ui.admin

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.*
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

/**
 * Admin Series Editor — Episode Add/Edit করার জন্য
 * AdminMoviesFragment থেকে isSeries=true movie তে "Manage Episodes" button দিয়ে খোলে
 */
class AdminSeriesEditorActivity : AppCompatActivity() {

    private val repo    = MovieRepository()
    private lateinit var movie: Movie
    private var selectedSeason = 0

    private lateinit var rvEpisodes: RecyclerView
    private lateinit var episodeAdapter: AdminEpisodeAdapter
    private lateinit var spinnerSeason: Spinner
    private lateinit var tvSeriesTitle: TextView
    private lateinit var fabAddEpisode: ExtendedFloatingActionButton
    private lateinit var btnAddSeason: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_series_editor)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Series Editor"

        tvSeriesTitle   = findViewById(R.id.tvSeriesEditorTitle)
        rvEpisodes      = findViewById(R.id.rvEpisodesAdmin)
        spinnerSeason   = findViewById(R.id.spinnerSeasonAdmin)
        fabAddEpisode   = findViewById(R.id.fabAddEpisode)
        btnAddSeason    = findViewById(R.id.btnAddSeason)
        progressBar     = findViewById(R.id.progressBarSeriesEditor)

        val movieId = intent.getStringExtra("movieId") ?: run { finish(); return }

        episodeAdapter = AdminEpisodeAdapter(
            onEdit   = { ep, epIndex -> showEpisodeDialog(ep, epIndex) },
            onDelete = { epIndex -> confirmDeleteEpisode(epIndex) }
        )
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter       = episodeAdapter

        loadMovie(movieId)

        fabAddEpisode.setOnClickListener {
            showEpisodeDialog(null, -1)
        }
        btnAddSeason.setOnClickListener {
            addNewSeason()
        }
    }

    private fun loadMovie(movieId: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val m = repo.getMovieById(movieId)
                if (m == null) { toast("মুভি পাওয়া যায়নি"); finish(); return@launch }
                movie = m
                tvSeriesTitle.text = movie.title
                setupSeasonSpinner()
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                toast("লোড করা যায়নি: ${e.message}")
                finish()
            }
        }
    }

    private fun setupSeasonSpinner() {
        val seasons = movie.seasons
        if (seasons.isEmpty()) {
            showEmptyState()
            return
        }
        val seasonLabels = seasons.mapIndexed { i, s ->
            "Season ${s.seasonNumber}: ${s.title.ifBlank { "Season ${s.seasonNumber}" }}"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasonLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSeason.adapter = adapter
        spinnerSeason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSeason = position
                refreshEpisodeList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        refreshEpisodeList()
    }

    private fun refreshEpisodeList() {
        val episodes = movie.seasons.getOrNull(selectedSeason)?.episodes ?: emptyList()
        episodeAdapter.submitList(episodes.toMutableList())
    }

    private fun showEmptyState() {
        toast("এই সিরিজে কোনো Season নেই। প্রথমে Season যোগ করুন।")
    }

    // ── Episode Add/Edit Dialog ───────────────────────────────────────────────

    private fun showEpisodeDialog(episode: Episode?, episodeIndex: Int) {
        val isEdit = episode != null
        val view   = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_episode, null)

        val etNum     = view.findViewById<TextInputEditText>(R.id.etEpisodeNumber)
        val etTitle   = view.findViewById<TextInputEditText>(R.id.etEpisodeTitle)
        val etDesc    = view.findViewById<TextInputEditText>(R.id.etEpisodeDesc)
        val etDur     = view.findViewById<TextInputEditText>(R.id.etEpisodeDuration)
        val etThumb   = view.findViewById<TextInputEditText>(R.id.etEpisodeThumbnail)
        val etStream  = view.findViewById<TextInputEditText>(R.id.etEpisodeStreamUrl)
        val etDl      = view.findViewById<TextInputEditText>(R.id.etEpisodeDownloadUrl)
        val switchFree= view.findViewById<Switch>(R.id.switchEpisodeFree)

        if (isEdit && episode != null) {
            etNum.setText(episode.episodeNumber.toString())
            etTitle.setText(episode.title)
            etDesc.setText(episode.description)
            etDur.setText(episode.duration)
            etThumb.setText(episode.thumbnailUrl)
            etStream.setText(episode.streamUrl)
            etDl.setText(episode.downloadUrl)
            switchFree.isChecked = episode.isFree
        } else {
            val nextNum = (movie.seasons.getOrNull(selectedSeason)?.episodes?.size ?: 0) + 1
            etNum.setText(nextNum.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Episode Edit করুন" else "নতুন Episode যোগ করুন")
            .setView(view)
            .setPositiveButton(if (isEdit) "আপডেট করুন" else "যোগ করুন") { _, _ ->
                val newEp = Episode(
                    episodeNumber = etNum.text.toString().toIntOrNull() ?: 1,
                    title         = etTitle.text.toString().trim(),
                    description   = etDesc.text.toString().trim(),
                    duration      = etDur.text.toString().trim(),
                    thumbnailUrl  = etThumb.text.toString().trim(),
                    streamUrl     = etStream.text.toString().trim(),
                    downloadUrl   = etDl.text.toString().trim(),
                    isFree        = switchFree.isChecked
                )
                if (newEp.streamUrl.isBlank()) {
                    toast("Stream URL দিন"); return@setPositiveButton
                }
                saveEpisode(newEp, episodeIndex)
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun saveEpisode(episode: Episode, episodeIndex: Int) {
        val seasons    = movie.seasons.toMutableList()
        val seasonData = seasons.getOrNull(selectedSeason) ?: return
        val episodes   = seasonData.episodes.toMutableList()

        if (episodeIndex >= 0) {
            episodes[episodeIndex] = episode
        } else {
            episodes.add(episode)
        }

        val updatedSeason   = seasonData.copy(episodes = episodes)
        seasons[selectedSeason] = updatedSeason
        movie = movie.copy(seasons = seasons)

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                repo.updateMovie(movie)
                refreshEpisodeList()
                toast(if (episodeIndex >= 0) "Episode আপডেট হয়েছে ✓" else "Episode যোগ হয়েছে ✓")
            } catch (e: Exception) {
                toast("সমস্যা: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmDeleteEpisode(episodeIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Episode Delete করবেন?")
            .setMessage("এই Episode টি মুছে ফেলা হবে।")
            .setPositiveButton("Delete") { _, _ ->
                val seasons  = movie.seasons.toMutableList()
                val season   = seasons.getOrNull(selectedSeason) ?: return@setPositiveButton
                val episodes = season.episodes.toMutableList()
                if (episodeIndex in episodes.indices) {
                    episodes.removeAt(episodeIndex)
                    seasons[selectedSeason] = season.copy(episodes = episodes)
                    movie = movie.copy(seasons = seasons)
                    progressBar.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        try {
                            repo.updateMovie(movie)
                            refreshEpisodeList()
                            toast("Episode মুছে ফেলা হয়েছে")
                        } catch (e: Exception) {
                            toast("সমস্যা: ${e.message}")
                        } finally { progressBar.visibility = View.GONE }
                    }
                }
            }
            .setNegativeButton("বাতিল", null).show()
    }

    private fun addNewSeason() {
        val view      = LayoutInflater.from(this).inflate(R.layout.dialog_add_season, null)
        val etSeasonTitle = view.findViewById<TextInputEditText>(R.id.etSeasonTitle)
        val etSeasonNum   = view.findViewById<TextInputEditText>(R.id.etSeasonNumber)
        etSeasonNum.setText((movie.seasons.size + 1).toString())

        AlertDialog.Builder(this)
            .setTitle("নতুন Season যোগ করুন")
            .setView(view)
            .setPositiveButton("যোগ করুন") { _, _ ->
                val seasonNum   = etSeasonNum.text.toString().toIntOrNull() ?: (movie.seasons.size + 1)
                val seasonTitle = etSeasonTitle.text.toString().trim().ifBlank { "Season $seasonNum" }
                val newSeason   = Season(seasonNumber = seasonNum, title = seasonTitle, episodes = emptyList())
                val seasons     = movie.seasons.toMutableList().apply { add(newSeason) }
                movie           = movie.copy(seasons = seasons, totalSeasons = seasons.size)
                progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        repo.updateMovie(movie)
                        setupSeasonSpinner()
                        spinnerSeason.setSelection(seasons.size - 1)
                        toast("Season যোগ হয়েছে ✓")
                    } catch (e: Exception) {
                        toast("সমস্যা: ${e.message}")
                    } finally { progressBar.visibility = View.GONE }
                }
            }
            .setNegativeButton("বাতিল", null).show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
