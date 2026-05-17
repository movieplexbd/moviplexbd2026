package com.ottapp.moviestream.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Reel
import com.ottapp.moviestream.databinding.ItemAdminMovieBinding

class AdminReelAdapter(
    private val onEdit: (Reel) -> Unit,
    private val onDelete: (Reel) -> Unit
) : ListAdapter<Reel, AdminReelAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemAdminMovieBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(reel: Reel) {
            b.tvTitle.text = reel.title.ifEmpty { "শিরোনাম নেই" }
            b.tvCategory.text = "রিলস"
            b.tvRating.text = ""
            b.tvFree.text = "রিলস"
            b.btnEdit.setOnClickListener { onEdit(reel) }
            b.btnDelete.setOnClickListener { onDelete(reel) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAdminMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<Reel>() {
        override fun areItemsTheSame(a: Reel, b: Reel) = a.id == b.id
        override fun areContentsTheSame(a: Reel, b: Reel) = a == b
    }
}
