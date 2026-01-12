package com.ur.apps.walk.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.android.launcher3.databinding.DialogSetDefaultLauncherBinding

import com.ur.apps.utils.CheckAndExitUtils
import com.ur.apps.walk.utils.LauncherDefaultUtils

class SetDefaultLauncherDialog : DialogFragment() {

    private var _binding: DialogSetDefaultLauncherBinding? = null
    private val binding get() = _binding!!

    private val checkHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            context?.let { ctx ->
                val isDefault = LauncherDefaultUtils.isDefaultLauncher(ctx)
                android.util.Log.i(TAG, "Checking default launcher status: $isDefault")
                if (isDefault) {
                    android.util.Log.i(TAG, "App is default launcher. Exiting if needed.")
                    CheckAndExitUtils.checkAndExitIfNeed(ctx)
                } else {
                    android.util.Log.i(TAG, "App is NOT default launcher. Scheduling next check.")
                    checkHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSetDefaultLauncherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTryNow.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                checkHandler.post(checkRunnable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var closeClickCount = 0
        binding.ivClose.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                closeClickCount++
                if (closeClickCount >= 2) {
                    dismiss()
                }
            }
            true
        }

        view.postDelayed({
            if (_binding != null) {
                binding.ivClose.visibility = View.VISIBLE
            }
        }, 3000)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkHandler.removeCallbacksAndMessages(null)
        _binding = null
    }

    companion object {
        const val TAG = "SetDefaultLauncherDialog"

        fun newInstance(): SetDefaultLauncherDialog {
            return SetDefaultLauncherDialog()
        }
    }
}
