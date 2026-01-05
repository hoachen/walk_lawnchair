package com.ur.apps.walk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sg.UserManager
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.utils.URLog
import com.ur.apps.walk.MainActivityRecycler.Companion.ON_CLAIM_GOAL
import com.ur.apps.walk.MainActivityRecycler.Companion.ON_CLAIM_TID
import com.ur.apps.walk.constants.StatisticConstants
import com.ur.apps.walk.databinding.ActivityPermissionRequestBinding
import org.json.JSONObject

private const val TAG = "PermissionRequestActivity"

class PermissionRequestActivity : BaseRewardActivity() {

    private lateinit var binding: ActivityPermissionRequestBinding
    private val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 1001
    private val REQUEST_CODE_STORAGE_PERMISSION = 1002
    private val REQUEST_CODE_ACTIVITY_RECOGNITION = 1003
    private val REQUEST_CODE_NOTIFICATION = 1004
    private val REQUEST_CODE_BATTERY_OPTIMIZATION = 1005

    // 权限申请步骤 - 配置必须权限和可选权限
    private var currentStep = 0
    private val steps = listOf(
        PermissionStep.SYSTEM_ALERT_WINDOW,   // 悬浮窗
        PermissionStep.NOTIFICATION,          // 通知显示
        PermissionStep.ACTIVITY_RECOGNITION,  // 步数检测
        PermissionStep.BATTERY_OPTIMIZATION,  // 电池优化
        PermissionStep.STORAGE                // 存储
    )

    // 必须权限列表 - 这些权限不允许跳过
    private val requiredPermissions = setOf(
        PermissionStep.ACTIVITY_RECOGNITION,
        // PermissionStep.NOTIFICATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        URLog.d(TAG, "onCreate: PermissionRequestActivity created")
        binding = ActivityPermissionRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        startPermissionFlow()
        UserManager.instance.refreshUserInfo()
    }

    private fun setupViews() {
        // 设置同意按钮点击事件
        binding.btnGrantPermission.setOnClickListener {
            handleGrantPermission()
        }

        // 设置跳过按钮点击事件
        binding.btnSkipPermission.setOnClickListener {
            handleSkipPermission()
        }
    }

    private fun startPermissionFlow() {
        URLog.d(TAG, "startPermissionFlow: currentStep=$currentStep, totalSteps=${steps.size}")
        if (currentStep < steps.size) {
            URLog.d(
                TAG,
                "startPermissionFlow: Starting step ${currentStep + 1} - ${steps[currentStep]}"
            )

            // 先检查当前步骤的权限是否已经获取
            if (isCurrentPermissionGranted()) {
                URLog.d(
                    TAG,
                    "startPermissionFlow: Permission already granted for step ${currentStep + 1}, moving to next step"
                )
                onCurrentPermissionGranted()
            } else {
                URLog.d(
                    TAG,
                    "startPermissionFlow: Permission not granted for step ${currentStep + 1}, waiting for user action"
                )
                updateUIForCurrentStep()
                // 不再自动请求权限，等待用户点击按钮
                URLog.d(
                    TAG,
                    "startPermissionFlow: Waiting for user action for step ${currentStep + 1}"
                )
            }
        } else {
            URLog.i(
                TAG,
                "startPermissionFlow: All permission steps completed, navigating to MainActivity"
            )
            navigateToMainActivity()
        }
    }

