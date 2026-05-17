package com.ottapp.moviestream.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.ottapp.moviestream.R
import com.ottapp.moviestream.adapter.EpisodeAdapter
import com.ottapp.moviestream.adapter.RelatedMoviesAdapter
import com.ottapp.moviestream.data.model.Actor
import com.ottapp.moviestream.data.model.DownloadQuality
import com.ottapp.moviestream.data.model.Episode
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.Season
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.repository.ActorRepository
import com.ottapp.moviestream.data.repository.DownloadRepository
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.data.repository.UserRepository
import com.ottapp.moviestream.databinding.FragmentDetailContainerBinding
import com.ottapp.moviestream.service.DownloadService
import com.ottapp.moviestream.ui.player.PlayerActivity
import com.ottapp.moviestream.ui.subscription.SubscriptionDialog
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.RecentlyViewedManager
import com.ottapp.moviestream.util.WatchlistManager
import com.ottapp.moviestream.util.hide
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.util.show
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.ottapp.moviestream.ui.watchparty.WatchPartyDialog
import com.ottapp.moviestream.util.ContinueWatchingSyncManager

class MovieDetailFragment : Fragment() {

    private var _binding: FragmentDetailContainerBinding? = null
    private val binding get() = _binding!!

    private val movieRepo = MovieRepository()
    private val actorRepo = ActorRepository()
    private val userRepo  = UserRepository()
    private lateinit var dlRepo: DownloadRepository

    private var currentMovie: Movie? = null
    private lateinit var watchlistManager: WatchlistManager
    private lateinit var recentlyViewedManager: RecentlyViewedManager

