package com.ottapp.moviestream.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.databinding.ItemAdminMovieBinding

class AdminMovieAdapter(
    private val onEdit:         (Movie) -> Unit,
    private val onDelete:       (Movie) -> Unit,
    private val onManageSeries: ((Movie) -> Unit)? = null  // ← নতুন: Series Editor
) : ListAdapter<Movie, AdminMovieAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemAdminMovieBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(movie: Movie) {
            b.tvTitle.text    = movie.title
            b.tvCategory.text = movie.category
            b.tvRating.text   = "★ ${movie.imdbRating}"
            b.tvTrending.visibility = if (movie.trending) View.VISIBLE else View.GONE

            if (movie.testMovie) {
                b.tvFree.text = "FREE"
                b.tvFree.setBackgroundColor(ContextCompat.getColor(b.root.context, R.color.green))
            } else {
                b.tvFree.text = "PREMIUM"
                b.tvFree.setBackgroundColor(ContextCompat.getColor(b.root.context, R.color.red))
            }

            b.btnEdit.setOnClickListener   { onEdit(movie) }
            b.btnDelete.setOnClickListener { onDelete(movie) }

            // Series হলে "Manage Episodes" button দেখাই
            try {
                if (movie.isSeries && onManageSeries != null) {
                    b.btnManageSeries?.visibility = View.VISIBLE
                    b.btnManageSeries?.setOnClickListener { onManageSeries.invoke(movie) }
                } else {
                    b.btnManageSeries?.visibility = View.GONE
                }
            } catch (e: Exception) { /* optional button in layout */ }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAdminMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(a: Movie, b: Movie) = a.id == b.id
        override fun areContentsTheSame(a: Movie, b: Movie) = a == b
    }
}
