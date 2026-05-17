package com.ottapp.moviestream.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.data.model.Actor
import com.ottapp.moviestream.databinding.ItemAdminActorBinding
import com.ottapp.moviestream.util.loadImage

class AdminActorAdapter(
    private val onEdit: (Actor) -> Unit,
    private val onDelete: (Actor) -> Unit
) : RecyclerView.Adapter<AdminActorAdapter.ActorViewHolder>() {

    private var actors = listOf<Actor>()
    private var filteredActors = listOf<Actor>()

    fun submitList(list: List<Actor>) {
        actors = list
        filteredActors = list
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredActors = if (query.isEmpty()) {
            actors
        } else {
            actors.filter { it.name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActorViewHolder {
        val binding = ItemAdminActorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActorViewHolder, position: Int) {
        holder.bind(filteredActors[position])
    }

    override fun getItemCount(): Int = filteredActors.size

    inner class ActorViewHolder(private val binding: ItemAdminActorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(actor: Actor) {
            binding.tvName.text = actor.name
            binding.ivActor.loadImage(actor.imageUrl)
            binding.btnEdit.setOnClickListener { onEdit(actor) }
            binding.btnDelete.setOnClickListener { onDelete(actor) }
        }
    }
}
