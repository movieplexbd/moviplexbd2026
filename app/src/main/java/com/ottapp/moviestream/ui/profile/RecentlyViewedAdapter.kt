package com.ottapp.moviestream.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ottapp.moviestream.R
import com.ottapp.moviestream.util.RecentlyViewedEntry

class RecentlyViewedAdapter(
    private val onItemClick: (RecentlyViewedEntry) -> Unit
) : RecyclerView.Adapter<RecentlyViewedAdapter.VH>() {

    private val items = mutableListOf<RecentlyViewedEntry>()

    fun submitList(list: List<RecentlyViewedEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.iv_thumb)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_card, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.tvTitle.text = entry.title
        Glide.with(holder.iv.context)
            .load(entry.bannerUrl)
            .placeholder(R.color.surface2)
            .into(holder.iv)
        holder.itemView.setOnClickListener { onItemClick(entry) }
    }
}
