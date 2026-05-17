package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.R
import com.ottapp.moviestream.util.WatchHistoryEntry

class ContinueWatchingAdapter(
    private val onClick: (WatchHistoryEntry) -> Unit
) : ListAdapter<WatchHistoryEntry, ContinueWatchingAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<WatchHistoryEntry>() {
            override fun areItemsTheSame(a: WatchHistoryEntry, b: WatchHistoryEntry) = a.movieId == b.movieId
            override fun areContentsTheSame(a: WatchHistoryEntry, b: WatchHistoryEntry) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.iv_cw_thumb)
        val tvTitle: TextView = view.findViewById(R.id.tv_cw_title)
        val progressBar: ProgressBar = view.findViewById(R.id.pb_cw_progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_continue_watching, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        holder.tvTitle.text = entry.title
        holder.progressBar.progress = entry.progressPercent
        holder.iv.loadImage(entry.bannerUrl)
        holder.itemView.setOnClickListener { onClick(entry) }
    }
}
