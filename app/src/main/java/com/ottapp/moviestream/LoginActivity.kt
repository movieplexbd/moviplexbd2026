package com.ottapp.moviestream

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ottapp.moviestream.data.repository.AuthRepository
import com.ottapp.moviestream.databinding.ActivityLoginBinding
import com.ottapp.moviestream.util.AccessManager
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
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            authRepository = AuthRepository(this)
        } catch (e: Exception) {
            Log.e("LoginActivity", "AuthRepository init failed: ${e.message}", e)
        }

        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                handleGoogleAuth(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Google sign in failed", e)
                showError("Google লগইন ব্যর্থ হয়েছে: ${e.message}")
            }
        } else {
            setLoading(false)
        }
    }

    private fun setupClickListeners() {
        binding.btnGoogleLogin.setOnClickListener {
            setLoading(true)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleGoogleAuth(idToken: String) {
        val repo = authRepository
        if (repo == null) {
            showError("Firebase সঠিকভাবে চালু হয়নি। অ্যাপ রিস্টার্ট করুন।")
            return
        }

        lifecycleScope.launch {
            val result = repo.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { goToMain() },
                onFailure = { showError(friendlyError(it.message)) }
            )
        }
    }

    private fun goToMain() {
        lifecycleScope.launch {
            try {
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
            msg.contains("network")               -> "ইন্টারনেট সংযোগ নেই"
            msg.contains("CONFIGURATION_NOT_FOUND") || msg.contains("API_NOT_AVAILABLE") -> "Firebase কনফিগারেশন সমস্যা"
            else                                  -> msg
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnGoogleLogin.text = ""
            binding.btnGoogleLogin.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnGoogleLogin.text = "Google দিয়ে লগইন করুন"
            binding.btnGoogleLogin.isEnabled = true
        }
    }

    private fun showError(msg: String) {
        toast(msg)
        setLoading(false)
    }
}
