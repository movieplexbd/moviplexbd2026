package com.ottapp.moviestream.ui.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ottapp.moviestream.util.loadImage
import com.ottapp.moviestream.R
import com.ottapp.moviestream.util.WatchlistEntry
import com.ottapp.moviestream.util.WatchlistManager

class WatchlistFragment : Fragment() {

    private var recycler: RecyclerView? = null
    private var emptyLayout: View? = null
    private lateinit var watchlistManager: WatchlistManager
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_watchlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        watchlistManager = WatchlistManager(requireContext())
        recycler = view.findViewById(R.id.rv_watchlist)
        emptyLayout = view.findViewById(R.id.layout_empty)

        adapter = WatchlistAdapter(
            onItemClick = { entry ->
                try {
                    findNavController().navigate(
                        R.id.action_watchlist_to_detail,
                        bundleOf("extra_movie_id" to entry.movieId)
                    )
                } catch (e: Exception) { }
            },
            onRemove = { entry ->
                AlertDialog.Builder(requireContext())
                    .setTitle("সরিয়ে দিন?")
                    .setMessage("\"${entry.title}\" ওয়াচলিস্ট থেকে সরাবেন?")
                    .setPositiveButton("হ্যাঁ") { _, _ ->
                        watchlistManager.removeFromWatchlist(entry.movieId)
                        loadData()
                    }
                    .setNegativeButton("না", null)
                    .show()
            }
        )
        recycler?.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler?.adapter = adapter
        loadData()
    }

    private fun loadData() {
        val items = watchlistManager.getWatchlist()
        adapter.submitList(items)
        if (items.isEmpty()) {
            emptyLayout?.visibility = View.VISIBLE
            recycler?.visibility = View.GONE
        } else {
            emptyLayout?.visibility = View.GONE
            recycler?.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        recycler = null
        emptyLayout = null
        super.onDestroyView()
    }
}

class WatchlistAdapter(
    private val onItemClick: (WatchlistEntry) -> Unit,
    private val onRemove: (WatchlistEntry) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.VH>() {

    private val items = mutableListOf<WatchlistEntry>()

    fun submitList(list: List<WatchlistEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.iv_thumb)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_card, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.tvTitle.text = entry.title
        holder.iv.loadImage(entry.bannerUrl)
            .placeholder(R.color.surface2).into(holder.iv)
        holder.itemView.setOnClickListener { onItemClick(entry) }
        holder.itemView.setOnLongClickListener { onRemove(entry); true }
    }
}
