package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.databinding.ItemMovieCardBinding
import com.ottapp.moviestream.util.loadImage

class MovieGridAdapter(
    private val onClick: (Movie) -> Unit,
    private val onLockedClick: ((Movie) -> Unit)? = null,
    private val onLoadMore: (() -> Unit)? = null
) : ListAdapter<Movie, MovieGridAdapter.VH>(Diff()) {

    private var lastAnimatedPosition = -1
    private var isPremiumUser = false

    /** Call this when the user's premium status changes – updates lock icons instantly */
    fun setPremiumUser(isPremium: Boolean) {
        if (isPremiumUser != isPremium) {
            isPremiumUser = isPremium
            notifyItemRangeChanged(0, itemCount)
        }
    }

    inner class VH(private val b: ItemMovieCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(movie: Movie) {
            b.ivThumb.loadImage(movie.bannerImageUrl, R.color.surface2)
            b.tvTitle.text = movie.title.ifEmpty { "Unknown" }
            b.tvRating.text = "\u2605 ${movie.imdbRating}"
            b.tvCategory.text = movie.category

            // Lock icon: show only for non-premium users on premium (non-free) content
            val isLocked = !isPremiumUser && !movie.testMovie
            b.ivLock.visibility = if (isLocked) View.VISIBLE else View.GONE

            b.ivThumb.transitionName = "movie_poster_${movie.id}"
            b.root.contentDescription = "${movie.title}, rated ${movie.imdbRating}"
            b.ivThumb.contentDescription = "${movie.title} poster"

            b.root.setOnClickListener {
                if (isLocked && onLockedClick != null) {
                    // Free user clicking premium content → show subscription
                    onLockedClick.invoke(movie)
                } else {
                    onClick(movie)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
        if (position > lastAnimatedPosition) {
            animateCard(holder.itemView, position)
            lastAnimatedPosition = position
        }
        // Smart lazy loading: trigger 5 items before the end
        if (position >= itemCount - 5) {
            onLoadMore?.invoke()
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.itemView.alpha = 1f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
    }

    private fun animateCard(view: View, position: Int) {
        val stagger = (position % 6) * 40L
        view.alpha = 0f
        view.scaleX = 0.88f
        view.scaleY = 0.88f
        view.translationY = 30f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setStartDelay(stagger)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    class Diff : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(a: Movie, b: Movie) = a.id == b.id
        override fun areContentsTheSame(a: Movie, b: Movie) = a == b
    }
}
