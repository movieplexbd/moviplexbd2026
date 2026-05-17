package com.ottapp.moviestream.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.ottapp.moviestream.R
import com.ottapp.moviestream.adapter.BannerAdapter
import com.ottapp.moviestream.adapter.MovieGridAdapter
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.databinding.FragmentHomeBinding
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.RecentlyViewedManager
import com.ottapp.moviestream.adapter.RecentlyViewedAdapter
import com.ottapp.moviestream.util.ErrorHandler
import com.ottapp.moviestream.util.NetworkMonitor
import com.ottapp.moviestream.util.WatchHistoryEntry
import com.ottapp.moviestream.adapter.ContinueWatchingAdapter
import com.ottapp.moviestream.ui.subscription.SubscriptionDialog
import com.google.android.material.chip.Chip
import com.ottapp.moviestream.util.hide
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.util.show

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val vm: HomeViewModel by viewModels()

    private var bannerAdapter: BannerAdapter? = null
    private var trendingAdapter: MovieGridAdapter? = null
    private var aiAdapter: MovieGridAdapter? = null
    private var banglaAdapter: MovieGridAdapter? = null
    private var hindiAdapter: MovieGridAdapter? = null
    private var allAdapter: MovieGridAdapter? = null
    private var continueWatchingAdapter: ContinueWatchingAdapter? = null
    private var recentlyViewedHomeAdapter: RecentlyViewedAdapter? = null
    private var genreMovieAdapter: MovieGridAdapter? = null
    private var recentlyViewedManager: RecentlyViewedManager? = null

    private val scrollHandler = Handler(Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null
    private var bannerTotal = 0

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val binding = FragmentHomeBinding.inflate(inflater, container, false)
            _binding = binding
            binding.root
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "inflate error: ${e.message}", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return
        try { initAdapters() }  catch (e: Exception) { log("initAdapters: ${e.message}") }
        try { initBanner() }    catch (e: Exception) { log("initBanner: ${e.message}") }
        try { observeData() }   catch (e: Exception) { log("observeData: ${e.message}") }
        try { initRefresh() }   catch (e: Exception) { log("initRefresh: ${e.message}") }
        try { initSearch() }    catch (e: Exception) { log("initSearch: ${e.message}") }
        try { initReels() }     catch (e: Exception) { log("initReels: ${e.message}") }
        try { initRecentlyViewed() } catch (e: Exception) { log("initRecentlyViewed: ${e.message}") }
        try { initGenreFilter() }    catch (e: Exception) { log("initGenreFilter: ${e.message}") }
    }

    override fun onResume() {
        super.onResume()
        if (bannerTotal > 1) startScroll(bannerTotal)
        // Refresh recently viewed when user returns to home
        try {
            val recentList = recentlyViewedManager?.getAll() ?: emptyList()
            if (recentList.isNotEmpty()) {
                _binding?.sectionRecentlyViewed?.show()
                recentlyViewedHomeAdapter?.submitList(recentList)
            }
        } catch (e: Exception) { }
    }

    override fun onPause() {
        super.onPause()
        stopScroll()
        runCatching { _binding?.shimmerLayout?.stopShimmer() }
    }

    override fun onDestroyView() {
        stopScroll()
        runCatching { _binding?.shimmerLayout?.stopShimmer() }
        runCatching { _binding?.bannerPager?.adapter = null }
        bannerAdapter = null
        trendingAdapter = null
        aiAdapter = null
        banglaAdapter = null
        hindiAdapter = null
        allAdapter = null
        continueWatchingAdapter = null
        recentlyViewedHomeAdapter = null
        genreMovieAdapter = null
        _binding = null
        super.onDestroyView()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun showSubscriptionDialog() {
        try {
            if (!isAdded || parentFragmentManager.isStateSaved) return
            SubscriptionDialog.newInstance().show(parentFragmentManager, SubscriptionDialog.TAG)
        } catch (e: Exception) {
            log("subscription dialog error: ${e.message}")
        }
    }

    private fun initAdapters() {
        val onClick: (Movie) -> Unit = { goToDetail(it) }
        val onLockedClick: (Movie) -> Unit = { showSubscriptionDialog() }

        trendingAdapter = MovieGridAdapter(onClick, onLockedClick)
        aiAdapter = MovieGridAdapter(onClick, onLockedClick)
        banglaAdapter   = MovieGridAdapter(onClick, onLockedClick)
        hindiAdapter    = MovieGridAdapter(onClick, onLockedClick)
        allAdapter      = MovieGridAdapter(onClick, onLockedClick)

        _binding?.rvAiRecommendations?.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
            adapter = aiAdapter
            setHasFixedSize(true)
        }

        _binding?.rvTrending?.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = trendingAdapter
            isNestedScrollingEnabled = false
        }
        _binding?.rvBangla?.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = banglaAdapter
            isNestedScrollingEnabled = false
        }
        _binding?.rvHindi?.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = hindiAdapter
            isNestedScrollingEnabled = false
        }
        _binding?.rvAll?.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = allAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun initBanner() {
        val adp = BannerAdapter { banner ->
            if (banner.movieId.isNotEmpty()) {
                goToDetailById(banner.movieId)
            }
        }
        bannerAdapter = adp
        _binding?.bannerPager?.let { pager ->
            pager.adapter = adp
            pager.offscreenPageLimit = 1
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateDots(position)
                }
            })
        }
    }

    private fun goToDetailById(movieId: String) {
        try {
            findNavController().navigate(R.id.action_home_to_detail,
                bundleOf(Constants.EXTRA_MOVIE_ID to movieId))
        } catch (e: Exception) {
            log("nav to detail error: ${e.message}")
        }
    }

    private fun initRefresh() {
        _binding?.swipeRefresh?.setOnRefreshListener {
            vm.loadData()
        }
    }

    private fun initSearch() {
        _binding?.btnSearch?.setOnClickListener {
            try {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .setLaunchSingleTop(true)
                    .build()
                findNavController().navigate(R.id.searchFragment, null, navOptions)
            } catch (e: Exception) {
                log("search nav: ${e.message}")
            }
        }
    }

    private fun initReels() {
        _binding?.btnReels?.setOnClickListener {
            try {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .setLaunchSingleTop(true)
                    .build()
                findNavController().navigate(R.id.reelsFragment, null, navOptions)
            } catch (e: Exception) {
                log("reels nav: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observers
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeData() {
        // Network error handling
        vm.networkError.observe(viewLifecycleOwner) { hasError ->
            if (hasError) {
                val rootView = _binding?.root ?: return@observe
                com.google.android.material.snackbar.Snackbar.make(
                    rootView,
                    "ইন্টারনেট সংযোগ নেই। পরে আবার চেষ্টা করুন।",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("আবার চেষ্টা করুন") {
                    vm.loadData()
                }.show()
            }
        }

        vm.loading.observe(viewLifecycleOwner) { isLoading ->
            val b = _binding ?: return@observe
            runCatching {
                if (isLoading) {
                    b.shimmerLayout.startShimmer()
                    b.shimmerLayout.show()
                    b.contentWrapper.hide()
                } else {
                    b.shimmerLayout.stopShimmer()
                    b.shimmerLayout.hide()
                    b.contentWrapper.show()
                    b.swipeRefresh.isRefreshing = false
                }
            }
        }

        vm.banners.observe(viewLifecycleOwner) { list ->
            if (_binding == null) return@observe
            runCatching {
                bannerAdapter?.submitList(list)
                buildDots(list.size)
                if (list.size > 1) startScroll(list.size) else stopScroll()
            }
        }

        vm.continueWatching.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                val section = b.root.findViewById<android.view.View>(R.id.section_continue_watching)
                val rv = b.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_continue_watching)
                if (list.isEmpty()) {
                    section?.visibility = android.view.View.GONE
                } else {
                    section?.visibility = android.view.View.VISIBLE
                    if (continueWatchingAdapter == null) {
                        continueWatchingAdapter = ContinueWatchingAdapter { entry ->
                            try {
                                findNavController().navigate(R.id.action_home_to_detail,
                                    androidx.core.os.bundleOf(Constants.EXTRA_MOVIE_ID to entry.movieId))
                            } catch (e: Exception) {}
                        }
                        rv?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                        rv?.adapter = continueWatchingAdapter
                    }
                    continueWatchingAdapter?.submitList(list)
                }
            }
        }

        vm.trendingMovies.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                trendingAdapter?.submitList(list)
                b.sectionTrending.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        vm.aiRecommendations.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                aiAdapter?.submitList(list)
                b.sectionAiRecommendations.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        vm.banglaMovies.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                banglaAdapter?.submitList(list)
                b.sectionBangla.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        vm.hindiMovies.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                hindiAdapter?.submitList(list)
                b.sectionHindi.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        vm.availableGenres.observe(viewLifecycleOwner) { genres ->
            val b = _binding ?: return@observe
            runCatching { buildGenreChips(genres) }
        }

        vm.genreFilteredMovies.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                val rv = b.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_genre_movies)
                if (list.isEmpty()) {
                    rv?.visibility = android.view.View.GONE
                } else {
                    rv?.visibility = android.view.View.VISIBLE
                    genreMovieAdapter?.submitList(list)
                }
            }
        }

        vm.allMovies.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe
            runCatching {
                allAdapter?.submitList(list)
                b.sectionAll.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        vm.currentUser.observe(viewLifecycleOwner) { user ->
            val b = _binding ?: return@observe
            runCatching {
                if (user == null) return@runCatching
                val initial = user.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                b.tvAvatarInitial.text = initial
                b.tvAvatarInitial.show()
                if (user.photoUrl.isNotEmpty()) {
                    b.ivAvatar.loadImage(user.photoUrl)
                    b.ivAvatar.show()
                }
                b.tvSubscriptionBadge.text = if (user.isPremium) "PREMIUM" else "FREE"
                b.tvSubscriptionBadge.show()

                // Instantly remove/add lock icons across all movie grids without reload
                val hasAccess = user.hasAccess
                trendingAdapter?.setPremiumUser(hasAccess)
                aiAdapter?.setPremiumUser(hasAccess)
                banglaAdapter?.setPremiumUser(hasAccess)
                hindiAdapter?.setPremiumUser(hasAccess)
                allAdapter?.setPremiumUser(hasAccess)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Banner dots
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildDots(count: Int) {
        bannerTotal = count
        val container = _binding?.bannerDots ?: return
        container.removeAllViews()
        if (count <= 1) return
        val dp = resources.displayMetrics.density
        repeat(count) { i ->
            runCatching {
                val sz = ((if (i == 0) 10 else 7) * dp).toInt()
                val iv = ImageView(requireContext())
                val lp = LinearLayout.LayoutParams(sz, sz)
                lp.setMargins(4, 0, 4, 0)
                iv.layoutParams = lp
                iv.setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                container.addView(iv)
            }
        }
    }

    private fun updateDots(selected: Int) {
        val container = _binding?.bannerDots ?: return
        val dp = resources.displayMetrics.density
        for (i in 0 until container.childCount) {
            runCatching {
                val iv = container.getChildAt(i) as? ImageView ?: return@runCatching
                val active = (i == selected)
                val sz = ((if (active) 10 else 7) * dp).toInt()
                val lp = LinearLayout.LayoutParams(sz, sz)
                lp.setMargins(4, 0, 4, 0)
                iv.layoutParams = lp
                iv.setImageResource(if (active) R.drawable.dot_active else R.drawable.dot_inactive)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auto scroll
    // ─────────────────────────────────────────────────────────────────────────

    private fun startScroll(count: Int) {
        stopScroll()
        if (count <= 1) return
        scrollRunnable = object : Runnable {
            override fun run() {
                val pager = _binding?.bannerPager ?: return
                runCatching {
                    pager.setCurrentItem((pager.currentItem + 1) % count, true)
                }
                scrollHandler.postDelayed(this, 4000)
            }
        }
        scrollHandler.postDelayed(scrollRunnable!!, 4000)
    }

    private fun stopScroll() {
        scrollRunnable?.let { scrollHandler.removeCallbacks(it) }
        scrollRunnable = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    private fun goToDetail(movie: Movie) {
        if (!isAdded || _binding == null) return
        runCatching {
            findNavController().navigate(
                R.id.action_home_to_detail,
                bundleOf(Constants.EXTRA_MOVIE_ID to movie.id)
            )
        }
    }

    private fun initRecentlyViewed() {
        val b = _binding ?: return
        recentlyViewedManager = RecentlyViewedManager(requireContext())
        recentlyViewedHomeAdapter = RecentlyViewedAdapter { entry ->
            try {
                val bundle = android.os.Bundle().apply {
                    putString(com.ottapp.moviestream.util.Constants.EXTRA_MOVIE_ID, entry.movieId)
                }
                findNavController().navigate(R.id.action_home_to_detail, bundle)
            } catch (e: Exception) { }
        }
        b.rvRecentlyViewedHome.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        b.rvRecentlyViewedHome.adapter = recentlyViewedHomeAdapter
        val recentList = recentlyViewedManager!!.getAll()
        if (recentList.isNotEmpty()) {
            b.sectionRecentlyViewed.show()
            recentlyViewedHomeAdapter!!.submitList(recentList)
        } else {
            b.sectionRecentlyViewed.hide()
        }
    }

    private fun initGenreFilter() {
        val b = _binding ?: return
        val rv = b.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_genre_movies)
            ?: return
        genreMovieAdapter = MovieGridAdapter(
            onClick = { goToDetail(it) },
            onLockedClick = { showSubscriptionDialog() }
        )
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        rv.adapter = genreMovieAdapter
    }

    private fun buildGenreChips(genres: List<String>) {
        val b = _binding ?: return
        val chipGroup = b.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cg_genre_filter)
            ?: return
        if (genres.isEmpty()) {
            b.root.findViewById<android.view.View>(R.id.section_genre_filter)?.visibility = android.view.View.GONE
            return
        }
        b.root.findViewById<android.view.View>(R.id.section_genre_filter)?.visibility = android.view.View.VISIBLE
        chipGroup.removeAllViews()
        genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre
                isCheckable = true
                isClickable = true
                textSize = 12f
                setChipBackgroundColorResource(R.color.surface2)
                setCheckedIconVisible(false)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) vm.filterByGenre(genre)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun log(msg: String) = android.util.Log.e("HomeFragment", msg)
}
