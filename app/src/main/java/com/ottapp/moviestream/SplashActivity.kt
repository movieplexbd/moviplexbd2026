package com.ottapp.moviestream

  import android.annotation.SuppressLint
  import android.content.Intent
  import android.net.Uri
  import android.os.Bundle
  import android.os.Handler
  import android.os.Looper
  import android.util.Log
  import androidx.appcompat.app.AlertDialog
  import androidx.appcompat.app.AppCompatActivity
  import androidx.lifecycle.lifecycleScope
  import com.airbnb.lottie.LottieAnimationView
  import com.airbnb.lottie.LottieDrawable
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.database.FirebaseDatabase
  import com.ottapp.moviestream.ui.onboarding.OnboardingActivity
  import com.ottapp.moviestream.util.AccessManager
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.tasks.await

  @SuppressLint("CustomSplashScreen")
  class SplashActivity : AppCompatActivity() {

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)

          try {
              setContentView(R.layout.activity_splash)

              // Start Lottie loading animation
              val lottieView = findViewById<LottieAnimationView>(R.id.lottie_loading)
              lottieView?.apply {
                  setAnimation(R.raw.lottie_loading)
                  repeatCount = LottieDrawable.INFINITE
                  playAnimation()
              }

              // Fade-in logo
              val logo = findViewById<android.widget.ImageView>(R.id.iv_splash_logo)
              logo?.apply {
                  alpha = 0f
                  animate().alpha(1f).setDuration(600).start()
              }

          } catch (e: Exception) {
              Log.e("SplashActivity", "Layout inflate error: ${e.message}", e)
          }

          lifecycleScope.launch {
              try {
                  checkUpdate()
              } catch (e: Exception) {
                  Log.e("SplashActivity", "Update check failed: ${e.message}", e)
                  proceedAfterDelay()
              }
          }
      }

      private fun proceedAfterDelay() {
          Handler(Looper.getMainLooper()).postDelayed({
              try {
                  if (!isFinishing && !isDestroyed) {
                      navigate()
                  }
              } catch (e: Exception) {
                  Log.e("SplashActivity", "Navigation error: ${e.message}", e)
                  safeNavigateTo(LoginActivity::class.java)
              }
          }, 1500)
      }

      private suspend fun checkUpdate() {
          val repository = com.ottapp.moviestream.data.repository.UpdateRepository()
          val config = repository.getUpdateConfig()
          
          if (config != null && config.isEnabled) {
              val currentVersionCode = packageManager.getPackageInfo(packageName, 0).versionCode
              val currentVersionName = packageManager.getPackageInfo(packageName, 0).versionName

              if (config.latestVersionCode > currentVersionCode) {
                  runOnUiThread {
                      val intent = Intent(this@SplashActivity, com.ottapp.moviestream.ui.update.UpdateActivity::class.java).apply {
                          putExtra("title", config.updateTitle)
                          putExtra("message", config.updateMessage)
                          putStringArrayListExtra("changelog", ArrayList(config.changelog))
                          putExtra("downloadLink", config.downloadLink)
                          putExtra("updateType", config.updateType)
                          putExtra("currentVersion", currentVersionName)
                          putExtra("latestVersion", config.latestVersionName)
                      }
                      startActivity(intent)
                      if (config.updateType == "FORCE") {
                          finish()
                      } else {
                          proceedAfterDelay()
                      }
                  }
              } else {
                  proceedAfterDelay()
              }
          } else {
              // Firebase update config নেই — GitHub Releases থেকে check করি
              checkGitHubUpdate()
          }
      }

      private suspend fun checkGitHubUpdate() {
          try {
              val ghManager = GitHubUpdateManager(this)
              val info = ghManager.checkForUpdate()
              if (info != null && info.isUpdateAvailable) {
                  val changelog = info.releaseNotes
                      .lines()
                      .filter { it.isNotBlank() }
                      .take(10)
                  runOnUiThread {
                      val intent = android.content.Intent(
                          this@SplashActivity,
                          com.ottapp.moviestream.ui.update.UpdateActivity::class.java
                      ).apply {
                          putExtra("title",          "নতুন আপডেট পাওয়া গেছে!")
                          putExtra("message",        "CineStream v${info.latestVersion} এখন পাওয়া যাচ্ছে")
                          putStringArrayListExtra("changelog", ArrayList(changelog))
                          putExtra("downloadLink",   info.downloadUrl)
                          putExtra("updateType",     info.updateType)
                          putExtra("currentVersion", info.currentVersion)
                          putExtra("latestVersion",  info.latestVersion)
                      }
                      startActivity(intent)
                      if (info.updateType == "FORCE") finish() else proceedAfterDelay()
                  }
              } else {
                  proceedAfterDelay()
              }
          } catch (e: Exception) {
              proceedAfterDelay()
          }
      }

      private fun navigate() {
          val user = FirebaseAuth.getInstance().currentUser
          if (user == null) {
              val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
              if (!prefs.getBoolean("seen", false)) {
                  safeNavigateTo(OnboardingActivity::class.java)
              } else {
                  safeNavigateTo(LoginActivity::class.java)
              }
          } else {
              safeNavigateTo(MainActivity::class.java)
          }
      }

      private fun safeNavigateTo(cls: Class<*>) {
          try {
              if (!isFinishing && !isDestroyed) {
                  startActivity(Intent(this, cls))
                  finish()
              }
          } catch (e: Exception) {
              Log.e("SplashActivity", "Navigate error: ${e.message}")
          }
      }
  }
  