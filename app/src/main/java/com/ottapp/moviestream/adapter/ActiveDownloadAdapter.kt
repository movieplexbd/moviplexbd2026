package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.databinding.ItemActiveDownloadBinding
import com.ottapp.moviestream.util.DownloadTracker
import com.ottapp.moviestream.util.loadImage

class ActiveDownloadAdapter(
    private val onCancel: (String) -> Unit
) : ListAdapter<DownloadTracker.ActiveDownload, ActiveDownloadAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemActiveDownloadBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DownloadTracker.ActiveDownload) {
            b.tvTitle.text = item.title
            b.ivThumb.loadImage(item.bannerUrl)

            if (item.progress < 0) {
                b.progressBar.isIndeterminate = true
                b.tvStatus.text = "সংযোগ করছে..."
                b.tvPercent.visibility = View.GONE
                b.tvFileSize.visibility = View.GONE
            } else {
                b.progressBar.isIndeterminate = false
                b.progressBar.progress = item.progress
                b.tvPercent.visibility = View.VISIBLE
                b.tvPercent.text = "${item.progress}%"

                // Show file size label e.g. "245.3 MB / 1.2 GB"
                if (item.sizeLabel.isNotEmpty()) {
                    b.tvFileSize.visibility = View.VISIBLE
                    b.tvFileSize.text = item.sizeLabel
                } else {
                    b.tvFileSize.visibility = View.GONE
                }
                b.tvStatus.text = "ডাউনলোড হচ্ছে..."
            }

            b.btnCancel.setOnClickListener { onCancel(item.movieId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemActiveDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<DownloadTracker.ActiveDownload>() {
        override fun areItemsTheSame(a: DownloadTracker.ActiveDownload, b: DownloadTracker.ActiveDownload) =
            a.movieId == b.movieId
        override fun areContentsTheSame(a: DownloadTracker.ActiveDownload, b: DownloadTracker.ActiveDownload) =
            a == b
    }
}
