package com.ottapp.moviestream.ui.download

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ottapp.moviestream.adapter.ActiveDownloadAdapter
import com.ottapp.moviestream.adapter.DownloadAdapter
import com.ottapp.moviestream.data.model.DownloadedMovie
import com.ottapp.moviestream.databinding.FragmentDownloadBinding
import com.ottapp.moviestream.service.DownloadService
import com.ottapp.moviestream.ui.player.PlayerActivity
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.hide
import com.ottapp.moviestream.util.show
import com.ottapp.moviestream.util.toReadableSize

class DownloadFragment : Fragment() {

    private var _binding: FragmentDownloadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadViewModel by viewModels()

    private lateinit var activeAdapter: ActiveDownloadAdapter
    private lateinit var downloadAdapter: DownloadAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        observeViewModel()
        updateFreeStorage()
    }

    private fun setupAdapters() {
        activeAdapter = ActiveDownloadAdapter { movieId -> cancelDownload(movieId) }
        binding.rvActive.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeAdapter
            isNestedScrollingEnabled = false
        }

        downloadAdapter = DownloadAdapter(
            onPlay   = { movie -> playOffline(movie) },
            onDelete = { movie -> confirmDelete(movie) }
        )
        binding.rvDownloads.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.activeDownloads.observe(viewLifecycleOwner) { active ->
            if (active.isNotEmpty()) {
                activeAdapter.submitList(active.values.toList())
                binding.sectionActive.show()
            } else {
                binding.sectionActive.hide()
            }
            refreshEmpty()
        }

        viewModel.downloads.observe(viewLifecycleOwner) { downloads ->
            downloadAdapter.submitList(downloads)
            if (downloads.isNotEmpty()) {
                binding.tvCompletedLabel.text = "সংরক্ষিত মুভি (${downloads.size}টি)"
                binding.sectionCompletedHeader.show()
                binding.cardStorage.show()
                binding.tvDownloadCount.text = downloads.size.toString()
            } else {
                binding.sectionCompletedHeader.hide()
                binding.cardStorage.visibility = View.GONE
            }
            refreshEmpty()
        }

        viewModel.storageUsed.observe(viewLifecycleOwner) { size ->
            binding.tvStorageUsed.text = size
            binding.cardStorage.show()
        }
    }

    private fun updateFreeStorage() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBlocksLong * stat.blockSizeLong
            binding.tvStorageFree.text = free.toReadableSize()
        } catch (e: Exception) {
            binding.tvStorageFree.text = "N/A"
        }
    }

    private fun refreshEmpty() {
        val hasActive    = viewModel.activeDownloads.value?.isNotEmpty() == true
        val hasCompleted = viewModel.downloads.value?.isNotEmpty() == true
        if (hasActive || hasCompleted) binding.layoutEmpty.hide() else binding.layoutEmpty.show()
    }

    private fun cancelDownload(movieId: String) {
        val intent = Intent(requireContext(), DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
            putExtra(DownloadService.EXTRA_MOVIE_ID_CANCEL, movieId)
        }
        requireContext().startService(intent)
    }

    private fun playOffline(movie: DownloadedMovie) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_MOVIE_ID,    movie.movieId)
            putExtra(Constants.EXTRA_MOVIE_TITLE, movie.title)
            putExtra(Constants.EXTRA_VIDEO_URL,   movie.localFilePath)
            putExtra(Constants.EXTRA_BANNER_URL,  movie.bannerImageUrl)
            putExtra(Constants.EXTRA_IS_LOCAL,    true)
        }
        startActivity(intent)
    }

    private fun confirmDelete(movie: DownloadedMovie) {
        AlertDialog.Builder(requireContext())
            .setTitle("ডিলিট করবেন?")
            .setMessage("\"${movie.title}\" ডাউনলোড মুছে ফেলতে চান?")
            .setPositiveButton("ডিলিট") { _, _ ->
                viewModel.deleteDownload(movie.movieId)
                updateFreeStorage()
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDownloads()
        updateFreeStorage()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
