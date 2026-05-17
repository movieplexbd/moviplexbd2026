package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.databinding.ItemRelatedMovieBinding
import com.ottapp.moviestream.util.loadImage

class RelatedMoviesAdapter(
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, RelatedMoviesAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(a: Movie, b: Movie) = a.id == b.id
            override fun areContentsTheSame(a: Movie, b: Movie) = a == b
        }
    }

    inner class VH(val b: ItemRelatedMovieBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemRelatedMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val movie = getItem(position)
        holder.b.ivThumb.loadImage(movie.bannerImageUrl)
        holder.b.tvTitle.text    = movie.title
        holder.b.tvCategory.text = movie.category
        if (movie.imdbRating > 0) {
            holder.b.tvRating.text = "⭐ ${movie.imdbRating}"
            holder.b.tvRating.visibility = android.view.View.VISIBLE
        } else {
            holder.b.tvRating.visibility = android.view.View.GONE
        }
        holder.b.root.setOnClickListener { onClick(movie) }
    }
}
