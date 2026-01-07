package com.ur.apps.walk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import com.ur.apps.utils.URLog
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.android.launcher3.databinding.ActivityProfileBinding
import com.ur.apps.walk.step.bean.ExerciseStats
import com.ur.apps.walk.step.callback.StepCountChangeCallBack
import com.ur.apps.walk.step.manager.StepManager
import com.ur.apps.walk.step.utils.SharedPreferencesUtils
import com.ur.apps.walk.ui.ArcProgressBar
import com.ur.apps.walk.utils.DialogUtils
import kotlin.math.min

private const val AVATAR_END_FIX = "avatar_end_fix"
private const val AVATAR_END_FIX_KEY = "avatar_end_fix_key"

class ProfileActivity : BaseActivity(), StepCountChangeCallBack {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var stepManager: StepManager
    private lateinit var sharedPreferences: SharedPreferencesUtils

    // 图片选择结果处理
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                saveAndDisplayAvatar(it)
            } ?: run {
                showError(getString(R.string.avatar_pick_failed))
            }
        }

    // 权限请求结果处理
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                showError(getString(R.string.avatar_permission_required))
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == getString(R.string.step_permission_code).toInt()) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 用户授予权限，重启计步器
                stepManager.restartStepDetector()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = SharedPreferencesUtils(this)
        setupToolbar()
        initStepManager()
        updateStepStats()
        setupGenderSelection()
        setupHeightSelection()
        setupWeightSelection()
        setupAvatarSelection()
        setupNicknameSelection()
        loadAvatar()
        loadNickname()
    }

    private fun setupToolbar() {
        binding.layoutToolbar.apply {
            btnSettings.setOnClickListener {
                startActivity(Intent(this@ProfileActivity, SettingsActivity::class.java))
            }
            btnLeaderboard.setOnClickListener {
                URLog.d("ProfileActivity", "Leaderboard button clicked")
                startActivity(Intent(this@ProfileActivity, LeaderboardActivity::class.java))
            }
            btnHome.setOnClickListener {
                finish()
            }
        }
    }

    private fun initStepManager() {
        stepManager = StepManager.getInstance(this)
        stepManager.registerCallback(this)
        if (!stepManager.checkActivityRecognitionPermission()) {
            stepManager.requestActivityRecognitionPermission(this)
        }
    }

    private fun updateStepStats() {

        // 获取7天平均步数
        val weekAvgSteps = stepManager.weekAverageSteps.toInt()

        // 获取30天平均步数
        val monthAvgSteps = stepManager.monthAverageSteps.toInt()

        // 获取最佳记录
        val bestStepData = stepManager.getMaxStepsDay()
        val bestSteps = bestStepData?.step?.toIntOrNull() ?: 0

        // 计算进度百分比（最大10000步）
        val maxStepsForProgress = 10000
        val weekProgress = min(100, (weekAvgSteps * 100) / maxStepsForProgress)
        val monthProgress = min(100, (monthAvgSteps * 100) / maxStepsForProgress)
        val bestProgress = min(100, (bestSteps * 100) / maxStepsForProgress)

        // 获取视图引用
        val stats7daysView = binding.layoutStepStats.stats7days.root
        val stats30daysView = binding.layoutStepStats.stats30days.root
        val statsBestView = binding.layoutStepStats.statsBest.root

        // 更新7天平均步数
        stats7daysView.findViewById<TextView>(R.id.tv_steps).text = weekAvgSteps.toString()
        stats7daysView.findViewById<TextView>(R.id.tv_label).text =
            getString(R.string.seven_days_avg)
        stats7daysView.findViewById<ArcProgressBar>(R.id.progress_circular)
            .setProgress(weekProgress)

        // 更新30天平均步数
        stats30daysView.findViewById<TextView>(R.id.tv_steps).text = monthAvgSteps.toString()
        stats30daysView.findViewById<TextView>(R.id.tv_label).text =
            getString(R.string.thirty_days_avg)
        stats30daysView.findViewById<ArcProgressBar>(R.id.progress_circular)
            .setProgress(monthProgress)

        // 更新最佳记录
        statsBestView.findViewById<TextView>(R.id.tv_steps).text = bestSteps.toString()
        statsBestView.findViewById<TextView>(R.id.tv_label).text = getString(R.string.best_record)
        statsBestView.findViewById<ArcProgressBar>(R.id.progress_circular).setProgress(bestProgress)

        // 设置点击事件，跳转到趋势页面
        stats7daysView.setOnClickListener {
            val intent = Intent(this, TrendsActivity::class.java).apply {
                putExtra(TrendsActivity.EXTRA_INITIAL_TAB, TrendsActivity.TAB_WEEKLY)
            }
            startActivity(intent)
        }

        stats30daysView.setOnClickListener {
            val intent = Intent(this, TrendsActivity::class.java).apply {
                putExtra(TrendsActivity.EXTRA_INITIAL_TAB, TrendsActivity.TAB_MONTHLY)
            }
            startActivity(intent)
        }

        statsBestView.setOnClickListener {
            val intent = Intent(this, TrendsActivity::class.java).apply {
                putExtra(TrendsActivity.EXTRA_INITIAL_TAB, TrendsActivity.TAB_DAILY)
            }
            startActivity(intent)
        }
    }

    private fun setupGenderSelection() {
        binding.layoutUserInfo.ivGender.setOnClickListener {
            showGenderSelectionDialog()
        }
    }

    private fun showGenderSelectionDialog() {
        val currentGender =
            sharedPreferences.getParam(getString(R.string.pref_key_gender), 0) as Int

        DialogUtils.showGenderDialog(
            context = this,
            currentGender = currentGender,
            onSave = { gender ->
                sharedPreferences.setParam(getString(R.string.pref_key_gender), gender)
                loadAvatar()
            }
        )
    }

    private fun setupHeightSelection() {
        binding.layoutUserInfo.ivHeight.setOnClickListener {
            showHeightSelectionDialog()
        }

        // 设置当前身高图标
        updateHeightIcon()
    }

    private fun showHeightSelectionDialog() {
        val isCm =
            sharedPreferences.getParam(getString(R.string.pref_key_height_unit_cm), true) as Boolean
        val currentHeight =
            sharedPreferences.getParam(getString(R.string.pref_key_height), 170) as Int

        DialogUtils.showHeightDialog(
            context = this,
            currentHeight = currentHeight,
            isCm = isCm,
            onSave = { height, newIsCm ->
                sharedPreferences.setParam(getString(R.string.pref_key_height), height)
                sharedPreferences.setParam(getString(R.string.pref_key_height_unit_cm), newIsCm)
                updateHeightIcon()
            }
        )
    }

    private fun updateHeightIcon() {
        val isCm =
            sharedPreferences.getParam(getString(R.string.pref_key_height_unit_cm), true) as Boolean
        val height = sharedPreferences.getParam(getString(R.string.pref_key_height), 170) as Int

        // 更新身高图标
        binding.layoutUserInfo.ivHeight.setImageResource(R.drawable.profile_height)

        // 设置长按提示
        val heightText = if (isCm) {
            getString(R.string.height_format_cm, height)
        } else {
            val cmToFtRate = getString(R.string.height_cm_to_ft_rate).toFloat()
            val cmToInchRate = getString(R.string.height_cm_to_inch_rate).toFloat()
            val feet = height / cmToFtRate // 转换为英尺
            val inches = ((height % cmToFtRate) / cmToInchRate).toInt() // 转换为英寸
            getString(R.string.height_format_ft, feet.toInt(), inches)
        }
        binding.layoutUserInfo.ivHeight.contentDescription = heightText
    }

    private fun setupWeightSelection() {
        binding.layoutUserInfo.ivWeight.setOnClickListener {
            showWeightSelectionDialog()
        }

        // 设置当前体重图标
        updateWeightIcon()
    }

    private fun showWeightSelectionDialog() {
        val isKg =
            sharedPreferences.getParam(getString(R.string.pref_key_weight_unit_kg), true) as Boolean
        val currentWeight =
            sharedPreferences.getParam(getString(R.string.pref_key_weight), 60.0f) as Float

        DialogUtils.showWeightDialog(
            context = this,
            currentWeight = currentWeight,
            isKg = isKg,
            onSave = { weight, newIsKg ->
                sharedPreferences.setParam(getString(R.string.pref_key_weight), weight)
                sharedPreferences.setParam(getString(R.string.pref_key_weight_unit_kg), newIsKg)
                updateWeightIcon()
            }
        )
    }

    private fun updateWeightIcon() {
        val isKg =
            sharedPreferences.getParam(getString(R.string.pref_key_weight_unit_kg), true) as Boolean
        val weight = sharedPreferences.getParam(getString(R.string.pref_key_weight), 60.0f) as Float

        // 更新体重图标
        binding.layoutUserInfo.ivWeight.setImageResource(R.drawable.profile_kg)

        // 设置长按提示
        val weightText = if (isKg) {
            getString(R.string.weight_format_kg, weight)
        } else {
            val kgToLbsRate = getString(R.string.weight_kg_to_lbs_rate).toFloat()
            val weightLbs = weight * kgToLbsRate
            getString(R.string.weight_format_lbs, weightLbs)
        }
        binding.layoutUserInfo.ivWeight.contentDescription = weightText
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.error))
            .setMessage(message)
            .setPositiveButton(getString(R.string.confirm), null)
            .show()
    }

    override fun onStepChange(stepCount: ExerciseStats?) {
        updateStepStats()
    }

    override fun onStepReachPeriod(hundred_level: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setupAvatarSelection() {
        binding.layoutUserInfo.ivAvatar.setOnClickListener {
            checkGalleryPermissionAndProceed()
        }
    }

    private fun checkGalleryPermissionAndProceed() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }

            else -> {
                requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    private fun openGallery() {
        pickImage.launch("image/*")
    }

    private fun saveAndDisplayAvatar(uri: Uri) {
        // 保存图片URI
        sharedPreferences.setParam(getString(R.string.pref_key_avatar_uri), uri.toString())

        // 使用Glide加载并显示图片
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .placeholder(R.drawable.ic_avatar_placeholder) // 添加占位图
            .error(R.drawable.ic_avatar_placeholder) // 添加错误图
            .into(binding.layoutUserInfo.ivAvatar)
    }

    private fun loadAvatar() {
        // 加载保存的头像
        val avatarUri =
            sharedPreferences.getParam(getString(R.string.pref_key_avatar_uri), "") as String
        val prefs = this.getSharedPreferences(AVATAR_END_FIX, Context.MODE_PRIVATE)
        var avatar = prefs.getString(AVATAR_END_FIX_KEY, "")
        val currentGender =
            sharedPreferences.getParam(getString(R.string.pref_key_gender), 0) as Int
        val hair = if (currentGender == 0) {
            "short01"
        } else {
            "long01"
        }
        if (avatar.isNullOrEmpty()) {
            //            // 设置默认头像
//            binding.layoutUserInfo.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder)

            avatar = System.currentTimeMillis().toString()
            prefs.edit().putString(AVATAR_END_FIX_KEY, avatar).apply()
            Glide.with(this)
                .load(Uri.parse("https://api.dicebear.com/9.x/adventurer/svg?seed=$avatar?hair=$hair"))
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.layoutUserInfo.ivAvatar)
        } else {
            Glide.with(this)
                .load(Uri.parse("https://api.dicebear.com/7.x/avataaars/png?seed={$avatar}?hair=$hair"))
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.layoutUserInfo.ivAvatar)
        }
    }

    private fun setupNicknameSelection() {
        binding.layoutUserInfo.tvNickname.setOnClickListener {
            showNicknameDialog()
        }
    }

    private fun showNicknameDialog() {
        val currentNickname =
            sharedPreferences.getParam(getString(R.string.pref_key_nickname), "") as String

        DialogUtils.showNicknameDialog(
            context = this,
            currentNickname = currentNickname,
            onSave = { nickname ->
                when {
                    nickname.isEmpty() -> {
                        showError(getString(R.string.nickname_empty_error))
                    }

                    nickname.length > 12 -> {
                        showError(getString(R.string.nickname_too_long_error))
                    }

                    else -> {
                        sharedPreferences.setParam(getString(R.string.pref_key_nickname), nickname)
                        updateNickname(nickname)
                    }
                }
            }
        )
    }

    private fun updateNickname(nickname: String) {
        binding.layoutUserInfo.tvNickname.text = nickname
    }

    private fun loadNickname() {
        val nickname =
            sharedPreferences.getParam(getString(R.string.pref_key_nickname), "") as String
        if (nickname.isNotEmpty()) {
            updateNickname(nickname)
        } else {
            binding.layoutUserInfo.tvNickname.setText(R.string.nickname_default)
        }
    }
}
