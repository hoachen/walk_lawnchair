package com.ur.apps.walk.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.android.launcher3.R
import com.android.launcher3.databinding.DialogRateUsBinding
import com.ur.apps.walk.step.utils.SharedPreferencesUtils

class RateUsDialog : DialogFragment() {

    private var _binding: DialogRateUsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogRateUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (!fromUser || rating == 0f) return@setOnRatingBarChangeListener

            // Mark as rated
            val prefs = SharedPreferencesUtils(requireContext())
            prefs.setParam(PREF_KEY_HAS_RATED, true)

            if (rating >= 5f) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "market://details?id=${requireContext().packageName}".toUri()
                    }
                    startActivity(intent)
                    dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Show thanks
                binding.tvTitle.visibility = View.GONE
                binding.ratingBar.visibility = View.GONE
                binding.tvMaybeLater.visibility = View.GONE
                binding.tvDesc.text = getString(R.string.feedback_thanks)
                binding.tvBtnDismiss.visibility = View.VISIBLE
            }
        }

        binding.tvMaybeLater.setOnClickListener {
            dismiss()
        }

        binding.tvBtnDismiss.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RateUsDialog"
        const val PREF_KEY_SHOULD_RATE_HINT_FIRST_DAY = "pref_key_should_rate_hint1"
        const val PREF_KEY_SHOULD_RATE_HINT_SECOND_DAY = "pref_key_should_rate_hint2"
        const val PREF_KEY_SHOULD_RATE_HINT_THIRD_DAY = "pref_key_should_rate_hint3"


        const val PREF_KEY_SHOULD_RATE_HINT_FIRST_DAY_SHOWED = "pref_key_should_rate_hint_showed1"
        const val PREF_KEY_SHOULD_RATE_HINT_SECOND_DAY_SHOWED = "pref_key_should_rate_hint_showed2"
        const val PREF_KEY_SHOULD_RATE_HINT_THIRD_DAY_SHOWED = "pref_key_should_rate_hint_showed3"
        const val PREF_KEY_HAS_RATED = "has_rated_us"

        fun newInstance(): RateUsDialog {
            return RateUsDialog()
        }
    }
}
