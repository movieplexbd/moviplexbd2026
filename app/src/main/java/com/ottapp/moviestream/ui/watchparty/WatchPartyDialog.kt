package com.ottapp.moviestream.ui.watchparty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ottapp.moviestream.R
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.launch

/**
 * Watch Party Dialog — Host/Join করার UI
 * MovieDetailFragment থেকে open হয়
 */
class WatchPartyDialog : DialogFragment() {

    companion object {
        const val TAG = "WatchPartyDialog"
        fun newInstance(movieId: String, movieTitle: String, videoUrl: String): WatchPartyDialog {
            return WatchPartyDialog().apply {
                arguments = Bundle().apply {
                    putString("movieId",    movieId)
                    putString("movieTitle", movieTitle)
                    putString("videoUrl",   videoUrl)
                }
            }
        }
    }

    var onRoomCreated: ((roomId: String, videoUrl: String, positionMs: Long) -> Unit)? = null
    var onRoomJoined:  ((roomId: String, videoUrl: String, positionMs: Long) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_watch_party, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieId    = arguments?.getString("movieId") ?: ""
        val movieTitle = arguments?.getString("movieTitle") ?: ""
        val videoUrl   = arguments?.getString("videoUrl") ?: ""

        val btnHost = view.findViewById<MaterialButton>(R.id.btnHostParty)
        val btnJoin = view.findViewById<MaterialButton>(R.id.btnJoinParty)
        val etCode  = view.findViewById<TextInputEditText>(R.id.etRoomCode)
        val tvInfo  = view.findViewById<TextView>(R.id.tvWatchPartyInfo)
        val layoutJoin = view.findViewById<View>(R.id.layoutJoinCode)

        tvInfo.text = "Watch Party\n\"$movieTitle\""

        // Host — room তৈরি করে
        btnHost.setOnClickListener {
            btnHost.isEnabled = false
            btnHost.text = "তৈরি হচ্ছে..."
            lifecycleScope.launch {
                try {
                    val roomId = WatchPartyManager.createRoom(movieId, movieTitle, videoUrl)
                    // Room code clipboard এ copy করে
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Watch Party Code", roomId))
                    requireContext().toast("Room Code: $roomId\n(Clipboard এ copy হয়েছে)")
                    onRoomCreated?.invoke(roomId, videoUrl, 0L)
                    dismiss()
                } catch (e: Exception) {
                    requireContext().toast("Room তৈরি করা যায়নি")
                    btnHost.isEnabled = true
                    btnHost.text = "Host করুন"
                }
            }
        }

        // Join — code দিয়ে join
        btnJoin.setOnClickListener {
            val code = etCode.text?.toString()?.trim()?.uppercase()
            if (code.isNullOrBlank() || code.length < 4) {
                etCode.error = "Room Code দিন"
                return@setOnClickListener
            }
            btnJoin.isEnabled = false
            btnJoin.text = "যোগ হচ্ছে..."
            lifecycleScope.launch {
                val state = WatchPartyManager.joinRoom(code)
                if (state == null) {
                    requireContext().toast("Room পাওয়া যায়নি। Code টি ঠিক আছে কিনা দেখুন।")
                    btnJoin.isEnabled = true
                    btnJoin.text = "Join করুন"
                } else {
                    requireContext().toast("Watch Party তে যোগ হয়েছে! ${state.memberCount} জন দেখছেন")
                    onRoomJoined?.invoke(state.roomId, state.videoUrl, state.positionMs)
                    dismiss()
                }
            }
        }

        // Join layout toggle
        layoutJoin.visibility = View.VISIBLE
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
