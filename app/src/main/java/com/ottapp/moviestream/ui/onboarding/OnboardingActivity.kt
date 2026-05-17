package com.ottapp.moviestream.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.ottapp.moviestream.LoginActivity
import com.ottapp.moviestream.R
import com.ottapp.moviestream.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardAdapter

    private val slides = listOf(
        OnboardSlide(R.drawable.app_logo, "CineStream-এ স্বাগতম!",
            "বাংলা ডাবিং মুভির সেরা অ্যাপ। হলিউড, ইন্ডিয়ান ও কোরিয়ান মুভি এখন বাংলায়।"),
        OnboardSlide(R.drawable.ic_play_circle, "২ ঘণ্টা ফ্রি ট্রায়াল",
            "প্রথমবার লগইন করলে আপনি পাবেন ২ ঘণ্টার বিনামূল্যে ট্রায়াল। কোনো কার্ড লাগবে না!"),
        OnboardSlide(R.drawable.ic_search, "মুভি খুঁজুন ও দেখুন",
            "হোম পেজ থেকে ট্রেন্ডিং মুভি দেখুন। যেকোনো মুভিতে ক্লিক → বিবরণ পড়ুন → ▶ দেখুন।"),
        OnboardSlide(R.drawable.ic_download, "ডাউনলোড করুন অফলাইনে",
            "ইন্টারনেট ছাড়াও দেখতে পারবেন। মুভি পেজে ডাউনলোড বোতাম চাপুন → ডাউনলোড ট্যাবে দেখুন।"),
        OnboardSlide(R.drawable.login_vector, "মাত্র ৳১০-তে সাবস্ক্রাইব করুন",
            "ট্রায়াল শেষে মাত্র ১০ টাকায় ১ মাসের পূর্ণ অ্যাক্সেস নিন। বিকাশ/নগদে পেমেন্ট করুন।")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OnboardAdapter(slides)
        binding.viewPager.adapter = adapter
        setupDots(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                if (position == slides.lastIndex) {
                    binding.btnNext.text = "শুরু করুন 🎬"
                    binding.btnSkip.visibility = View.GONE
                } else {
                    binding.btnNext.text = "পরবর্তী →"
                    binding.btnSkip.visibility = View.VISIBLE
                }
            }
        })

        binding.btnNext.setOnClickListener {
            val cur = binding.viewPager.currentItem
            if (cur < slides.lastIndex) {
                binding.viewPager.setCurrentItem(cur + 1, true)
            } else {
                finishOnboarding()
            }
        }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun setupDots(selected: Int) {
        binding.layoutDots.removeAllViews()
        slides.indices.forEach { i ->
            val dot = ImageView(this).apply {
                setImageResource(if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive)
                val size = if (i == selected) 12 else 8
                val lp = LinearLayout.LayoutParams(
                    (size * resources.displayMetrics.density).toInt(),
                    (size * resources.displayMetrics.density).toInt()
                ).apply { setMargins(8, 0, 8, 0) }
                layoutParams = lp
            }
            binding.layoutDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until binding.layoutDots.childCount) {
            val dot = binding.layoutDots.getChildAt(i) as ImageView
            dot.setImageResource(if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive)
            val size = if (i == selected) 12 else 8
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            lp.width = (size * resources.displayMetrics.density).toInt()
            lp.height = (size * resources.displayMetrics.density).toInt()
            dot.layoutParams = lp
        }
    }

    private fun finishOnboarding() {
        // Aligning with SplashActivity's key "seen"
        getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            .edit().putBoolean("seen", true).apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    companion object {
        fun isOnboardingShown(context: Context): Boolean =
            context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
                .getBoolean("seen", false)
    }
}
