package com.ottapp.moviestream.ui.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ottapp.moviestream.R
import com.ottapp.moviestream.adapter.MovieGridAdapter
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.databinding.FragmentMoviesBinding
import com.ottapp.moviestream.ui.subscription.SubscriptionDialog
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.hide
import com.ottapp.moviestream.util.show

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var adapter: MovieGridAdapter

    private val categories = listOf(
        "সব"      to Constants.CAT_ALL,
        "বাংলা"   to Constants.CAT_BANGLA,
        "হিন্দি"  to Constants.CAT_HINDI,
        "ট্রেন্ডিং" to Constants.CAT_TRENDING
    )
    private val chipViews = mutableListOf<TextView>()
    private var selectedCat = Constants.CAT_ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MovieGridAdapter(
            onClick       = { movie -> openDetail(movie) },
            onLockedClick = { showSubscriptionDialog() },
            onLoadMore    = { viewModel.loadNextPage() }
        )
        binding.rvMovies.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvMovies.adapter = adapter

        buildCategoryChips()
        observeViewModel()
    }

    private fun buildCategoryChips() {
        val group = binding.chipGroup
        group.removeAllViews()
        chipViews.clear()

        val dp8  = (8  * resources.displayMetrics.density).toInt()
        val dp20 = (20 * resources.displayMetrics.density).toInt()
        val dp36 = (36 * resources.displayMetrics.density).toInt()

        categories.forEachIndexed { i, (label, cat) ->
            val chip = TextView(requireContext()).apply {
                text = label
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.NORMAL)
                setPadding(dp20, 0, dp20, 0)
                height = dp36
                gravity = android.view.Gravity.CENTER
                isClickable = true
                isFocusable = true
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_selector)
                setOnClickListener { selectChip(i, cat) }
            }
            val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp36)
            if (i < categories.size - 1) lp.marginEnd = dp8
            group.addView(chip, lp)
            chipViews.add(chip)
        }
        selectChip(0, Constants.CAT_ALL)
    }

    private fun selectChip(index: Int, cat: String) {
        selectedCat = cat
        chipViews.forEachIndexed { i, chip ->
            chip.isSelected = (i == index)
            chip.setTextColor(
                ContextCompat.getColor(requireContext(),
                    if (i == index) R.color.white else R.color.t2)
            )
        }
        viewModel.setCategory(cat)
    }

    private fun showSubscriptionDialog() {
        try {
            if (!isAdded || parentFragmentManager.isStateSaved) return
            SubscriptionDialog.newInstance().show(parentFragmentManager, SubscriptionDialog.TAG)
        } catch (e: Exception) { }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.show()
                binding.rvMovies.hide()
                binding.layoutEmpty.hide()
            } else {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.hide()
            }
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            adapter.setPremiumUser(user?.hasAccess == true)
        }

        viewModel.filteredMovies.observe(viewLifecycleOwner) { movies ->
            adapter.submitList(movies)
            binding.tvResultCount.text =
                if (movies.isEmpty()) "" else "${movies.size}টি মুভি"

            val isLoading = viewModel.loading.value == true
            if (!isLoading) {
                if (movies.isEmpty()) {
                    binding.layoutEmpty.show()
                    binding.rvMovies.hide()
                } else {
                    binding.layoutEmpty.hide()
                    binding.rvMovies.show()
                }
            }
        }

        // ── Error handling ──────────────────────────────────────────────────
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                val root = _binding?.root ?: return@observe
                Snackbar.make(root, errorMsg, Snackbar.LENGTH_LONG)
                    .setAction("আবার চেষ্টা করুন") { viewModel.loadMovies() }
                    .show()
            }
        }
    }

    private fun openDetail(movie: Movie) {
        if (!isAdded || _binding == null) return
        try {
            findNavController().navigate(
                R.id.action_movies_to_detail,
                bundleOf(Constants.EXTRA_MOVIE_ID to movie.id)
            )
        } catch (_: Exception) { }
    }

    override fun onDestroyView() {
        _binding?.shimmerLayout?.stopShimmer()
        _binding = null
        super.onDestroyView()
    }
}
