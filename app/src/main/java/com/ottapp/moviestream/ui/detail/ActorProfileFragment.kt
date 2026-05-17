package com.ottapp.moviestream.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ottapp.moviestream.R
import com.ottapp.moviestream.adapter.MovieGridAdapter
import com.ottapp.moviestream.data.repository.ActorRepository
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.databinding.FragmentActorProfileBinding
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.hide
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.util.show
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class ActorProfileFragment : Fragment() {

    private var _binding: FragmentActorProfileBinding? = null
    private val binding get() = _binding!!

    private val actorRepo = ActorRepository()
    private val movieRepo = MovieRepository()
    private lateinit var movieAdapter: MovieGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actorId = arguments?.getString("actor_id")
        if (actorId.isNullOrEmpty()) {
            requireContext().toast("অভিনেতার তথ্য পাওয়া যায়নি")
            findNavController().navigateUp()
            return
        }

        // Back button (layout uses btn_back NOT toolbar)
        try { binding.btnBack.setOnClickListener { findNavController().navigateUp() } } catch (e: Exception) { }

        // Setup RecyclerView for actor movies (layout uses rv_actor_movies)
        movieAdapter = MovieGridAdapter(
            onClick = { movie ->
                try {
                    val bundle = Bundle().apply { putString(Constants.EXTRA_MOVIE_ID, movie.id) }
                    findNavController().navigate(R.id.action_actor_to_detail, bundle)
                } catch (e: Exception) { }
            }
        )
        binding.rvActorMovies.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvActorMovies.adapter = movieAdapter

        loadActorData(actorId)
    }

    private fun loadActorData(actorId: String) {
        lifecycleScope.launch {
            try {
                val actor = actorRepo.getActorById(actorId)
                if (_binding == null) return@launch

                if (actor != null) {
                    binding.tvActorName.text = actor.name
                    binding.ivActor.loadImage(actor.imageUrl)

                    // Bio (optional field — show only if not blank)
                    try {
                        val bio = (actor as? Any)?.let {
                            it.javaClass.getDeclaredField("bio").also { f -> f.isAccessible = true }
                                .get(it) as? String
                        }
                        if (!bio.isNullOrBlank()) {
                            binding.tvActorBio.text = bio
                            binding.tvActorBio.show()
                        } else {
                            binding.tvActorBio.hide()
                        }
                    } catch (e: Exception) {
                        binding.tvActorBio.hide()
                    }

                    val allMovies = movieRepo.getAllMovies()
                    val actorMovies = allMovies.filter { it.actorIds.contains(actorId) }
                    movieAdapter.submitList(actorMovies)

                } else {
                    requireContext().toast("অভিনেতার তথ্য পাওয়া যায়নি")
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                if (_binding == null) return@launch
                requireContext().toast("লোড করতে সমস্যা হয়েছে")
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
