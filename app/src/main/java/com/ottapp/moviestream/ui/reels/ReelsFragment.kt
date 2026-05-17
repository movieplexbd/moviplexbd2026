package com.ottapp.moviestream.ui.reels

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.Reel
import com.ottapp.moviestream.data.repository.ReelRepository
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReelsFragment : Fragment() {

    private var viewPager: ViewPager2? = null
    private val reels = mutableListOf<Reel>()
    
    @Inject
    lateinit var repo: ReelRepository

    private var currentAdapter: ReelsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_reels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewpager_reels)
        loadReels()
    }

    private fun loadReels() {
        lifecycleScope.launch {
            try {
                val all = repo.getAllReels()
                if (isAdded && viewPager != null) {
                    val prioritized = all.shuffled().sortedByDescending { it.movieId.isNotEmpty() }
                    reels.clear()
                    reels.addAll(prioritized)
                    setupPager()
                }
            } catch (e: Exception) {
                requireContext().toast("রিলস লোড করতে সমস্যা হয়েছে")
            }
        }
    }

    private fun setupPager() {
        currentAdapter = ReelsAdapter(reels) { reel, action ->
            when (action) {
                "search" -> {
                    val bundle = bundleOf("query" to reel.movieTitle)
                    findNavController().navigate(R.id.searchFragment, bundle)
                }
                "download" -> {
                    if (reel.movieId.isNotEmpty()) {
                        val bundle = bundleOf(Constants.EXTRA_MOVIE_ID to reel.movieId)
                        findNavController().navigate(R.id.movieDetailFragment, bundle)
                    } else if (reel.movieTitle.isNotEmpty()) {
                        val bundle = bundleOf("query" to reel.movieTitle)
                        findNavController().navigate(R.id.searchFragment, bundle)
                        requireContext().toast("সরাসরি মুভি পাওয়া যায়নি, সার্চ করা হচ্ছে")
                    } else {
                        requireContext().toast("এই রিলটির সাথে কোনো মুভি যুক্ত নেই")
                    }
                }
                "share" -> {
                    shareReel(reel)
                }
            }
        }
        
        viewPager?.apply {
            orientation = ViewPager2.ORIENTATION_VERTICAL
            this.adapter = currentAdapter
            offscreenPageLimit = 3
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentAdapter?.playAtPosition(position)
                }
            })

            setPageTransformer { page, position ->
                page.apply {
                    val absPos = Math.abs(position)
                    alpha = 1f - absPos * 0.5f
                }
            }
        }
    }

    private fun shareReel(reel: Reel) {
        val shareText = "Check out this reel: ${reel.title}\nWatch on CineStream OTT!\n${reel.videoUrl}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Reel via"))
    }

    override fun onPause() {
        super.onPause()
        currentAdapter?.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        viewPager?.let {
            currentAdapter?.playAtPosition(it.currentItem)
        }
    }

    override fun onDestroyView() {
        currentAdapter?.releaseAll()
        viewPager?.adapter = null
        viewPager = null
        super.onDestroyView()
    }
}

class ReelsAdapter(
    private val items: List<Reel>,
    private val onAction: (Reel, String) -> Unit
) : RecyclerView.Adapter<ReelsAdapter.ReelViewHolder>() {

    private val players = mutableMapOf<Int, ExoPlayer>()

    inner class ReelViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val webView: WebView = view.findViewById(R.id.webview_reel)
        val playerView: PlayerView = view.findViewById(R.id.player_view_reel)
        val progress: ProgressBar = view.findViewById(R.id.progress_reel)
        val tvTitle: TextView = view.findViewById(R.id.tv_reel_title)
        val tvMovieName: TextView = view.findViewById(R.id.tv_movie_name)
        val btnSearch: View = view.findViewById(R.id.btn_search_movie)
        val btnDownload: View = view.findViewById(R.id.btn_download_reel)
        val btnShare: View = view.findViewById(R.id.btn_share_reel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        return ReelViewHolder(v)
    }

    override fun getItemCount() = items.size

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        val reel = items[position]
        holder.tvTitle.text = reel.title
        holder.tvMovieName.text = if (reel.movieTitle.isNotEmpty()) "মুভি: ${reel.movieTitle}" else ""
        
        holder.btnSearch.setOnClickListener { onAction(reel, "search") }
        holder.btnDownload.setOnClickListener { onAction(reel, "download") }
        holder.btnShare.setOnClickListener { onAction(reel, "share") }

        // Use WebView for all reel URLs to support Cloudinary and other embeds
        holder.playerView.visibility = View.GONE
        holder.webView.visibility = View.VISIBLE
        setupEmbeddedPlayer(holder.webView, reel.videoUrl, holder.progress)
        
        // Pre-fetch next 2 items
        preFetch(position + 1)
        preFetch(position + 2)
    }

    private fun setupExoPlayer(holder: ReelViewHolder, position: Int, url: String) {
        if (url.isEmpty()) return
        
        val context = holder.view.context
        val player = players[position] ?: ExoPlayer.Builder(context).build().also {
            players[position] = it
        }
        
        holder.playerView.player = player
        player.repeatMode = Player.REPEAT_MODE_ONE
        
        if (player.mediaItemCount == 0) {
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                holder.progress.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
            }
        })
    }

    private fun preFetch(position: Int) {
        if (position >= items.size || players.containsKey(position)) return
        // We don't have context here easily without holder, but we can wait for onBind
    }

    fun playAtPosition(position: Int) {
        players.forEach { (pos, player) ->
            if (pos == position) player.play() else player.pause()
        }
    }

    fun pauseAll() {
        players.values.forEach { it.pause() }
    }

    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
    }

    override fun onViewRecycled(holder: ReelViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.bindingAdapterPosition
        players[position]?.let {
            it.release()
            players.remove(position)
        }
        try { 
            holder.webView.loadUrl("about:blank")
        } catch (e: Exception) { }
    }

    private fun extractYouTubeId(url: String): String? {
        if (url.isBlank()) return null
        val patterns = listOf(
            Regex("youtu\\.be/([^?&/]+)"),
            Regex("youtube\\.com/watch\\?v=([^?&/]+)"),
            Regex("youtube\\.com/embed/([^?&/]+)"),
            Regex("youtube\\.com/shorts/([^?&/]+)"),
            Regex("youtube\\.com/v/([^?&/]+)")
        )
        patterns.forEach { p -> 
            p.find(url)?.groupValues?.getOrNull(1)?.let { return it } 
        }
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupEmbeddedPlayer(webView: WebView, videoUrl: String, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) progress.visibility = View.GONE
            }
        }
        webView.webViewClient = WebViewClient()
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; background: #000; overflow: hidden; }
                    body, html { width: 100%; height: 100%; }
                    .video-container {
                        position: relative;
                        width: 100vw;
                        height: 100vh;
                    }
                    iframe {
                        position: absolute;
                        top: 50%;
                        left: 50%;
                        width: 100vw;
                        height: 100vh;
                        transform: translate(-50%, -50%);
                        border: none;
                    }
                </style>
            </head>
            <body>
                <div class="video-container">
                    <iframe src="$videoUrl" 
                            allow="autoplay; fullscreen; encrypted-media; picture-in-picture" 
                            allowfullscreen></iframe>
                </div>
            </body>
            </html>
        """.trimIndent()
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
