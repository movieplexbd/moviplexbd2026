package com.ottapp.moviestream.ui.subscription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ottapp.moviestream.data.repository.SubscriptionRepository
import com.ottapp.moviestream.databinding.ActivitySubscriptionBinding
import com.ottapp.moviestream.util.Constants
import kotlinx.coroutines.launch

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private val subRepo = SubscriptionRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCopyButton()
        setupSubmitButton()
        setupBackButton()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupCopyButton() {
        binding.btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip      = ClipData.newPlainText("payment_number", Constants.PAYMENT_NUMBER)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "নম্বরটি কপি হয়েছে ✓", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            val transactionId = binding.etTransactionId.text.toString().trim()

            if (transactionId.isEmpty()) {
                Toast.makeText(this, "ট্রানজেকশন আইডি লিখুন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (transactionId.length < 6) {
                Toast.makeText(this, "সঠিক ট্রানজেকশন আইডি দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

            setLoading(true)
            lifecycleScope.launch {
                val result = subRepo.submitPayment(transactionId, deviceId)
                setLoading(false)

                result.fold(
                    onSuccess = {
                        showSuccess()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@SubscriptionActivity,
                            "সমস্যা হয়েছে: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled    = !loading
        binding.btnCopy.isEnabled      = !loading
        binding.etTransactionId.isEnabled = !loading
    }

    private fun showSuccess() {
        binding.layoutForm.visibility    = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE
        binding.btnDone.setOnClickListener { finish() }
    }
}
