package com.ottapp.moviestream.ui.admin

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.Episode

class AdminEpisodeAdapter(
    private val onEdit:   (Episode, Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : ListAdapter<Episode, AdminEpisodeAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(a: Episode, b: Episode) = a.episodeNumber == b.episodeNumber
            override fun areContentsTheSame(a: Episode, b: Episode) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvEpNum   : TextView       = view.findViewById(R.id.tvAdminEpNumber)
        val tvEpTitle : TextView       = view.findViewById(R.id.tvAdminEpTitle)
        val tvEpUrl   : TextView       = view.findViewById(R.id.tvAdminEpUrl)
        val tvEpFree  : TextView       = view.findViewById(R.id.tvAdminEpFree)
        val btnEdit   : MaterialButton = view.findViewById(R.id.btnAdminEpEdit)
        val btnDelete : MaterialButton = view.findViewById(R.id.btnAdminEpDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_episode, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ep = getItem(position)
        holder.tvEpNum.text   = "Ep ${ep.episodeNumber}"
        holder.tvEpTitle.text = ep.title.ifBlank { "Untitled" }
        holder.tvEpUrl.text   = ep.streamUrl.take(50).ifBlank { "No URL" }
        holder.tvEpFree.text  = if (ep.isFree) "🆓 Free" else "🔒 Premium"
        holder.btnEdit.setOnClickListener   { onEdit(ep, position) }
        holder.btnDelete.setOnClickListener { onDelete(position) }
    }
}
