package com.ottapp.moviestream

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ottapp.moviestream.data.repository.AuthRepository
import com.ottapp.moviestream.databinding.ActivityLoginBinding
import com.ottapp.moviestream.util.AccessManager
import com.ottapp.moviestream.util.hide
import com.ottapp.moviestream.util.show
import com.ottapp.moviestream.util.toast
import com.ottapp.moviestream.data.model.UserDevice
import com.ottapp.moviestream.data.repository.UserRepository
import android.provider.Settings
import android.os.Build
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var authRepository: AuthRepository? = null
    private val userRepo = UserRepository()

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            authRepository = AuthRepository(this)
        } catch (e: Exception) {
            Log.e("LoginActivity", "AuthRepository init failed: ${e.message}", e)
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnEmailAction.setOnClickListener {
            handleEmailAuth()
        }

        binding.tvToggleMode.setOnClickListener {
            toggleMode()
        }
    }

    private fun toggleMode() {
        isSignUpMode = !isSignUpMode
        if (isSignUpMode) {
            binding.tilConfirmPassword.show()
            binding.tilName.show()
            binding.btnEmailAction.text = getString(R.string.btn_signup)
            binding.tvToggleMode.text   = getString(R.string.toggle_to_signin)
            binding.tvAuthTitle.text    = getString(R.string.title_signup)
        } else {
            binding.tilConfirmPassword.hide()
            binding.tilName.hide()
            binding.btnEmailAction.text = getString(R.string.btn_signin)
            binding.tvToggleMode.text   = getString(R.string.toggle_to_signup)
            binding.tvAuthTitle.text    = getString(R.string.title_signin)
        }
    }

    private fun handleEmailAuth() {
        val repo = authRepository
        if (repo == null) {
            showError("Firebase সঠিকভাবে চালু হয়নি। অ্যাপ রিস্টার্ট করুন।")
            return
        }

        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showError("ইমেইল এবং পাসওয়ার্ড দিন")
            return
        }
        if (password.length < 6) {
            showError("পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে")
            return
        }

        if (isSignUpMode) {
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val name            = binding.etName.text.toString().trim()
            if (password != confirmPassword) {
                showError("পাসওয়ার্ড মিলছে না")
                return
            }
            if (name.isEmpty()) {
                showError("আপনার নাম দিন")
                return
            }
            setLoading(true)
            lifecycleScope.launch {
                val result = repo.signUpWithEmail(email, password, name)
                result.fold(
                    onSuccess = { goToMain() },
                    onFailure = { showError(friendlyError(it.message)) }
                )
            }
        } else {
            setLoading(true)
            lifecycleScope.launch {
                val result = repo.signInWithEmail(email, password)
                result.fold(
                    onSuccess = { goToMain() },
                    onFailure = { showError(friendlyError(it.message)) }
                )
            }
        }
    }

    private fun goToMain() {
        lifecycleScope.launch {
            try {
                // Register device
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val user = userRepo.getCurrentUser()
                if (user != null) {
                    userRepo.updateDevice(user.uid, UserDevice(
                        deviceId = deviceId,
                        deviceName = deviceName,
                        lastLogin = System.currentTimeMillis(),
                        isActive = true
                    ))
                }
                
                AccessManager(this@LoginActivity).checkAccess()
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login success post-processing error: ${e.message}")
            }
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun friendlyError(msg: String?): String {
        return when {
            msg == null                           -> "অজানা সমস্যা হয়েছে"
            msg.contains("password")              -> "পাসওয়ার্ড ভুল আছে"
            msg.contains("no user") || msg.contains("identifier") -> "এই ইমেইলে কোনো অ্যাকাউন্ট নেই"
            msg.contains("already in use")        -> "এই ইমেইল দিয়ে আগেই অ্যাকাউন্ট আছে"
            msg.contains("badly formatted") || msg.contains("format") -> "ইমেইল সঠিক নয়"
            msg.contains("network")               -> "ইন্টারনেট সংযোগ নেই"
            msg.contains("CONFIGURATION_NOT_FOUND") || msg.contains("API_NOT_AVAILABLE") -> "Firebase কনফিগারেশন সমস্যা"
            else                                  -> msg
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnEmailAction.text = ""
            binding.btnEmailAction.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnEmailAction.text = if (isSignUpMode) getString(R.string.btn_signup) else getString(R.string.btn_signin)
            binding.btnEmailAction.isEnabled = true
        }
    }

    private fun showError(msg: String) {
        toast(msg)
        setLoading(false)
    }
}
