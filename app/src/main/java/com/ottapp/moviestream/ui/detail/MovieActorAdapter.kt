package com.ottapp.moviestream.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Actor
import com.ottapp.moviestream.databinding.ItemActorCircleBinding
import com.ottapp.moviestream.util.loadImage

class MovieActorAdapter(
    private val onActorClick: (Actor) -> Unit
) : ListAdapter<Actor, MovieActorAdapter.ActorViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActorViewHolder {
        val binding = ItemActorCircleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActorViewHolder(private val binding: ItemActorCircleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(actor: Actor) {
            binding.tvName.text = actor.name
            binding.ivActor.loadImage(actor.imageUrl)
            binding.root.setOnClickListener { onActorClick(actor) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Actor>() {
        override fun areItemsTheSame(a: Actor, b: Actor) = a.id == b.id
        override fun areContentsTheSame(a: Actor, b: Actor) = a == b
    }
}
