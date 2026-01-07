package com.ur.apps.walk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ur.apps.ad.admob.AdmobAdLoader
import com.ur.apps.utils.URLog
import com.ur.apps.walk.adapters.SettingsAdapter
import com.android.launcher3.databinding.ActivitySettingsBinding
import com.ur.apps.walk.settings.SettingsManager
import com.ur.apps.walk.settings.model.SettingItem
import com.ur.apps.walk.utils.DialogUtils
import com.ur.apps.walk.utils.LocaleHelper
import com.ur.apps.walk.utils.LogUtils
import com.ur.apps.walk.utils.RegionHelper
import com.ur.apps.walk.utils.ThemeManager
import com.ur.apps.walk.utils.ThemeManager.BrandTheme
import kotlinx.coroutines.launch

private const val TAG = "SettingsActivity"

class SettingsActivity : BaseActivity(), SettingsAdapter.Listeners {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager
    private lateinit var adapter: SettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initSettings()
    }

    private fun setupToolbar() {
        binding.layoutToolbar.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initSettings() {
        settingsManager = SettingsManager(this)

        // 创建适配器，使用this作为监听器
        adapter = SettingsAdapter(
            this,
            settingsManager.getSettingItems(),
            this
        )

        binding.rvSettings.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
        }
    }

    // 显示语言选择对话框
    private fun showLanguageSelectionDialog() {
        val languages = LocaleHelper.getAvailableLanguages(this)
        val currentLanguage = LocaleHelper.getLanguage(this)

        DialogUtils.showLanguageSelectionDialog(
            context = this,
            currentLanguage = currentLanguage,
            languages = languages,
            onLanguageSelected = { languageCode ->
                if (languageCode != currentLanguage) {
                    // 保存语言设置
                    LocaleHelper.setLanguage(this, languageCode)
                    // 重启应用到首页
                    LocaleHelper.restartApp(this)
                }
            }
        )
    }

    // 显示区域选择对话框
    private fun showRegionSelectionDialog() {
        val regions = RegionHelper.getAvailableRegions(this)
        val currentRegion = RegionHelper.getRegion(this)

        DialogUtils.showRegionSelectionDialog(
            context = this,
            currentRegion = currentRegion,
            regions = regions,
            onRegionSelected = { regionCode ->
                if (regionCode != currentRegion) {
                    // 保存区域设置
                    lifecycleScope.launch {
                        if (RegionHelper.setRegion(this@SettingsActivity, regionCode)) {
                            // 刷新设置项
                            refreshSettings()
                        } else {
                            Toast.makeText(
                                this@SettingsActivity,
                                getString(R.string.update_region_error_toast),
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
            }
        )
    }

    // 刷新设置界面
    private fun refreshSettings() {
        // 创建新的适配器
        adapter = SettingsAdapter(
            this,
            settingsManager.getSettingItems(),
            this
        )
        binding.rvSettings.adapter = adapter
    }

    // 重启Activity以应用语言变更
    private fun restartActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
        // 添加过渡动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // SettingsAdapter.Listeners 接口实现
    override fun onSwitchChanged(id: String, isChecked: Boolean) {
        settingsManager.updateSetting(id, isChecked)
    }

    // 版本号点击计数器
    private var versionClickCount = 0
    private val versionClickThreshold = 15
    private val versionClickAdmobIdThreshold = 6

    override fun onValueItemClicked(id: String) {
        when (id) {
            getString(R.string.settings_key_language) -> showLanguageSelectionDialog()
            getString(R.string.settings_key_region) -> showRegionSelectionDialog()
            getString(R.string.settings_key_version) -> {
                versionClickCount++
                if (versionClickCount >= versionClickAdmobIdThreshold) {
                    AdmobAdLoader.useDebugAdmobId = true
                    LockAdManager.instance.setDebugMode()
                    Toast.makeText(this, "开启Debug 模式",
                        Toast.LENGTH_SHORT).show()
                    URLog.i(TAG, "user Debug Admob native unit id")
                }
                if (versionClickCount >= versionClickThreshold) {
                    versionClickCount = 0
                    triggerLogCollection(context = this)
                }
            }

            // 可以处理其他值项点击
        }
    }

    override fun onThemeSelected(theme: BrandTheme) {
        // 切换主题
        ThemeManager.getInstance().changeTheme(this, theme)
    }

    override fun onDistanceUnitChanged(id: String, isKm: Boolean) {
        settingsManager.updateSetting(id, isKm)
    }

    override fun onActionItemClicked(id: SettingItem.Item.Action) {
        // 处理动作项点击
        URLog.d(TAG, "点击了动作项: $id")
        id.onClick.invoke()
    }

    /**
     * 触发日志收集和分享
     */
    private fun triggerLogCollection(context: Context) {
        try {
            URLog.i("SettingsManager", "triggerLogCollection: Starting log collection and sharing")
            val appName = context.getString(R.string.app_name)
            val versionName = getAppVersionName(context)
            val triggerTime =
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())

            URLog.d(
                "SettingsManager",
                "triggerLogCollection: appName=$appName, versionName=$versionName, triggerTime=$triggerTime"
            )

            val shareTitle = "$appName 调试日志"
            val shareMessage = "这是通过连续点击版本号触发的调试日志收集。\n\n" +
                    "设备信息：${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "系统版本：Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n" +
                    "应用版本：$versionName\n" +
                    "触发时间：$triggerTime\n\n" +
                    "请将此日志文件分享给开发人员用于调试分析。"

            URLog.d("SettingsManager", "triggerLogCollection: Calling LogUtils.collectAndShareLogs")
            LogUtils.collectAndShareLogs(context, shareTitle, shareMessage)
            URLog.i(
                "SettingsManager",
                "triggerLogCollection: Log collection and sharing initiated successfully"
            )
        } catch (e: Exception) {
            URLog.e(
                "SettingsManager",
                "triggerLogCollection: Error occurred during log collection",
                e
            )
            // 这里可以添加一个Toast来通知用户日志收集失败，但由于是隐藏功能，暂时不提示
        }
    }


    private fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: context.getString(R.string.settings_value_unknown)
        } catch (e: PackageManager.NameNotFoundException) {
            context.getString(R.string.settings_value_unknown)
        }
    }
}
