package com.ottapp.moviestream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Episode
import com.ottapp.moviestream.databinding.ItemEpisodeBinding
import com.ottapp.moviestream.util.loadImage

class EpisodeAdapter(
    private val hasAccess: Boolean,
    private val onPlay:           (Episode) -> Unit,
    private val onLockedClick:    () -> Unit,
    private val onDownload:       ((Episode) -> Unit)? = null,   // ← নতুন: episode download
    private val isDownloaded:     ((Episode) -> Boolean)? = null // ← download check
) : ListAdapter<Episode, EpisodeAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(a: Episode, b: Episode) = a.episodeNumber == b.episodeNumber
            override fun areContentsTheSame(a: Episode, b: Episode) = a == b
        }
    }

    inner class VH(val b: ItemEpisodeBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ep = getItem(position)
        val b  = holder.b

        b.tvEpisodeNumber.text   = "E${ep.episodeNumber}"
        b.tvEpisodeTitle.text    = ep.title.ifBlank { "Episode ${ep.episodeNumber}" }
        b.tvEpisodeDuration.text = ep.duration.ifBlank { "" }
        b.tvEpisodeDesc.text     = ep.description.ifBlank { "" }

        if (ep.thumbnailUrl.isNotBlank()) b.ivEpisodeThumbnail.loadImage(ep.thumbnailUrl)

        val canPlay = hasAccess || ep.isFree
        b.tvFreeBadge.visibility = if (ep.isFree) View.VISIBLE else View.GONE
        b.ivLock.visibility      = if (!canPlay && !ep.isFree) View.VISIBLE else View.GONE

        // ── Download button ─────────────────────────────────────────────────
        val dlUrl = ep.downloadUrl.ifBlank { ep.streamUrl }
        if (onDownload != null && dlUrl.isNotBlank() && canPlay) {
            val downloaded = isDownloaded?.invoke(ep) ?: false
            if (downloaded) {
                b.btnDownloadEpisode.visibility    = View.GONE
                b.tvEpisodeDownloaded.visibility   = View.VISIBLE
            } else {
                b.btnDownloadEpisode.visibility    = View.VISIBLE
                b.tvEpisodeDownloaded.visibility   = View.GONE
                b.btnDownloadEpisode.setOnClickListener { onDownload.invoke(ep) }
            }
        } else {
            b.btnDownloadEpisode.visibility  = View.GONE
            b.tvEpisodeDownloaded.visibility = View.GONE
        }

        b.root.setOnClickListener          { if (canPlay) onPlay(ep) else onLockedClick() }
        b.btnPlayEpisode.setOnClickListener { if (canPlay) onPlay(ep) else onLockedClick() }
    }
}
