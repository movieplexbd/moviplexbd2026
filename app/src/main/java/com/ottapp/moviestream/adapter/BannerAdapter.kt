package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Banner
import com.ottapp.moviestream.databinding.ItemBannerBinding
import com.ottapp.moviestream.util.loadImage

class BannerAdapter(
    private val onClick: (Banner) -> Unit
) : ListAdapter<Banner, BannerAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemBannerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(banner: Banner) {
            b.ivBanner.loadImage(banner.imageUrl)
            b.tvBannerTitle.text = banner.title
            b.tvBannerCategory.text = banner.category
            b.tvBannerRating.text = "⭐ ${banner.imdbRating}"
            b.btnBannerPlay.setOnClickListener { onClick(banner) }
            b.root.setOnClickListener { onClick(banner) }
            if (banner.testMovie) b.tvFreeBadge.visibility = android.view.View.VISIBLE
            else b.tvFreeBadge.visibility = android.view.View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(a: Banner, b: Banner) = a.id == b.id
        override fun areContentsTheSame(a: Banner, b: Banner) = a == b
    }
}