    private var episodeAdapter: EpisodeAdapter? = null
    private var relatedAdapter: RelatedMoviesAdapter? = null
    private var hasAccess = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dlRepo = DownloadRepository(requireContext())
        watchlistManager = WatchlistManager(requireContext())
        recentlyViewedManager = RecentlyViewedManager(requireContext())

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val movieId = arguments?.getString(Constants.EXTRA_MOVIE_ID)
        if (movieId.isNullOrEmpty()) {
            requireContext().toast("মুভি পাওয়া যায়নি")
            findNavController().navigateUp()
            return
        }
        loadMovie(movieId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadMovie(movieId: String) {
        binding.progressCenter.show()
        binding.layoutContent.hide()

        lifecycleScope.launch {
            try {
                val movieDeferred = async { movieRepo.getMovieById(movieId) }
                val userDeferred  = async { try { userRepo.getCurrentUser() } catch (e: Exception) { null } }

                val movie = movieDeferred.await()
                val user  = userDeferred.await()
                if (_binding == null) return@launch

                if (movie == null) {
                    requireContext().toast("মুভি পাওয়া যায়নি")
                    findNavController().navigateUp()
                    return@launch
                }

                currentMovie = movie
                try { recentlyViewedManager.addMovie(movie) } catch (e: Exception) { }

                hasAccess = movie.testMovie || user?.hasAccess == true

                binding.progressCenter.hide()
                populateUI(movie, user)
                binding.layoutContent.show()

                // Load related movies in background
                loadRelatedMovies(movie)

            } catch (e: Exception) {
                if (_binding == null) return@launch
                val msg = when {
                    e is java.net.UnknownHostException    -> "ইন্টারনেট সংযোগ নেই"
                    e is java.net.SocketTimeoutException  -> "সার্ভার সাড়া দিচ্ছে না"
                    e.message?.contains("permission", ignoreCase = true) == true -> "অনুমতি নেই, সাইন ইন করুন"
                    else                                  -> "মুভি লোড করতে সমস্যা হয়েছে"
                }
                requireContext().toast(msg)
                findNavController().navigateUp()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Populate UI
    // ─────────────────────────────────────────────────────────────────────────

    private fun populateUI(movie: Movie, user: User?) {
        if (_binding == null) return

        // Hero image + title
        binding.ivBanner.loadImage(movie.detailThumbnailUrl.ifBlank { movie.bannerImageUrl })
        binding.tvTitleOverlay.text = movie.title

        // Badges
        if (movie.testMovie) binding.tvFreeBadge.show() else binding.tvFreeBadge.hide()
        if (movie.isSeries)  binding.tvSeriesBadge.show() else binding.tvSeriesBadge.hide()

        // Meta
        binding.tvRating.text   = "⭐ ${movie.imdbRating}"
        binding.tvCategory.text = movie.category
        binding.tvDescription.text = movie.description
        if (movie.year > 0)           { binding.tvYear.text = movie.year.toString(); binding.tvYear.show() }
        if (movie.duration.isNotBlank()) { binding.tvDuration.text = movie.duration; binding.tvDuration.show() }

        // Genre chips
        setupGenreChips(movie.genreList)

        // Trailer button
        if (movie.trailerUrl.isNotBlank()) {
            binding.btnTrailer.show()
            binding.btnTrailer.setOnClickListener { openPlayer(movie, movie.trailerUrl, isTrailer = true) }
        } else {
            binding.btnTrailer.hide()
        }

        // Cast
        if (movie.actorIds.isNotEmpty()) loadActors(movie.actorIds)
        else binding.layoutActors.hide()

        // Action buttons (watchlist, share, rate)
        setupActionButtons(movie)

        if (hasAccess) {
            binding.layoutLocked.hide()
            if (movie.isSeries) {
                // Series: hide center play, show episodes
                binding.layoutPlayCenter.hide()
                setupSeriesUI(movie)
                binding.layoutDownloads.hide()
            } else {
                // Movie: show play + downloads
                binding.layoutPlayCenter.show()
                setupDownloadButtons(movie)
                binding.layoutSeries.hide()
                binding.btnWatch.setOnClickListener {
                    val url = if (dlRepo.isDownloaded(movie.id)) dlRepo.getLocalFilePath(movie.id) else movie.videoStreamUrl
                    openPlayer(movie, url)
                }
            }
        } else {
            binding.layoutDownloads.hide()
            binding.layoutSeries.hide()
            binding.btnWatch.setOnClickListener { openSubscriptionDialog() }
            binding.layoutLocked.show()
            binding.btnSubscribe.setOnClickListener { openSubscriptionDialog() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Genre chips
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupGenreChips(genres: List<String>) {
        if (genres.isEmpty()) { binding.layoutGenres.hide(); return }
        binding.layoutGenres.show()
        binding.cgGenres.removeAllViews()
        genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.surface2)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.t2))
                textSize = 11f
            }
            binding.cgGenres.addView(chip)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Series UI
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSeriesUI(movie: Movie) {
        if (movie.seasons.isEmpty()) { binding.layoutSeries.hide(); return }
        binding.layoutSeries.show()

        val seasons = movie.seasons
        val seasonLabels = seasons.map { "Season ${it.seasonNumber}: ${it.title.ifBlank { "" }}" }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasonLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeason.adapter = spinnerAdapter

        // Show episodes for selected season
        val showEpisodes = { season: Season ->
            episodeAdapter = EpisodeAdapter(
                hasAccess     = hasAccess,
                onPlay        = { ep -> openEpisode(movie, ep) },
                onLockedClick = { openSubscriptionDialog() },
                onDownload    = { ep -> downloadEpisode(movie, ep) },
                isDownloaded  = { ep -> dlRepo.isDownloaded("${movie.id}_ep${ep.episodeNumber}") }
            )
            binding.rvEpisodes.layoutManager = LinearLayoutManager(requireContext())
            binding.rvEpisodes.adapter = episodeAdapter
            episodeAdapter?.submitList(season.episodes)
        }

        showEpisodes(seasons[0])

        binding.spinnerSeason.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                showEpisodes(seasons[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Related movies
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadRelatedMovies(movie: Movie) {
        lifecycleScope.launch {
            try {
                val related = movieRepo.getRelatedMovies(movie, limit = 12)
                if (_binding == null) return@launch
                if (related.isNotEmpty()) {
                    binding.layoutRelated.show()
                    relatedAdapter = RelatedMoviesAdapter { relMovie ->
                        val bundle = Bundle().apply { putString(Constants.EXTRA_MOVIE_ID, relMovie.id) }
                        try { findNavController().navigate(R.id.movieDetailFragment, bundle) } catch (e: Exception) { }
                    }
                    binding.rvRelated.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.rvRelated.adapter = relatedAdapter
                    relatedAdapter?.submitList(related)
                } else {
                    binding.layoutRelated.hide()
                }
            } catch (e: Exception) {
                binding.layoutRelated.hide()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action buttons
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupActionButtons(movie: Movie) {
        // Watchlist
        val inList = watchlistManager.isInWatchlist(movie.id)
        updateWatchlistIcon(inList)
        binding.btnWatchlistAction.setOnClickListener {
            val added = watchlistManager.toggleWatchlist(movie)
            updateWatchlistIcon(added)
            requireContext().toast(if (added) "ওয়াচলিস্টে যোগ হয়েছে ✓" else "ওয়াচলিস্ট থেকে সরানো হয়েছে")
        }

        // Share with thumbnail (OpenGraph deep link)
        binding.btnShareAction.setOnClickListener {
            shareMovieWithThumbnail(movie)
        }

        // Watch Party — button functionality
        try {
            binding.btnWatchParty?.setOnClickListener {
                showWatchPartyDialog(movie)
            }
        } catch (e: Exception) { /* optional button */ }

        // Rate
        binding.btnRateAction.setOnClickListener {
            if (!isAdded || parentFragmentManager.isStateSaved) return@setOnClickListener
            try { RatingDialog.newInstance(movie.id, movie.title).show(parentFragmentManager, RatingDialog.TAG) }
            catch (e: Exception) { }
        }
    }

    private fun updateWatchlistIcon(inList: Boolean) {
        val icon = binding.btnWatchlistAction.getChildAt(0) as? android.widget.ImageView
        icon?.setColorFilter(ContextCompat.getColor(requireContext(), if (inList) R.color.brand_primary else R.color.t2))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Downloads
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupDownloadButtons(movie: Movie) {
        binding.layoutDownloads.removeAllViews()
        val qualities = movie.downloads.ifEmpty {
            if (movie.downloadUrl.isNotBlank()) listOf(DownloadQuality("Download", movie.downloadUrl, "")) else emptyList()
        }
        if (qualities.isEmpty()) { binding.layoutDownloads.hide(); return }
        binding.layoutDownloads.show()

        qualities.forEach { quality ->
            val btn = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(8, 0, 8, 0)
            btn.layoutParams = params
            btn.text = if (quality.size.isNotBlank()) "⬇ ${quality.quality}\n(${quality.size})" else "⬇ ${quality.quality}"
            btn.textSize = 12f; btn.setPadding(0, 20, 0, 20)
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            btn.setStrokeColorResource(R.color.red); btn.strokeWidth = 3; btn.cornerRadius = 20

            if (dlRepo.isDownloaded(movie.id)) {
                btn.alpha = 0.5f; btn.isEnabled = false; btn.text = "Downloaded ✓"
            } else {
                btn.setOnClickListener { handleDownloadClick(movie, quality.url) }
            }
            binding.layoutDownloads.addView(btn)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actors
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadActors(actorIds: List<String>) {
        lifecycleScope.launch {
            try {
                val actors = actorRepo.getActorsByIds(actorIds)
                if (_binding == null) return@launch
                if (actors.isNotEmpty()) {
                    binding.layoutActors.show()
                    val adapter = MovieActorAdapter { actor -> navigateToActor(actor) }
                    binding.rvActors.adapter = adapter
                    adapter.submitList(actors)
                } else {
                    binding.layoutActors.hide()
                }
            } catch (e: Exception) {
                binding.layoutActors.hide()
            }
        }
    }

    private fun navigateToActor(actor: Actor) {
        try {
            val bundle = Bundle().apply { putString("actor_id", actor.id) }
            findNavController().navigate(R.id.action_detail_to_actor, bundle)
        } catch (e: Exception) {
            requireContext().toast("অভিনেতার প্রোফাইল খুলতে সমস্যা")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun openEpisode(movie: Movie, ep: Episode) {
        if (ep.streamUrl.isBlank()) { requireContext().toast("ভিডিও পাওয়া যায়নি"); return }
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_MOVIE_ID,    movie.id)
            putExtra(Constants.EXTRA_MOVIE_TITLE, "${movie.title} - ${ep.title}")
            putExtra(Constants.EXTRA_VIDEO_URL,   ep.streamUrl)
            putExtra(Constants.EXTRA_BANNER_URL,  ep.thumbnailUrl.ifBlank { movie.bannerImageUrl })
            putExtra(Constants.EXTRA_IS_LOCAL,    false)
        }
        startActivity(intent)
    }

    /**
     * Episode offline download
     * Episode এর unique ID = {seriesId}_ep{episodeNumber}
     * DownloadService existing infrastructure use করে
     */
    private fun downloadEpisode(movie: Movie, ep: Episode) {
        val dlUrl = ep.downloadUrl.ifBlank { ep.streamUrl }
        if (dlUrl.isBlank()) {
            requireContext().toast("এই Episode এর download URL নেই")
            return
        }
        val epId    = "${movie.id}_ep${ep.episodeNumber}"
        val epTitle = "${movie.title} - E${ep.episodeNumber}: ${ep.title}"

        if (dlRepo.isDownloaded(epId)) {
            requireContext().toast("ইতিমধ্যে ডাউনলোড হয়েছে ✓")
            return
        }

        val intent = Intent(requireContext(), com.ottapp.moviestream.service.DownloadService::class.java).apply {
            putExtra(Constants.EXTRA_MOVIE_ID,    epId)
            putExtra(Constants.EXTRA_MOVIE_TITLE, epTitle)
            putExtra(Constants.EXTRA_VIDEO_URL,   dlUrl)
            putExtra(Constants.EXTRA_BANNER_URL,  ep.thumbnailUrl.ifBlank { movie.bannerImageUrl })
        }
        requireContext().startService(intent)
        requireContext().toast("ডাউনলোড শুরু হয়েছে: $epTitle")
        // Adapter refresh করি download state দেখানোর জন্য
        episodeAdapter?.notifyDataSetChanged()
    }

    private fun openPlayer(movie: Movie, url: String, isTrailer: Boolean = false) {
        if (url.isBlank()) { requireContext().toast("ভিডিও পাওয়া যায়নি"); return }
        val isLocal = !isTrailer && dlRepo.isDownloaded(movie.id)
        val title   = if (isTrailer) "${movie.title} — Trailer" else movie.title
        val intent  = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_MOVIE_ID,    movie.id)
            putExtra(Constants.EXTRA_MOVIE_TITLE, title)
            putExtra(Constants.EXTRA_VIDEO_URL,   url)
            putExtra(Constants.EXTRA_BANNER_URL,  movie.bannerImageUrl)
            putExtra(Constants.EXTRA_IS_LOCAL,    isLocal)
        }
        startActivity(intent)
    }

    /** Quality dialog দেখিয়ে তারপর download শুরু করে */
    private fun handleDownloadClick(movie: Movie, downloadUrl: String) {
        showDownloadQualityDialog(movie, downloadUrl)
    }

    private fun showDownloadQualityDialog(movie: Movie, downloadUrl: String) {
        val qualities = arrayOf("Auto (সেরা মান)", "1080p (Full HD)", "720p (HD)", "480p (SD)", "360p (কম data)")
        var selectedIndex = 0
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Download মান বেছে নিন")
            .setSingleChoiceItems(qualities, 0) { _, which -> selectedIndex = which }
            .setPositiveButton("Download শুরু করুন") { _, _ ->
                val qualityTag = when (selectedIndex) {
                    1 -> "1080p"; 2 -> "720p"; 3 -> "480p"; 4 -> "360p"
                    else -> "Auto"
                }
                startDownload(movie, downloadUrl, qualityTag)
                try { findNavController().navigate(R.id.action_global_to_download) }
                catch (e: Exception) { requireContext().toast("ডাউনলোড শুরু হয়েছে") }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun startDownload(movie: Movie, downloadUrl: String, quality: String = "Auto") {
        val intent = Intent(requireContext(), DownloadService::class.java).apply {
            putExtra(Constants.EXTRA_MOVIE_ID,    movie.id)
            putExtra(Constants.EXTRA_MOVIE_TITLE, movie.title)
            putExtra(Constants.EXTRA_VIDEO_URL,   downloadUrl)
            putExtra(Constants.EXTRA_BANNER_URL,  movie.bannerImageUrl)
            putExtra(com.ottapp.moviestream.service.DownloadService.EXTRA_DOWNLOAD_QUALITY, quality)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        requireContext().toast("ডাউনলোড শুরু হয়েছে ($quality)...")
    }

    private fun openSubscriptionDialog() {
        try {
            if (!isAdded || parentFragmentManager.isStateSaved) return
            SubscriptionDialog.newInstance().show(parentFragmentManager, SubscriptionDialog.TAG)
        } catch (e: Exception) { }
    }

    override fun onDestroyView() {
        episodeAdapter = null
        relatedAdapter = null
        _binding = null
        super.onDestroyView()
    }

    // ── Share with OpenGraph thumbnail ────────────────────────────────────────

    private fun shareMovieWithThumbnail(movie: com.ottapp.moviestream.data.model.Movie) {
        val deepLink   = "https://cinestream.app/movie/${movie.id}"
        val shareText  = """🎬 ${movie.title}
⭐ ${movie.imdbRating}/10 | ${movie.year} | ${movie.duration}

CineStream এ দেখুন:
$deepLink

📱 Download: https://github.com/YOUR_GITHUB/CineStreamOTT/releases"""

        // WhatsApp/Facebook এ share করলে OpenGraph meta tag দিয়ে thumbnail দেখাবে
        // (cinestream.app/movie/{id} এ og:image, og:title, og:description থাকতে হবে)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "দেখুন: ${movie.title} — CineStream")
        }
        startActivity(android.content.Intent.createChooser(intent, "শেয়ার করুন"))
    }

    // ── Watch Party ───────────────────────────────────────────────────────────

    private fun showWatchPartyDialog(movie: com.ottapp.moviestream.data.model.Movie) {
        val videoUrl = movie.videoStreamUrl
        if (videoUrl.isBlank()) {
            requireContext().toast("এই মুভির জন্য Watch Party সম্ভব নয়")
            return
        }
        if (!isAdded || parentFragmentManager.isStateSaved) return
        val dialog = WatchPartyDialog.newInstance(movie.id, movie.title, videoUrl)
        dialog.onRoomCreated = { roomId, url, startPos ->
            launchPlayerForWatchParty(movie, url, roomId, isHost = true, startPos = startPos)
        }
        dialog.onRoomJoined = { roomId, url, startPos ->
            launchPlayerForWatchParty(movie, url, roomId, isHost = false, startPos = startPos)
        }
        dialog.show(parentFragmentManager, WatchPartyDialog.TAG)
    }

    private fun launchPlayerForWatchParty(
        movie: com.ottapp.moviestream.data.model.Movie,
        videoUrl: String, roomId: String,
        isHost: Boolean, startPos: Long
    ) {
        val intent = android.content.Intent(requireContext(), com.ottapp.moviestream.ui.player.PlayerActivity::class.java).apply {
            putExtra(com.ottapp.moviestream.util.Constants.EXTRA_MOVIE_ID,    movie.id)
            putExtra(com.ottapp.moviestream.util.Constants.EXTRA_MOVIE_TITLE, movie.title)
            putExtra(com.ottapp.moviestream.util.Constants.EXTRA_VIDEO_URL,   videoUrl)
            putExtra("watch_party_room_id", roomId)
            putExtra("watch_party_is_host", isHost)
            putExtra("watch_party_start_pos", startPos)
        }
        startActivity(intent)
    }


}