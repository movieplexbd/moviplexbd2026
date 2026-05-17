package com.ottapp.moviestream.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.repository.RatingRepository
import kotlinx.coroutines.launch

class RatingDialog : BottomSheetDialogFragment() {

    private val ratingRepo = RatingRepository()

    companion object {
        const val TAG = "RatingDialog"
        private const val ARG_MOVIE_ID = "movie_id"
        private const val ARG_MOVIE_TITLE = "movie_title"

        fun newInstance(movieId: String, movieTitle: String): RatingDialog {
            return RatingDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MOVIE_ID, movieId)
                    putString(ARG_MOVIE_TITLE, movieTitle)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_rating, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieId = arguments?.getString(ARG_MOVIE_ID) ?: return
        val movieTitle = arguments?.getString(ARG_MOVIE_TITLE) ?: ""

        val tvTitle = view.findViewById<TextView>(R.id.tv_rating_movie_title)
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar)
        val etReview = view.findViewById<EditText>(R.id.et_review)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_rating)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_rating)

        tvTitle.text = movieTitle

        lifecycleScope.launch {
            try {
                val existing = ratingRepo.getUserRating(movieId)
                if (existing != null) {
                    ratingBar.rating = existing.rating
                    etReview.setText(existing.review)
                }
            } catch (_: Exception) {}
        }

        btnCancel.setOnClickListener { dismiss() }

        btnSubmit.setOnClickListener {
            val stars = ratingBar.rating
            if (stars == 0f) {
                Toast.makeText(requireContext(), "রেটিং দিন (১-৫ স্টার)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val review = etReview.text.toString().trim()
            btnSubmit.isEnabled = false
            lifecycleScope.launch {
                val success = ratingRepo.submitRating(movieId, stars, review)
                if (!isAdded) return@launch
                if (success) {
                    Toast.makeText(requireContext(), "রেটিং সফলভাবে জমা হয়েছে!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "রেটিং জমা করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    btnSubmit.isEnabled = true
                }
            }
        }
    }
}
