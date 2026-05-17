package com.ottapp.moviestream.ui.player

  import android.app.PictureInPictureParams
  import android.content.SharedPreferences
  import android.content.res.Configuration
  import android.os.Build
  import android.os.Bundle
  import android.os.Handler
  import android.os.Looper
  import android.content.Context
  import android.media.AudioManager
  import android.util.Log
  import android.util.Rational
  import android.view.GestureDetector
  import android.view.MotionEvent
  import android.view.View
  import android.view.WindowInsets
  import android.view.WindowInsetsController
  import androidx.annotation.OptIn
  import androidx.appcompat.app.AppCompatActivity
  import androidx.core.view.GestureDetectorCompat
  import androidx.media3.common.MediaItem
  import androidx.media3.common.PlaybackException
  import androidx.media3.common.Player
  import androidx.media3.common.util.UnstableApi
  import androidx.media3.exoplayer.ExoPlayer
  import com.ottapp.moviestream.R
  import com.ottapp.moviestream.databinding.ActivityPlayerBinding
  import com.ottapp.moviestream.util.Constants
  import com.ottapp.moviestream.util.hide
  import com.ottapp.moviestream.util.show
  import com.ottapp.moviestream.util.toast
  import com.ottapp.moviestream.util.toFormattedTime
import com.ottapp.moviestream.util.WatchHistoryManager
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.UserActivity
import com.ottapp.moviestream.data.repository.UserRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.ottapp.moviestream.util.ContinueWatchingSyncManager
import com.ottapp.moviestream.ui.watchparty.WatchPartyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

  @OptIn(UnstableApi::class)
  class PlayerActivity : AppCompatActivity() {

      companion object {
          private const val TAG = "PlayerActivity"
          private const val HIDE_DELAY_MS = 4000L
      }

      private lateinit var binding: ActivityPlayerBinding
      private var player: ExoPlayer? = null
      private lateinit var prefs: SharedPreferences
       private lateinit var watchHistoryManager: WatchHistoryManager
      private val userRepo = UserRepository()
      private var movieId    = ""
      private var movieTitle = ""
      private var videoUrl   = ""

      private val hideHandler  = Handler(Looper.getMainLooper())
      private val hideRunnable = Runnable { hideControls() }
      private var controlsVisible = true

      private val speedLabels = arrayOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
      private val speedValues = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
      private var speedIndex  = 2

      private var subtitleEnabled = false

    // ── Video Quality Selector ─────────────────────────────────────────────────
    private val qualityLabels = arrayOf("Auto", "1080p", "720p", "480p", "360p")
    private var currentQualityIndex = 0

    // ── Watch Party ───────────────────────────────────────────────────────────
    private var watchPartyRoomId: String? = null
    private var isWatchPartyHost = false
    private var watchPartySyncJob: Job? = null
    private val watchPartyPushInterval = 5000L  // 5 sec এ একবার sync

    // ── Cross-device Continue Watching ────────────────────────────────────────
    private lateinit var cwSyncManager: ContinueWatchingSyncManager

      private val progressHandler = Handler(Looper.getMainLooper())
      private val progressLoop = object : Runnable {
          override fun run() {
              updateProgressBar()
              progressHandler.postDelayed(this, 500)
          }
      }

      private lateinit var gestureDetector: GestureDetectorCompat
      private lateinit var audioManager: AudioManager
      private var maxVolume = 0
      private var currentVolume = 0
      private var currentBrightness = 0.5f

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          try {
              binding = ActivityPlayerBinding.inflate(layoutInflater)
              setContentView(binding.root)
              hideSystemUI()

              prefs      = getSharedPreferences("ott_prefs", MODE_PRIVATE)
              watchHistoryManager = WatchHistoryManager(this)
              cwSyncManager = ContinueWatchingSyncManager(this)
              movieId    = intent.getStringExtra(Constants.EXTRA_MOVIE_ID)    ?: ""
              movieTitle = intent.getStringExtra(Constants.EXTRA_MOVIE_TITLE) ?: "Movie"
              videoUrl   = intent.getStringExtra(Constants.EXTRA_VIDEO_URL)   ?: ""

              // Watch Party extras
              watchPartyRoomId  = intent.getStringExtra("watch_party_room_id")
              isWatchPartyHost  = intent.getBooleanExtra("watch_party_is_host", false)
              val wpStartPos    = intent.getLongExtra("watch_party_start_pos", -1L)

              if (videoUrl.isBlank()) {
                  toast("ভিডিও URL পাওয়া যায়নি")
                  finish()
                  return
              }

              // Override start pos with watch party position if guest
              if (wpStartPos >= 0L && !isWatchPartyHost) {
                  intent.putExtra(Constants.EXTRA_START_POS, wpStartPos)
              }

              binding.tvPlayerTitle.text = movieTitle

              audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
              maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

              setupGestureDetector()
              setupPlayer()
              setupButtons()

          } catch (e: Exception) {
              Log.e(TAG, "onCreate error: ${e.message}", e)
              toast("Player শুরু করতে সমস্যা")
              finish()
          }
      }

      // ── Gesture Detector ──────────────────────────────────────────────────────

      private fun setupGestureDetector() {
          gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
              override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                  toggleControls()
                  return true
              }

              override fun onDoubleTap(e: MotionEvent): Boolean {
                  val screenWidth = binding.root.width
                  if (e.x < screenWidth / 2) {
                      seekBy(-10_000)
                      showSeekAnimation(forward = false)
                  } else {
                      seekBy(+10_000)
                      showSeekAnimation(forward = true)
                  }
                  return true
              }
          })

          var isHorizontalSwipe = false
          var isVerticalSwipe   = false
          var startX = 0f
          var startY = 0f
          var startPos = 0L
          var startVolume = 0
          var startBrightness = 0f

          binding.gestureArea.setOnTouchListener { _, event ->
              gestureDetector.onTouchEvent(event)
              val screenWidth  = binding.root.width
              val screenHeight = binding.root.height

              when (event.action) {
                  MotionEvent.ACTION_DOWN -> {
                      startX = event.x
                      startY = event.y
                      startPos = player?.currentPosition ?: 0L
                      startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                      startBrightness = window.attributes.screenBrightness.let { if (it < 0) 0.5f else it }
                      isHorizontalSwipe = false
                      isVerticalSwipe   = false
                  }
                  MotionEvent.ACTION_MOVE -> {
                      val deltaX = event.x - startX
                      val deltaY = startY - event.y

                      if (!isHorizontalSwipe && !isVerticalSwipe) {
                          if (abs(deltaX) > 30f && abs(deltaX) > abs(deltaY)) {
                              isHorizontalSwipe = true
                          } else if (abs(deltaY) > 30f && abs(deltaY) > abs(deltaX)) {
                              isVerticalSwipe = true
                          }
                      }

                      if (isHorizontalSwipe) {
                          val seekDelta = (deltaX * 200L).toLong()
                          val maxDur = player?.duration?.coerceAtLeast(0L) ?: 0L
                          val newPos = (startPos + seekDelta).coerceIn(0L, maxDur)
                          val sign = if (deltaX > 0) "+" else ""
                          binding.tvSeekIndicator.text = "$sign${seekDelta / 1000}s"
                          binding.tvSeekIndicator.show()
                          binding.tvPosition.text = newPos.toFormattedTime()
                      } else if (isVerticalSwipe) {
                          val percent = deltaY / screenHeight
                          if (startX < screenWidth / 2) {
                              val newBrightness = (startBrightness + percent).coerceIn(0f, 1f)
                              val lp = window.attributes
                              lp.screenBrightness = newBrightness
                              window.attributes = lp
                              binding.brightnessContainer.show()
                              binding.brightnessProgress.progress = (newBrightness * 100).toInt()
                              try { binding.tvBrightnessValue.text = "${(newBrightness * 100).toInt()}%" } catch (_: Exception) {}
                          } else {
                              val volumeDelta = (percent * maxVolume).toInt()
                              val newVolume = (startVolume + volumeDelta).coerceIn(0, maxVolume)
                              audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                              binding.volumeContainer.show()
                              binding.volumeProgress.progress = (newVolume.toFloat() / maxVolume * 100).toInt()
                              try { binding.tvVolumeValue.text = "${(newVolume.toFloat() / maxVolume * 100).toInt()}%" } catch (_: Exception) {}
                          }
                      }
                  }
                  MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                      if (isHorizontalSwipe) {
                          val deltaX = event.x - startX
                          val seekDelta = (deltaX * 200L).toLong()
                          val maxDur = player?.duration?.coerceAtLeast(0L) ?: 0L
                          val newPos = (startPos + seekDelta).coerceIn(0L, maxDur)
                          player?.seekTo(newPos)
                      }
                      binding.tvSeekIndicator.hide()
                      binding.brightnessContainer.hide()
                      binding.volumeContainer.hide()
                      isHorizontalSwipe = false
                      isVerticalSwipe   = false
                  }
              }
              true
          }
      }

      // ── Player ────────────────────────────────────────────────────────────────

      private fun setupPlayer() {
          try {
              val exo = ExoPlayer.Builder(this).build()
              player = exo
              binding.playerView.player = exo
              exo.setMediaItem(MediaItem.fromUri(videoUrl))

              val saved = try {
                  prefs.getLong(Constants.PREF_PLAYBACK_POSITION + movieId, 0L)
              } catch (e: Exception) { 0L }
              if (saved > 5000L) exo.seekTo(saved)

              exo.prepare()
              exo.playWhenReady = true

              exo.addListener(object : Player.Listener {
                  override fun onPlaybackStateChanged(state: Int) {
                      when (state) {
                          Player.STATE_BUFFERING -> {
                              binding.progressBuffering.show()
                              binding.loadingOverlay.show()
                          }
                          Player.STATE_READY -> {
                              binding.progressBuffering.hide()
                              binding.loadingOverlay.hide()
                              startProgressLoop()
                              scheduleHide()
                          }
                          Player.STATE_ENDED -> {
                              binding.btnPlayPause.setImageResource(R.drawable.ic_replay)
                              savePosition(0L)
                              showControls(autoHide = false)
                          }
                          else -> {}
                      }
                  }

                  override fun onIsPlayingChanged(isPlaying: Boolean) {
                      binding.btnPlayPause.setImageResource(
                          if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                      )
                  }

                  override fun onPlayerError(error: PlaybackException) {
                      binding.progressBuffering.hide()
                      binding.loadingOverlay.hide()
                      toast("ভিডিও লোড করতে সমস্যা: ${error.message}")
                      showControls(autoHide = false)
                  }
              })
          } catch (e: Exception) {
              Log.e(TAG, "setupPlayer error: ${e.message}", e)
              toast("Player শুরু করতে সমস্যা")
              finish()
          }
      }

      // ── Buttons ───────────────────────────────────────────────────────────────

      private fun setupButtons() {
          binding.btnBack.setOnClickListener { finish() }

          binding.btnPlayPause.setOnClickListener {
              val p = player ?: return@setOnClickListener
              if (p.playbackState == Player.STATE_ENDED) {
                  p.seekTo(0); p.play()
              } else {
                  if (p.isPlaying) p.pause() else p.play()
              }
              scheduleHide()
          }

          binding.btnSeekBack.setOnClickListener {
              seekBy(-10_000)
              showSeekAnimation(forward = false)
              scheduleHide()
          }

          binding.btnSeekForward.setOnClickListener {
              seekBy(+10_000)
              showSeekAnimation(forward = true)
              scheduleHide()
          }

          // Speed — cycle through speeds on each tap (Material3 Chip)
          binding.tvSpeed.setOnClickListener {
              speedIndex = (speedIndex + 1) % speedValues.size
              player?.setPlaybackSpeed(speedValues[speedIndex])
              binding.tvSpeed.text = speedLabels[speedIndex]
              scheduleHide()
          }

          // Subtitle toggle
          binding.btnSubtitle.setOnClickListener {
              subtitleEnabled = !subtitleEnabled
              binding.btnSubtitle.alpha = if (subtitleEnabled) 1f else 0.5f
              val p = player ?: return@setOnClickListener
              if (subtitleEnabled) {
                  p.trackSelectionParameters = p.trackSelectionParameters
                      .buildUpon()
                      .setPreferredTextLanguages("ben", "hin", "eng")
                      .build()
              } else {
                  p.trackSelectionParameters = p.trackSelectionParameters
                      .buildUpon()
                      .setDisabledTrackTypes(setOf(androidx.media3.common.C.TRACK_TYPE_TEXT))
                      .build()
              }
              scheduleHide()
          }
          binding.btnSubtitle.alpha = 0.5f  // disabled by default

          binding.btnPip.setOnClickListener { enterPip() }

          // ── Video Quality Selector ─────────────────────────────────────────
          try {
              binding.btnQuality?.setOnClickListener {
                  showQualityDialog()
                  scheduleHide()
              }
          } catch (e: Exception) { /* optional button */ }

          // ── Watch Party Guest sync ─────────────────────────────────────────
          watchPartyRoomId?.let { roomId ->
              if (!isWatchPartyHost) {
                  startWatchPartyGuestSync(roomId)
              } else {
                  startWatchPartyHostPush(roomId)
              }
          }

          binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
              override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                  if (fromUser) {
                      val dur = player?.duration?.coerceAtLeast(1L) ?: 1L
                      binding.tvPosition.text = (progress.toLong() * dur / 1000L).toFormattedTime()
                  }
              }
              override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                  progressHandler.removeCallbacks(progressLoop)
                  cancelAutoHide()
              }
              override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                  val p = player ?: return
                  val dur = p.duration.coerceAtLeast(1L)
                  val pos = ((seekBar?.progress ?: 0).toLong() * dur / 1000L).coerceAtLeast(0L)
                  p.seekTo(pos)
                  startProgressLoop()
                  scheduleHide()
              }
          })
      }

      // ── Controls Show / Hide ──────────────────────────────────────────────────

      private fun toggleControls() {
          if (controlsVisible) hideControls() else showControls()
      }

      private fun showControls(autoHide: Boolean = true) {
          controlsVisible = true
          binding.controlsOverlay.animate().cancel()
          binding.controlsOverlay.alpha = 1f
          binding.controlsOverlay.visibility = View.VISIBLE
          if (autoHide) scheduleHide()
      }

      private fun hideControls() {
          controlsVisible = false
          binding.controlsOverlay.animate()
              .alpha(0f)
              .setDuration(300)
              .withEndAction {
                  if (!controlsVisible) binding.controlsOverlay.visibility = View.INVISIBLE
              }
              .start()
      }

      private fun scheduleHide() {
          cancelAutoHide()
          hideHandler.postDelayed(hideRunnable, HIDE_DELAY_MS)
      }

      private fun cancelAutoHide() {
          hideHandler.removeCallbacks(hideRunnable)
      }

      // ── Helpers ───────────────────────────────────────────────────────────────

      private fun seekBy(deltaMs: Long) {
          val p = player ?: return
          val newPos = (p.currentPosition + deltaMs).coerceIn(0L, p.duration.coerceAtLeast(0L))
          p.seekTo(newPos)
      }

      private fun showSeekAnimation(forward: Boolean) {
          try {
              val indicator = if (forward) binding.seekForwardIndicator else binding.seekBackIndicator
              indicator.alpha = 0f
              indicator.visibility = View.VISIBLE
              indicator.animate().alpha(1f).setDuration(120).withEndAction {
                  indicator.animate().alpha(0f).setStartDelay(350).setDuration(250).withEndAction {
                      indicator.visibility = View.GONE
                  }.start()
              }.start()
          } catch (e: Exception) {
              Log.e(TAG, "Seek animation error: ${e.message}")
          }
      }

      private fun startProgressLoop() {
          progressHandler.removeCallbacks(progressLoop)
          progressHandler.post(progressLoop)
      }

      private fun updateProgressBar() {
          val p = player ?: return
          val duration = p.duration.coerceAtLeast(0L)
          val position = p.currentPosition
          if (duration > 0) {
              binding.seekBar.progress = ((position * 1000L) / duration).toInt()
          }
          binding.tvPosition.text = position.toFormattedTime()
          binding.tvDuration.text = duration.toFormattedTime()
      }

      private fun enterPip() {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              try {
                  val params = PictureInPictureParams.Builder()
                      .setAspectRatio(Rational(16, 9))
                      .build()
                  enterPictureInPictureMode(params)
              } catch (e: Exception) {
                  toast("PiP সাপোর্টেড নয়")
              }
          }
      }

      override fun onPictureInPictureModeChanged(inPiP: Boolean, cfg: Configuration) {
          super.onPictureInPictureModeChanged(inPiP, cfg)
          if (inPiP) {
              binding.controlsOverlay.visibility = View.INVISIBLE
              cancelAutoHide()
          } else {
              showControls()
          }
      }

      private fun hideSystemUI() {
          try {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                  window.insetsController?.let {
                      it.hide(WindowInsets.Type.systemBars())
                      it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                  }
              } else {
                  @Suppress("DEPRECATION")
                  window.decorView.systemUiVisibility = (
                      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                          or View.SYSTEM_UI_FLAG_FULLSCREEN
                          or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                          or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                          or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                          or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                  )
              }
          } catch (e: Exception) {
              Log.e(TAG, "hideSystemUI error: ${e.message}")
          }
      }

      private fun savePosition(pos: Long) {
          try {
              val duration = player?.duration ?: 0L
              if (movieId.isNotEmpty() && movieTitle.isNotEmpty()) {
                  val position = pos.takeIf { it > 0L } ?: (player?.currentPosition ?: 0L)
                  prefs.edit().putLong(Constants.PREF_PLAYBACK_POSITION + movieId, position).apply()
                  if (position > 3000L && duration > 10_000L) {
                      watchHistoryManager.saveProgress(Movie(id = movieId, title = movieTitle), position, duration)
                      
                      // Cross-device sync (Firebase)
                      lifecycleScope.launch {
                          try {
                              cwSyncManager.syncProgress(
                                  movieId   = movieId,
                                  title     = movieTitle,
                                  bannerUrl = "",
                                  category  = "",
                                  positionMs = position,
                                  durationMs = duration
                              )
                          } catch (e: Exception) { /* offline ok */ }
                      }

                      // Log to server
                      lifecycleScope.launch {
                          try {
                              val user = userRepo.getCurrentUser()
                              if (user != null) {
                                  userRepo.logActivity(user.uid, UserActivity(
                                      movieId = movieId,
                                      movieTitle = movieTitle,
                                      timestamp = System.currentTimeMillis(),
                                      durationWatched = position,
                                      action = "watch"
                                  ))
                              }
                          } catch (e: Exception) { /* ignore */ }
                      }
                  }
              }
          } catch (e: Exception) {
              Log.e(TAG, "savePosition error: ${e.message}")
          }
      }

      override fun onUserLeaveHint() {
          super.onUserLeaveHint()
          if (player?.isPlaying == true) {
              enterPip()
          }
      }

      override fun onStop() {
          super.onStop()
          try {
              val inPiP = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false
              if (!inPiP) {
                  savePosition(0L)
                  player?.pause()
              }
          } catch (e: Exception) {
              Log.e(TAG, "onStop error: ${e.message}")
          }
      }

      override fun onDestroy() {
          super.onDestroy()
          try {
              progressHandler.removeCallbacks(progressLoop)
              hideHandler.removeCallbacks(hideRunnable)
              player?.release()
              player = null
          } catch (e: Exception) {
              Log.e(TAG, "onDestroy error: ${e.message}")
          }
      }
  
    // ── Video Quality Selector ────────────────────────────────────────────────

    private fun showQualityDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("ভিডিও মান নির্বাচন করুন")
        builder.setSingleChoiceItems(qualityLabels, currentQualityIndex) { dialog, which ->
            currentQualityIndex = which
            applyQuality(which)
            try { binding.btnQuality?.text = if (which == 0) "HD" else qualityLabels[which] } catch (e: Exception) {}
            dialog.dismiss()
        }
        builder.show()
    }

    /**
     * ExoPlayer এ quality apply করি
     * HLS stream: track selection দিয়ে bitrate limit করি
     * Direct MP4: resolution by file URL (যদি multiple quality URL থাকে)
     */
    private fun applyQuality(qualityIndex: Int) {
        val p = player ?: return
        val maxHeight = when (qualityIndex) {
            0 -> Int.MAX_VALUE   // Auto
            1 -> 1080
            2 -> 720
            3 -> 480
            4 -> 360
            else -> Int.MAX_VALUE
        }
        p.trackSelectionParameters = p.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .build()
        toast("মান: ${qualityLabels[qualityIndex]}")
    }

    // ── Watch Party ───────────────────────────────────────────────────────────

    /** Guest: Host এর playback state observe করে sync রাখে */
    private fun startWatchPartyGuestSync(roomId: String) {
        watchPartySyncJob = lifecycleScope.launch {
            WatchPartyManager.observeRoom(roomId).collectLatest { state ->
                val p = player ?: return@collectLatest
                val diff = kotlin.math.abs(p.currentPosition - state.positionMs)
                // 3 সেকেন্ড এর বেশি diff থাকলে seek করি
                if (diff > 3000L) {
                    p.seekTo(state.positionMs)
                }
                if (state.isPlaying && !p.isPlaying) p.play()
                else if (!state.isPlaying && p.isPlaying) p.pause()
            }
        }
        // Guest এর জন্য player control disable করি (Host control করবে)
        toast("Watch Party: Host এর সাথে sync হচ্ছে...")
    }

    /** Host: নিজের state Firebase এ push করে */
    private fun startWatchPartyHostPush(roomId: String) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val p = player ?: return
                lifecycleScope.launch {
                    WatchPartyManager.pushPlaybackState(roomId, p.currentPosition, p.isPlaying)
                }
                handler.postDelayed(this, watchPartyPushInterval)
            }
        }
        handler.post(runnable)
        toast("Watch Party শুরু হয়েছে! Room: $roomId")
    }


}