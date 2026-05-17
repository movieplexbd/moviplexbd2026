package com.ottapp.moviestream.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ottapp.moviestream.data.model.Actor
import com.ottapp.moviestream.data.repository.ActorRepository
import com.ottapp.moviestream.databinding.DialogAddEditActorBinding
import com.ottapp.moviestream.databinding.FragmentAdminActorsBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

class AdminActorsFragment : Fragment() {

    private var _binding: FragmentAdminActorsBinding? = null
    private val binding get() = _binding!!
    private val repo = ActorRepository()
    private lateinit var adapter: AdminActorAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminActorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminActorAdapter(
            onEdit = { showAddEditDialog(it) },
            onDelete = { showDeleteConfirm(it) }
        )

        binding.rvActors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActors.adapter = adapter

        binding.etSearch.addTextChangedListener { adapter.filter(it.toString()) }
        binding.fabAdd.setOnClickListener { showAddEditDialog(null) }

        loadActors()
    }

    private fun loadActors() {
        _binding?.progressBar?.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val actors = repo.getAllActors()
                if (_binding == null) return@launch
                adapter.submitList(actors)
                binding.tvCount.text = "${actors.size} জন"
            } catch (e: Exception) {
                context?.toast("লোড করতে সমস্যা হয়েছে")
            } finally {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAddEditDialog(actor: Actor?) {
        val dialogBinding = DialogAddEditActorBinding.inflate(layoutInflater)
        val isEdit = actor != null

        if (isEdit) {
            dialogBinding.etName.setText(actor?.name)
            dialogBinding.etImageUrl.setText(actor?.imageUrl)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "অভিনেতা এডিট করুন" else "নতুন অভিনেতা যোগ করুন")
            .setView(dialogBinding.root)
            .setPositiveButton("সেভ") { _, _ ->
                val name = dialogBinding.etName.text.toString().trim()
                val imageUrl = dialogBinding.etImageUrl.text.toString().trim()

                if (name.isEmpty()) {
                    context?.toast("নাম দিন")
                    return@setPositiveButton
                }

                val newActor = Actor(
                    id = actor?.id ?: "",
                    name = name,
                    imageUrl = imageUrl
                )

                lifecycleScope.launch {
                    try {
                        if (isEdit) repo.updateActor(newActor) else repo.addActor(newActor)
                        context?.toast("সফল হয়েছে ✓")
                        if (_binding != null) loadActors()
                    } catch (e: Exception) {
                        context?.toast("সমস্যা হয়েছে: ${e.message}")
                    }
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun showDeleteConfirm(actor: Actor) {
        AlertDialog.Builder(requireContext())
            .setTitle("ডিলিট নিশ্চিত করুন")
            .setMessage("${actor.name} কে কি ডিলিট করতে চান?")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.deleteActor(actor.id)
                        context?.toast("ডিলিট হয়েছে ✓")
                        if (_binding != null) loadActors()
                    } catch (e: Exception) {
                        context?.toast("সমস্যা হয়েছে: ${e.message}")
                    }
                }
            }
            .setNegativeButton("না", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