    private fun updateUIForCurrentStep() {
        URLog.d(
            TAG,
            "updateUIForCurrentStep: Updating UI for step ${currentStep + 1} - ${steps[currentStep]}"
        )

        // 根据当前步骤是否为必须权限来更新跳过按钮的可见性
        val isRequiredPermission = steps[currentStep] in requiredPermissions
        binding.btnSkipPermission.visibility = if (isRequiredPermission) View.GONE else View.VISIBLE
        binding.coinGrantPermission.visibility = View.INVISIBLE
        binding.tvCoinGrantPermission.visibility = View.INVISIBLE
        when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> {
                URLog.d(
                    TAG,
                    "updateUIForCurrentStep: Setting up ACTIVITY_RECOGNITION permission UI"
                )
                binding.tvPermissionTitle.text =
                    getString(R.string.permission_request_title_activity_recognition)
                binding.tvPermissionDescription.text = getString(
                    R.string.permission_request_description_activity_recognition,
                    getString(R.string.app_name)
                )
                binding.ivPermissionIcon.setImageResource(R.drawable.ic_lock_screen)
            }

            PermissionStep.NOTIFICATION -> {
                URLog.d(TAG, "updateUIForCurrentStep: Setting up NOTIFICATION permission UI")
                binding.tvPermissionTitle.text =
                    getString(R.string.permission_request_title_notification)
                binding.tvPermissionDescription.text = getString(
                    R.string.permission_request_description_notification,
                    getString(R.string.app_name)
                )
                binding.ivPermissionIcon.setImageResource(R.drawable.ic_lock_screen)
            }

            PermissionStep.BATTERY_OPTIMIZATION -> {
                URLog.d(
                    TAG,
                    "updateUIForCurrentStep: Setting up BATTERY_OPTIMIZATION permission UI"
                )
                binding.tvPermissionTitle.text =
                    getString(R.string.permission_request_title_battery_optimization)
                binding.tvPermissionDescription.text = getString(
                    R.string.permission_request_description_battery_optimization,
                    getString(R.string.app_name)
                )
                binding.ivPermissionIcon.setImageResource(R.drawable.ic_lock_screen)
            }

            PermissionStep.SYSTEM_ALERT_WINDOW -> {
                URLog.d(TAG, "updateUIForCurrentStep: Setting up SYSTEM_ALERT_WINDOW permission UI")
                binding.tvPermissionTitle.text =
                    getString(R.string.permission_request_title_system_alert)
                binding.tvPermissionDescription.text = getString(
                    R.string.permission_request_description_system_alert,
                    getString(R.string.app_name)
                )
                binding.ivPermissionIcon.setImageResource(R.drawable.ic_lock_screen)
                binding.coinGrantPermission.visibility = View.VISIBLE
                binding.tvCoinGrantPermission.visibility = View.VISIBLE
            }

            PermissionStep.STORAGE -> {
                URLog.d(TAG, "updateUIForCurrentStep: Setting up STORAGE permission UI")
                binding.tvPermissionTitle.text =
                    getString(R.string.permission_request_title_storage)
                binding.tvPermissionDescription.text = getString(
                    R.string.permission_request_description_storage,
                    getString(R.string.app_name)
                )
                binding.ivPermissionIcon.setImageResource(R.drawable.ic_gallery)
            }
        }
        // preload reward video
        AdLoaderManager.loadRewardVideoAd(this)
        AdLoaderManager.loadInterstitialAd(this)
        URLog.d(TAG, "updateUIForCurrentStep: UI updated successfully")
    }

    private fun requestCurrentPermission() {
        URLog.d(
            TAG,
            "requestCurrentPermission: Requesting permission for step ${currentStep + 1} - ${steps[currentStep]}"
        )
        when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> {
                URLog.d(TAG, "requestCurrentPermission: Requesting ACTIVITY_RECOGNITION permission")
                requestActivityRecognitionPermission("current")
            }

            PermissionStep.NOTIFICATION -> {
                URLog.d(TAG, "requestCurrentPermission: Requesting NOTIFICATION permission")
                requestNotificationPermission("current")
            }

            PermissionStep.BATTERY_OPTIMIZATION -> {
                URLog.d(TAG, "requestCurrentPermission: Requesting BATTERY_OPTIMIZATION permission")
                requestBatteryOptimizationPermission("current")
            }

            PermissionStep.SYSTEM_ALERT_WINDOW -> {
                URLog.d(TAG, "requestCurrentPermission: Requesting SYSTEM_ALERT_WINDOW permission")
                requestSystemAlertWindowPermission("current")
            }

            PermissionStep.STORAGE -> {
                URLog.d(TAG, "requestCurrentPermission: Requesting STORAGE permission")
                requestStoragePermission("current")
            }
        }
    }

    private fun handleGrantPermission() {
        URLog.d(
            TAG,
            "handleGrantPermission: User clicked grant permission for step ${currentStep + 1} - ${steps[currentStep]}"
        )
        when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> {
                URLog.d(
                    TAG,
                    "handleGrantPermission: Proceeding with ACTIVITY_RECOGNITION permission request"
                )
                requestActivityRecognitionPermission("click")
            }

            PermissionStep.NOTIFICATION -> {
                URLog.d(
                    TAG,
                    "handleGrantPermission: Proceeding with NOTIFICATION permission request"
                )
                requestNotificationPermission("click")
            }

            PermissionStep.BATTERY_OPTIMIZATION -> {
                URLog.d(
                    TAG,
                    "handleGrantPermission: Proceeding with BATTERY_OPTIMIZATION permission request"
                )
                requestBatteryOptimizationPermission("click")
            }

            PermissionStep.SYSTEM_ALERT_WINDOW -> {
                URLog.d(
                    TAG,
                    "handleGrantPermission: Proceeding with SYSTEM_ALERT_WINDOW permission request"
                )
                requestSystemAlertWindowPermission("click")
            }

            PermissionStep.STORAGE -> {
                URLog.d(TAG, "handleGrantPermission: Proceeding with STORAGE permission request")
                requestStoragePermission("click")
            }
        }
    }

    private fun handleSkipPermission() {
        when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> {
                // 活动识别权限是必须权限，不允许跳过
                URLog.w(TAG, "User tried to skip required ACTIVITY_RECOGNITION permission")
                showRequiredPermissionDeniedDialog()
            }

            PermissionStep.NOTIFICATION -> {
                URLog.w(TAG, "User skipped notification permission")
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_notification,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }

            PermissionStep.BATTERY_OPTIMIZATION -> {
                URLog.w(TAG, "User skipped battery optimization permission")
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_battery_optimization,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }

            PermissionStep.SYSTEM_ALERT_WINDOW -> {
                URLog.w(TAG, "User skipped system alert window permission")
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_system_alert,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }

            PermissionStep.STORAGE -> {
                URLog.w(TAG, "User skipped storage permission")
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_storage,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }
        }
    }

    private fun requestSystemAlertWindowPermission(source: String) {
        URLog.d(TAG, "requestSystemAlertWindowPermission: Checking SYSTEM_ALERT_WINDOW permission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            URLog.d(
                TAG,
                "requestSystemAlertWindowPermission: Android version >= M, checking overlay permission"
            )
            if (!Settings.canDrawOverlays(this)) {
                URLog.d(
                    TAG,
                    "requestSystemAlertWindowPermission: No overlay permission, starting settings activity"
                )
                // 没有权限，跳转到设置页面
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.fromParts("package", packageName, null)
                )
                intent.data = Uri.parse("package:$packageName")
                URLog.d(
                    TAG,
                    "requestSystemAlertWindowPermission: Starting activity for result with REQUEST_CODE_SYSTEM_ALERT_WINDOW"
                )

                TDAnalyticsManager.reportTrackEvent(
                    StatisticConstants.SYSTEM_ALERT_PERMISSION_REQUEST,
                    JSONObject().apply {
                        JSONObject().apply {
                            put(StatisticConstants.SOURCE, source)
                        }
                    }
                )
                startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
            } else {
                URLog.d(TAG, "requestSystemAlertWindowPermission: Already has overlay permission")
                // 已经有权限
                onSystemAlertWindowGranted()
            }
        } else {
            URLog.d(
                TAG,
                "requestSystemAlertWindowPermission: Android version < M, default permission granted"
            )
            // Android 6.0 以下版本默认有权限
            onSystemAlertWindowGranted()
        }
    }

    private fun requestStoragePermission(source: String) {
        URLog.d(TAG, "requestStoragePermission: Checking STORAGE permission")

        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用新的照片和视频权限
            URLog.d(TAG, "requestStoragePermission: Android 13+, using READ_MEDIA_IMAGES")
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-12 使用 READ_EXTERNAL_STORAGE
            URLog.d(TAG, "requestStoragePermission: Android 6.0-12, using READ_EXTERNAL_STORAGE")
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 6.0 以下版本默认有权限
            URLog.d(
                TAG,
                "requestStoragePermission: Android version < M, default permission granted"
            )
            onStoragePermissionGranted()
            return
        }

        // 检查权限状态
        val ungrantedPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            URLog.d(
                TAG,
                "requestStoragePermission: No storage permission, requesting permissions: $ungrantedPermissions"
            )
            // 请求存储权限
            ActivityCompat.requestPermissions(
                this,
                ungrantedPermissions.toTypedArray(),
                REQUEST_CODE_STORAGE_PERMISSION
            )

            TDAnalyticsManager.reportTrackEvent(
                StatisticConstants.STORAGE_PERMISSION_REQUEST,
                JSONObject().apply {
                    JSONObject().apply {
                        put(StatisticConstants.SOURCE, source)
                    }
                }
            )
        } else {
            URLog.d(TAG, "requestStoragePermission: Already has storage permission")
            // 已经有权限
            onStoragePermissionGranted()
        }
    }

    private fun onSystemAlertWindowGranted() {
        URLog.i(TAG, "System alert window permission granted")
        Toast.makeText(this, R.string.permission_granted_message_system_alert, Toast.LENGTH_SHORT)
            .show()
        showRewardAd()
        moveToNextStep()
    }

    private fun onStoragePermissionGranted() {
        URLog.i(TAG, "Storage permission granted")
        Toast.makeText(this, R.string.permission_granted_message_storage, Toast.LENGTH_SHORT).show()
        moveToNextStep()
    }

    /**
     * 请求活动识别权限
     */
    private fun requestActivityRecognitionPermission(source: String) {
        URLog.d(
            TAG,
            "requestActivityRecognitionPermission: Checking ACTIVITY_RECOGNITION permission"
        )

        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 ACTIVITY_RECOGNITION
            URLog.d(
                TAG,
                "requestActivityRecognitionPermission: Android 10+, using ACTIVITY_RECOGNITION"
            )
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            // Android 6.0 以下版本默认有权限
            URLog.d(
                TAG,
                "requestActivityRecognitionPermission: Android version < M, default permission granted"
            )
            onActivityRecognitionGranted()
            return
        }

        // 检查权限状态
        val ungrantedPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            URLog.d(
                TAG,
                "requestActivityRecognitionPermission: No activity recognition permission, requesting permissions: $ungrantedPermissions"
            )

            TDAnalyticsManager.reportTrackEvent(
                StatisticConstants.ACTIVITY_PERMISSION_REQUEST,
                JSONObject().apply {
                    JSONObject().apply {
                        put(StatisticConstants.SOURCE, source)
                    }
                }
            )
            // 请求活动识别权限
            ActivityCompat.requestPermissions(
                this,
                ungrantedPermissions.toTypedArray(),
                REQUEST_CODE_ACTIVITY_RECOGNITION
            )
        } else {
            URLog.d(
                TAG,
                "requestActivityRecognitionPermission: Already has activity recognition permission"
            )
            // 已经有权限
            onActivityRecognitionGranted()
        }
    }

    /**
     * 请求通知权限
     */
    private fun requestNotificationPermission(source: String) {
        URLog.d(TAG, "requestNotificationPermission: Checking NOTIFICATION permission")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要通知权限
            URLog.d(
                TAG,
                "requestNotificationPermission: Android 13+, requesting POST_NOTIFICATIONS"
            )
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                URLog.d(
                    TAG,
                    "requestNotificationPermission: No notification permission, requesting POST_NOTIFICATIONS"
                )
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )

                TDAnalyticsManager.reportTrackEvent(
                    StatisticConstants.NOTIFICATION_PERMISSION_REQUEST,
                    JSONObject().apply {
                        JSONObject().apply {
                            put(StatisticConstants.SOURCE, source)
                        }
                    }
                )
            } else {
                URLog.d(TAG, "requestNotificationPermission: Already has notification permission")
                onNotificationGranted()
            }
        } else {
            // Android 13 以下版本默认有权限
            URLog.d(
                TAG,
                "requestNotificationPermission: Android version < 13, default permission granted"
            )
            onNotificationGranted()
        }
    }

    /**
     * 请求忽略电池优化权限
     */
    private fun requestBatteryOptimizationPermission(source: String) {
        URLog.d(
            TAG,
            "requestBatteryOptimizationPermission: Checking BATTERY_OPTIMIZATION permission"
        )

        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            URLog.d(
                TAG,
                "requestBatteryOptimizationPermission: No battery optimization permission, starting settings activity"
            )
            // 没有权限，跳转到设置页面
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            URLog.d(
                TAG,
                "requestBatteryOptimizationPermission: Starting activity for result with REQUEST_CODE_BATTERY_OPTIMIZATION"
            )

            TDAnalyticsManager.reportTrackEvent(
                StatisticConstants.BATTERY_PERMISSION_REQUEST,
                JSONObject().apply {
                    JSONObject().apply {
                        put(StatisticConstants.SOURCE, source)
                    }
                }
            )
            startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
        } else {
            URLog.d(
                TAG,
                "requestBatteryOptimizationPermission: Already has battery optimization permission"
            )
            // 已经有权限
            onBatteryOptimizationGranted()
        }
    }

    private fun onActivityRecognitionGranted() {
        URLog.i(TAG, "Activity recognition permission granted")
        Toast.makeText(
            this,
            getString(R.string.permission_granted_message_activity_recognition),
            Toast.LENGTH_SHORT
        ).show()
        moveToNextStep()
    }

    private fun onNotificationGranted() {
        URLog.i(TAG, "Notification permission granted")
        Toast.makeText(
            this,
            getString(R.string.permission_granted_message_notification),
            Toast.LENGTH_SHORT
        ).show()
        moveToNextStep()
    }

    private fun onBatteryOptimizationGranted() {
        URLog.i(TAG, "Battery optimization permission granted")
        Toast.makeText(
            this,
            getString(R.string.permission_granted_message_battery_optimization),
            Toast.LENGTH_SHORT
        ).show()
        moveToNextStep()
    }

    /**
     * 检查当前步骤的权限是否已经获取
     */
    private fun isCurrentPermissionGranted(): Boolean {
        URLog.d(
            TAG,
            "isCurrentPermissionGranted: Checking permission for step ${currentStep + 1} - ${steps[currentStep]}"
        )
        return when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> {
                val permissionsToCheck = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissionsToCheck.add(Manifest.permission.ACTIVITY_RECOGNITION)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permissionsToCheck.add(Manifest.permission.BODY_SENSORS)
                } else {
                    URLog.d(
                        TAG,
                        "isCurrentPermissionGranted: Android version < M, ACTIVITY_RECOGNITION permission default granted"
                    )
                    return true
                }

                val granted = permissionsToCheck.all { permission ->
                    ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                }
                URLog.d(
                    TAG,
                    "isCurrentPermissionGranted: ACTIVITY_RECOGNITION permission granted = $granted"
                )
                granted
            }

            PermissionStep.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    URLog.d(
                        TAG,
                        "isCurrentPermissionGranted: NOTIFICATION permission granted = $granted"
                    )
                    granted
                } else {
                    URLog.d(
                        TAG,
                        "isCurrentPermissionGranted: Android version < 13, NOTIFICATION permission default granted"
                    )
                    true
                }
            }

            PermissionStep.BATTERY_OPTIMIZATION -> {
                val powerManager = getSystemService(PowerManager::class.java)
                val granted = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
                URLog.d(
                    TAG,
                    "isCurrentPermissionGranted: BATTERY_OPTIMIZATION permission granted = $granted"
                )
                granted
            }

            PermissionStep.SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val granted = Settings.canDrawOverlays(this)
                    URLog.d(
                        TAG,
                        "isCurrentPermissionGranted: SYSTEM_ALERT_WINDOW permission granted = $granted"
                    )
                    granted
                } else {
                    URLog.d(
                        TAG,
                        "isCurrentPermissionGranted: Android version < M, SYSTEM_ALERT_WINDOW permission default granted"
                    )
                    true
                }
            }

            PermissionStep.STORAGE -> {
                val permissionsToCheck = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToCheck.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permissionsToCheck.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    URLog.d(
                        TAG,
                        "isCurrentPermissionGranted: Android version < M, STORAGE permission default granted"
                    )
                    return true
                }

                val granted = permissionsToCheck.all { permission ->
                    ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                }
                URLog.d(TAG, "isCurrentPermissionGranted: STORAGE permission granted = $granted")
                granted
            }
        }
    }

    /**
     * 当前步骤权限已获取时的处理
     */
    private fun onCurrentPermissionGranted() {
        URLog.d(
            TAG,
            "onCurrentPermissionGranted: Permission already granted for step ${currentStep + 1}, moving to next step"
        )
        // 静默移动到下一步，不显示Toast
        moveToNextStep()
    }

    private fun moveToNextStep() {
        URLog.d(
            TAG,
            "moveToNextStep: Moving from step ${currentStep + 1} to step ${currentStep + 2}"
        )
        currentStep++
        URLog.d(TAG, "moveToNextStep: Current step is now $currentStep")
        startPermissionFlow()
    }

    private fun navigateToMainActivity() {
        // 跳转到新的RecyclerView版本的MainActivity
        val taskId = intent.getIntExtra(ON_CLAIM_TID, -1)
        val goal = intent.getIntExtra(ON_CLAIM_GOAL, -1)
        val intent = Intent(this, MainActivityRecycler::class.java)
        if (taskId != -1 && goal != -1) {
            intent.putExtra(ON_CLAIM_TID, taskId)
            intent.putExtra(ON_CLAIM_GOAL, goal)
        }
        startActivity(intent)
        // 添加过渡动画
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        // 关闭当前页面，避免返回到此页面
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        URLog.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SYSTEM_ALERT_WINDOW -> {
                URLog.d(TAG, "onActivityResult: Handling SYSTEM_ALERT_WINDOW permission result")
                URLog.d(
                    TAG,
                    "onActivityResult: Checking overlay permission after settings return"
                )
                if (Settings.canDrawOverlays(this)) {
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.SYSTEM_ALERT_PERMISSION,
                        JSONObject().apply {
                            put(StatisticConstants.GRANTED, true)
                        }
                    )
                    URLog.d(TAG, "onActivityResult: Overlay permission granted by user")
                    onSystemAlertWindowGranted()
                } else {
                    // 用户拒绝了权限，显示解释性对话框
                    URLog.w(TAG, "onActivityResult: User denied system alert window permission")
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.SYSTEM_ALERT_PERMISSION,
                        JSONObject().apply {
                            JSONObject().apply {
                                put(StatisticConstants.GRANTED, false)
                            }
                        }
                    )
                    showSystemAlertWindowDeniedDialog()
                }
            }

            REQUEST_CODE_BATTERY_OPTIMIZATION -> {
                URLog.d(TAG, "onActivityResult: Handling BATTERY_OPTIMIZATION permission result")
                val powerManager = getSystemService(PowerManager::class.java)
                if (powerManager != null && powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    URLog.d(
                        TAG,
                        "onActivityResult: Battery optimization permission granted by user"
                    )
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.BATTERY_PERMISSION,
                        JSONObject().apply {
                            put(StatisticConstants.GRANTED, true)
                        }
                    )
                    onBatteryOptimizationGranted()
                } else {
                    // 用户没有授予电池优化权限
                    URLog.w(
                        TAG,
                        "onActivityResult: User did not grant battery optimization permission"
                    )
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.BATTERY_PERMISSION,
                        JSONObject().apply {
                            JSONObject().apply {
                                put(StatisticConstants.GRANTED, false)
                            }
                        }
                    )
                    // 保持当前步骤，等待用户再次尝试
                    URLog.d(
                        TAG,
                        "onActivityResult: Staying at current step for BATTERY_OPTIMIZATION"
                    )
                }
            }

            else -> {
                URLog.d(TAG, "onActivityResult: Unhandled requestCode: $requestCode")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        URLog.d(
            TAG,
            "onRequestPermissionsResult: requestCode=$requestCode, permissions=${permissions.contentToString()}, grantResults=${grantResults.contentToString()}"
        )
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSION -> {
                URLog.d(TAG, "onRequestPermissionsResult: Handling STORAGE permission result")
                // 检查是否至少有一个权限被授予
                val granted =
                    grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    URLog.d(TAG, "onRequestPermissionsResult: Storage permission granted by user")
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.STORAGE_PERMISSION,
                        JSONObject().apply {
                            put(StatisticConstants.GRANTED, true)
                        }
                    )
                    onStoragePermissionGranted()
                } else {
                    // 用户拒绝了权限
                    URLog.w(TAG, "onRequestPermissionsResult: User denied storage permission")

                    // 检查是否应该显示请求权限的理由
                    val shouldShowRationale = permissions.any { permission ->
                        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                    }

                    if (shouldShowRationale) {
                        // 用户暂时拒绝，显示解释性对话框，让用户重新尝试
                        TDAnalyticsManager.reportTrackEvent(
                            StatisticConstants.STORAGE_PERMISSION,
                            JSONObject().apply {
                                JSONObject().apply {
                                    put(StatisticConstants.GRANTED, false)
                                    put(StatisticConstants.TIMES, 1)
                                }
                            }
                        )
                        URLog.d(
                            TAG,
                            "onRequestPermissionsResult: User temporarily denied, showing rationale dialog"
                        )
                        showStoragePermissionDeniedDialog()
                    } else {
                        // 用户永久拒绝（选择了"不再询问"），引导用户去设置
                        URLog.d(
                            TAG,
                            "onRequestPermissionsResult: User permanently denied, showing settings dialog"
                        )
                        TDAnalyticsManager.reportTrackEvent(
                            StatisticConstants.STORAGE_PERMISSION,
                            JSONObject().apply {
                                JSONObject().apply {
                                    put(StatisticConstants.GRANTED, false)
                                    put(StatisticConstants.TIMES, 2)
                                }
                            }
                        )
                        showStoragePermissionPermanentlyDeniedDialog()
                    }
                }
            }

            REQUEST_CODE_ACTIVITY_RECOGNITION -> {
                URLog.d(
                    TAG,
                    "onRequestPermissionsResult: Handling ACTIVITY_RECOGNITION permission result"
                )
                // 检查是否至少有一个权限被授予
                val granted =
                    grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    URLog.d(
                        TAG,
                        "onRequestPermissionsResult: Activity recognition permission granted by user"
                    )
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.ACTIVITY_PERMISSION,
                        JSONObject().apply {
                            put(StatisticConstants.GRANTED, true)
                        }
                    )
                    onActivityRecognitionGranted()
                } else {
                    // 用户拒绝了权限
                    URLog.w(
                        TAG,
                        "onRequestPermissionsResult: User denied activity recognition permission"
                    )

                    // 检查是否应该显示请求权限的理由
                    val shouldShowRationale = permissions.any { permission ->
                        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                    }

                    if (shouldShowRationale) {
                        // 用户暂时拒绝，显示解释性对话框，让用户重新尝试
                        URLog.d(
                            TAG,
                            "onRequestPermissionsResult: User temporarily denied, showing rationale dialog"
                        )
                        TDAnalyticsManager.reportTrackEvent(
                            StatisticConstants.ACTIVITY_PERMISSION,
                            JSONObject().apply {
                                JSONObject().apply {
                                    put(StatisticConstants.GRANTED, false)
                                    put(StatisticConstants.TIMES, 1)
                                }
                            }
                        )
                        showRequiredPermissionDeniedDialog()
                    } else {
                        // 用户永久拒绝（选择了"不再询问"），引导用户去设置
                        URLog.d(
                            TAG,
                            "onRequestPermissionsResult: User permanently denied, showing settings dialog"
                        )
                        TDAnalyticsManager.reportTrackEvent(
                            StatisticConstants.ACTIVITY_PERMISSION,
                            JSONObject().apply {
                                JSONObject().apply {
                                    put(StatisticConstants.GRANTED, false)
                                    put(StatisticConstants.TIMES, 2)
                                }
                            }
                        )
                        showRequiredPermissionPermanentlyDeniedDialog()
                    }
                }
            }

            REQUEST_CODE_NOTIFICATION -> {
                URLog.d(TAG, "onRequestPermissionsResult: Handling NOTIFICATION permission result")
                // 检查是否至少有一个权限被授予
                val granted =
                    grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    URLog.d(
                        TAG,
                        "onRequestPermissionsResult: Notification permission granted by user"
                    )
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.NOTIFICATION_PERMISSION,
                        JSONObject().apply {
                            put(StatisticConstants.GRANTED, true)
                        }
                    )
                    onNotificationGranted()
                } else {
                    // 用户拒绝了权限
                    URLog.w(TAG, "onRequestPermissionsResult: User denied notification permission")

                    // 检查是否应该显示请求权限的理由
                    val shouldShowRationale = permissions.any { permission ->
                        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                    }

                    if (shouldShowRationale) {
                        // 用户暂时拒绝，显示解释性对话框，让用户重新尝试
                        URLog.d(
                            TAG,
                            "onRequestPermissionsResult: User temporarily denied, showing rationale dialog"
                        )
                        TDAnalyticsManager.reportTrackEvent(
                            StatisticConstants.NOTIFICATION_PERMISSION,
                            JSONObject().apply {
                                JSONObject().apply {
                                    put(StatisticConstants.GRANTED, false)
                                    put(StatisticConstants.TIMES, 1)
                                }
                            }
                        )
                        showRequiredPermissionDeniedDialog()
                    } else {
                        // 用户永久拒绝（选择了"不再询问"），引导用户去设置
                        URLog.d(
                            TAG,
                            "onRequestPermissionsResult: User permanently denied, showing settings dialog"
                        )
                        TDAnalyticsManager.reportTrackEvent(
                            StatisticConstants.NOTIFICATION_PERMISSION,
                            JSONObject().apply {
                                JSONObject().apply {
                                    put(StatisticConstants.GRANTED, false)
                                    put(StatisticConstants.TIMES, 2)
                                }
                            }
                        )
                        showRequiredPermissionPermanentlyDeniedDialog()
                    }
                }
            }

            else -> {
                URLog.d(TAG, "onRequestPermissionsResult: Unhandled requestCode: $requestCode")
            }
        }
    }

    /**
     * 显示必须权限被拒绝的对话框
     */
    private fun showRequiredPermissionDeniedDialog() {
        val permissionName = when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> getString(R.string.permission_name_activity_recognition)
            PermissionStep.NOTIFICATION -> getString(R.string.permission_name_notification)
            PermissionStep.BATTERY_OPTIMIZATION -> getString(R.string.permission_name_battery_optimization)
            else -> getString(R.string.permission_name_required)
        }

        // 创建一个对话框，解释为什么需要这个必须权限，并要求用户重新请求
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.required_permission_denied_title_format, permissionName))
            .setMessage(
                getString(
                    R.string.required_permission_denied_message_format,
                    permissionName
                )
            )
            .setPositiveButton(getString(R.string.required_permission_retry)) { dialog, which ->
                URLog.d(
                    TAG,
                    "showRequiredPermissionDeniedDialog: User wants to retry required permission"
                )
                // 重新请求当前权限
                requestCurrentPermission()
            }
            .setNegativeButton(getString(R.string.required_permission_exit_app)) { dialog, which ->
                URLog.w(TAG, "showRequiredPermissionDeniedDialog: User chose to exit app")
                // 退出应用
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 显示必须权限被永久拒绝的对话框
     */
    private fun showRequiredPermissionPermanentlyDeniedDialog() {
        val permissionName = when (steps[currentStep]) {
            PermissionStep.ACTIVITY_RECOGNITION -> getString(R.string.permission_name_activity_recognition)
            PermissionStep.NOTIFICATION -> getString(R.string.permission_name_notification)
            PermissionStep.BATTERY_OPTIMIZATION -> getString(R.string.permission_name_battery_optimization)
            else -> getString(R.string.permission_name_required)
        }

        // 创建一个对话框，解释权限被永久拒绝，引导用户去设置
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.required_permission_permanently_denied_title_format,
                    permissionName
                )
            )
            .setMessage(
                getString(
                    R.string.required_permission_permanently_denied_message_format,
                    permissionName
                )
            )
            .setPositiveButton(getString(R.string.required_permission_go_to_settings)) { dialog, which ->
                URLog.d(
                    TAG,
                    "showRequiredPermissionPermanentlyDeniedDialog: User wants to go to settings"
                )
                // 跳转到应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                // 用户从设置返回后，重新检查权限
                // 这里不移动下一步，等待用户返回后重新检查
            }
            .setNegativeButton(getString(R.string.required_permission_exit_app)) { dialog, which ->
                URLog.w(
                    TAG,
                    "showRequiredPermissionPermanentlyDeniedDialog: User chose to exit app"
                )
                // 退出应用
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        URLog.d(TAG, "onResume: Activity resumed, checking if we need to verify permission status")
        // 当从设置页面返回时，检查当前步骤的权限状态
        if (currentStep < steps.size) {
            URLog.d(TAG, "onResume: Checking permission status for current step ${currentStep + 1}")
            if (isCurrentPermissionGranted()) {
                URLog.d(TAG, "onResume: Permission granted while in settings, moving to next step")
                onCurrentPermissionGranted()
            } else {
                URLog.d(TAG, "onResume: Permission still not granted, staying at current step")
            }
        }
    }

    private fun showStoragePermissionDeniedDialog() {
        // 创建一个对话框，解释为什么需要存储权限，并询问用户是否要重新请求
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.permission_denied_dialog_title_storage)
            .setMessage(R.string.permission_denied_dialog_message_storage)
            .setPositiveButton(R.string.permission_denied_dialog_retry) { dialog, which ->
                URLog.d(
                    TAG,
                    "showStoragePermissionDeniedDialog: User wants to retry storage permission"
                )
                // 重新请求存储权限
                requestStoragePermission("click")
            }
            .setNegativeButton(R.string.permission_denied_dialog_skip) { dialog, which ->
                URLog.w(
                    TAG,
                    "showStoragePermissionDeniedDialog: User chose to skip storage permission"
                )
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_storage,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSystemAlertWindowDeniedDialog() {
        // 创建一个对话框，解释为什么需要锁屏权限，并询问用户是否要重新尝试
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.permission_denied_dialog_title_system_alert)
            .setMessage(R.string.permission_denied_dialog_message_system_alert)
            .setPositiveButton(R.string.permission_denied_dialog_retry) { dialog, which ->
                URLog.d(
                    TAG,
                    "showSystemAlertWindowDeniedDialog: User wants to retry system alert window permission"
                )
                // 重新请求锁屏权限
                requestSystemAlertWindowPermission("dialog")
            }
            .setNegativeButton(R.string.permission_denied_dialog_skip) { dialog, which ->
                URLog.w(
                    TAG,
                    "showSystemAlertWindowDeniedDialog: User chose to skip system alert window permission"
                )
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_system_alert,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }
            .setCancelable(false)
            .show()
    }

    private fun showStoragePermissionPermanentlyDeniedDialog() {
        // 创建一个对话框，解释权限被永久拒绝，引导用户去设置
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.permission_permanently_denied_dialog_title_storage)
            .setMessage(R.string.permission_permanently_denied_dialog_message_storage)
            .setPositiveButton(R.string.permission_permanently_denied_dialog_settings) { dialog, which ->
                URLog.d(
                    TAG,
                    "showStoragePermissionPermanentlyDeniedDialog: User wants to go to settings"
                )
                // 跳转到应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                // 用户从设置返回后，重新检查权限
                // 这里不移动下一步，等待用户返回后重新检查
            }
            .setNegativeButton(R.string.permission_permanently_denied_dialog_skip) { dialog, which ->
                URLog.w(
                    TAG,
                    "showStoragePermissionPermanentlyDeniedDialog: User chose to skip storage permission"
                )
                Toast.makeText(
                    this,
                    R.string.permission_skipped_message_storage,
                    Toast.LENGTH_SHORT
                ).show()
                moveToNextStep()
            }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // 禁用返回键，强制用户选择同意或跳过
        Toast.makeText(this, R.string.permission_back_press_message, Toast.LENGTH_SHORT).show()
    }

    sealed class PermissionStep {
        object ACTIVITY_RECOGNITION : PermissionStep()
        object NOTIFICATION : PermissionStep()
        object BATTERY_OPTIMIZATION : PermissionStep()
        object SYSTEM_ALERT_WINDOW : PermissionStep()
        object STORAGE : PermissionStep()
    }
}
