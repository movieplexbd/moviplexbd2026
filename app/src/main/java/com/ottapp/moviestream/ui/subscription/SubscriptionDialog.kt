package com.ottapp.moviestream.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.R
import com.ottapp.moviestream.databinding.DialogSubscriptionBinding
import com.ottapp.moviestream.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubscriptionDialog : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SubscriptionDialog"

        private const val BKASH_NUMBER = "01913305107"
        private const val MONTHLY_PRICE = "১০"
        private const val YEARLY_PRICE = "৫০"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"

        fun newInstance() = SubscriptionDialog()
    }

    private var _binding: DialogSubscriptionBinding? = null
    private val binding get() = _binding!!

    private var selectedPlan = "monthly"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.tvBkashNumber.text = BKASH_NUMBER
        binding.tvMonthlyPrice.text = "৳ ১০"
        binding.tvYearlyPrice.text = "৳ ৫০"

        setupPlanSelection()
        setupCopyNumber()
        setupSubmit()
    }

    private fun setupPlanSelection() {
        binding.planMonthly.setOnClickListener { selectPlan("monthly") }
        binding.planYearly.setOnClickListener { selectPlan("yearly") }
        selectPlan("monthly")
    }

    private fun selectPlan(plan: String) {
        selectedPlan = plan
        binding.planMonthly.setBackgroundResource(
            if (plan == "monthly") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected
        )
        binding.planYearly.setBackgroundResource(
            if (plan == "yearly") R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected
        )
    }

    private fun setupCopyNumber() {
        binding.tvBkashNumber.setOnClickListener {
            try {
                val cm = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("bKash", BKASH_NUMBER)
                cm.setPrimaryClip(clip)
                requireContext().toast("নম্বর কপি হয়েছে!")
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun setupSubmit() {
        binding.btnSubmit.setOnClickListener {
            val trxId = binding.etTrxId.text?.toString()?.trim() ?: ""
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""

            if (trxId.isEmpty()) {
                requireContext().toast("ট্রানজেকশন আইডি দিন")
                return@setOnClickListener
            }
            if (phone.length < 11) {
                requireContext().toast("সঠিক ফোন নম্বর দিন")
                return@setOnClickListener
            }

            submitPaymentRequest(trxId, phone)
        }
    }

    private fun submitPaymentRequest(trxId: String, phone: String) {
        try {
            binding.btnSubmit.isEnabled = false
            binding.btnSubmit.text = "পাঠানো হচ্ছে..."

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_$phone"
            val data = mapOf(
                "uid"       to uid,
                "phone"     to phone,
                "trxId"     to trxId,
                "plan"      to selectedPlan,
                "price"     to if (selectedPlan == "monthly") MONTHLY_PRICE else YEARLY_PRICE,
                "timestamp" to System.currentTimeMillis(),
                "status"    to "pending"
            )

            FirebaseDatabase.getInstance(DB_URL)
                .getReference("subscription_requests")
                .push()
                .setValue(data)
                .addOnSuccessListener {
                    requireContext().toast("সাবমিট সফল! ২৪ ঘন্টার মধ্যে অ্যাকাউন্ট সক্রিয় হবে")
                    lifecycleScope.launch {
                        delay(1500)
                        dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    requireContext().toast("সমস্যা হয়েছে: ${e.message}")
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.text = "সাবমিট করুন"
                }
        } catch (e: Exception) {
            requireContext().toast("সমস্যা হয়েছে")
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = "সাবমিট করুন"
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
