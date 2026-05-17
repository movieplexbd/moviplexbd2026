package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.R
import com.ottapp.moviestream.util.RecentlyViewedEntry

class RecentlyViewedAdapter(
    private val onClick: (RecentlyViewedEntry) -> Unit
) : ListAdapter<RecentlyViewedEntry, RecentlyViewedAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RecentlyViewedEntry>() {
            override fun areItemsTheSame(a: RecentlyViewedEntry, b: RecentlyViewedEntry) = a.movieId == b.movieId
            override fun areContentsTheSame(a: RecentlyViewedEntry, b: RecentlyViewedEntry) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.iv_rv_thumb)
        val tvTitle: TextView = view.findViewById(R.id.tv_rv_title)
        val tvCategory: TextView = view.findViewById(R.id.tv_rv_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recently_viewed, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        holder.tvTitle.text = entry.title
        holder.tvCategory.text = entry.category
        holder.iv.loadImage(entry.bannerUrl)
        holder.itemView.setOnClickListener { onClick(entry) }
    }
}
