package com.ur.apps.walk.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.android.launcher3.R
import com.ur.apps.walk.settings.model.SettingItem
import com.ur.apps.walk.step.utils.SharedPreferencesUtils
import com.ur.apps.walk.utils.LocaleHelper
import com.ur.apps.walk.utils.RegionHelper
import com.ur.apps.walk.utils.getThemeColor
import androidx.core.net.toUri

class SettingsManager(private val context: Context) {
    private val sharedPreferences = SharedPreferencesUtils(context)

    fun getSettingItems(): List<SettingItem.Section> {
        val list = mutableListOf(
//            // 我的账户
//            SettingItem.Section(
//                id = context.getString(R.string.settings_key_account),
//                title = context.getString(R.string.settings_account),
//                items = listOf(
//                    SettingItem.Item.Value(
//                        id = context.getString(R.string.settings_key_nickname),
//                        title = context.getString(R.string.settings_nickname),
//                        value = sharedPreferences.getParam(context.getString(R.string.pref_key_nickname), context.getString(R.string.not_set)) as String,
//                        onClick = {}
//                    )
//                )
//            ),
//            // 主题配色
//            SettingItem.Section(
//                id = context.getString(R.string.settings_key_theme),
//                title = context.getString(R.string.settings_theme),
//                items = listOf(
//                    SettingItem.Item.ColorPicker(
//                        id = "theme_color",
//                        title = context.getString(R.string.settings_theme_color),
//                        selectedColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
//                        onClick = {}
//                    )
//                ),
//                isExpanded = true
//            ),
            // 计步数据源
//            SettingItem.Section(
//                id = "step_source",
//                title = context.getString(R.string.settings_step_source),
//                items = listOf(
//                    SettingItem.Item.Switch(
//                        id = "use_system_step",
//                        title = context.getString(R.string.settings_step_source_system),
//                        isChecked = sharedPreferences.getParam("use_system_step", false) as Boolean
//                    )
//                )
//            ),
            // 计步相关问题
//            SettingItem.Section(
//                id = "step_issues",
//                title = context.getString(R.string.settings_step_issues),
//                items = listOf(
//                    SettingItem.Item.Action(
//                        id = "step_help",
//                        title = context.getString(R.string.settings_feedback_qa),
//                        onClick = {}
//                    )
//                )
//            ),
            // 属性配置
            SettingItem.Section(
                id = context.getString(R.string.settings_key_preferences),
                title = context.getString(R.string.settings_preferences),
                items = listOf(
                    SettingItem.Item.Distance(
                        id = "distance_unit",
                        title = context.getString(R.string.settings_distance_unit),
                        isKm = true,
                    ),
                    SettingItem.Item.Switch(
                        id = context.getString(R.string.settings_key_notification),
                        title = context.getString(R.string.settings_notification),
                        isChecked = true,
                    ),
                    SettingItem.Item.Switch(
                        id = context.getString(R.string.settings_key_sounds),
                        title = context.getString(R.string.settings_sounds),
                        isChecked = true,
                    ),
                    SettingItem.Item.Switch(
                        id = context.getString(R.string.settings_key_vibration),
                        title = context.getString(R.string.settings_vibration),
                        isChecked = true,
                    ),
                ),
            ),
            // 喜欢我们吗
            SettingItem.Section(
                id = "like_us",
                title = context.getString(R.string.settings_like_us),
                items = listOf(
                    SettingItem.Item.Action(
                        id = "rate_us",
                        title = context.getString(R.string.settings_rate_us),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = "market://details?id=${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        },
                    ),
                    SettingItem.Item.Action(
                        id = context.getString(R.string.settings_key_share),
                        title = context.getString(R.string.settings_share),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://play.google.com/store/apps/details?id=${context.packageName}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                    ),
                ),
            ),
        )
//        // Feedback & QA
//        val feedback = SettingItem.Section(
//            id = context.getString(R.string.settings_key_feedback),
//            title = context.getString(R.string.settings_feedback),
//            items = listOf()
//        )
//        val contactInfo = UserManager.instance.getContactUsInfo();
//
//        contactInfo?.let {
//            feedback.title = it.btnText
//            val actionList = mutableListOf(
//                SettingItem.Item.Action(
//                    id = "display_text",
//                    title = contactInfo.text,
//                    onClick = {}
//                ),
//            )
//                if (contactInfo.hasEmail()) {
//                actionList.add(SettingItem.Item.Action(
//                    id = "email_us",
//                    title = contactInfo.email!!,
//                    onClick = {
//                        com.ur.apps.walk.utils.LogUtils.collectAndShareLogs(
//                            context,
//                            "${context.getString(R.string.app_name)} 日志反馈",
//                            "请在此描述您遇到的问题：\n\n\n\n\n" +
//                                    "----------\n" +
//                                    "设备信息：${Build.MANUFACTURER} ${Build.MODEL}\n" +
//                                    "系统版本：Android ${Build.VERSION.RELEASE}\n" +
//                                    "应用版本：${getAppVersionName(context)}\n"
//                        )
//                    }
//                ))
//            }
//            if (contactInfo.hasTelegram()) {
//                actionList.add(SettingItem.Item.Action(
//                    id = "tel_us",
//                    title = contactInfo.tg!!,
//                    onClick = {}
//                ))
//            }
//            feedback.items = actionList
//            list.add(feedback)
//        }

        // 隐私协议&条款
        list.add(
            SettingItem.Section(
                id = context.getString(R.string.settings_key_privacy),
                title = context.getString(R.string.settings_privacy),
                items = listOf(
                    SettingItem.Item.Action(
                        id = "privacy_policy",
                        title = context.getString(R.string.settings_privacy_policy),
                        onClick = {
                            com.ur.apps.walk.utils.DialogUtils.showPrivacyDialog(context)
                        },
                    ),
//                    SettingItem.Item.Action(
//                        id = context.getString(R.string.settings_key_terms),
//                        title = context.getString(R.string.settings_terms),
//                        onClick = {}
//                    )
                ),
            ),
        )
        // 语言切换
        list.add(
            SettingItem.Section(
                id = context.getString(R.string.settings_key_language),
                title = context.getString(R.string.settings_language),
                items = listOf(
                    SettingItem.Item.Value(
                        id = context.getString(R.string.settings_key_language),
                        title = context.getString(R.string.settings_language),
                        value = LocaleHelper.getLanguageName(
                            context,
                            LocaleHelper.getLanguage(context),
                        ),
                        onClick = {},
                    ),
                ),
            ),
        )
        // 所在国家地区
        list.add(
            SettingItem.Section(
                id = context.getString(R.string.settings_key_region),
                title = context.getString(R.string.settings_region),
                items = listOf(
                    SettingItem.Item.Value(
                        id = context.getString(R.string.settings_key_region),
                        title = context.getString(R.string.settings_region),
                        value = RegionHelper.getRegionName(
                            context,
                            RegionHelper.getRegion(context),
                        ),
                        onClick = {},
                    ),
                ),
            ),
        )
        // 版本号
        list.add(
            SettingItem.Section(
                id = context.getString(R.string.settings_key_version),
                title = context.getString(R.string.settings_version),
                items = listOf(
                    SettingItem.Item.Value(
                        id = context.getString(R.string.settings_key_version),
                        title = context.getString(R.string.settings_version),
                        value = try {
                            val packageInfo =
                                context.packageManager.getPackageInfo(context.packageName, 0)
                            context.getString(
                                R.string.settings_version_format,
                                packageInfo.versionName
                                    ?: context.getString(R.string.settings_value_unknown),
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            context.getString(R.string.settings_value_unknown)
                        },
                        onClick = {
                        },
                    ),
                ),
            ),
        )
        return list
    }

    fun updateSetting(id: String, value: Any) {
        when (id) {
            context.getString(R.string.settings_key_nickname) -> sharedPreferences.setParam(
                context.getString(
                    R.string.pref_key_nickname,
                ),
                value,
            )

            "use_system_step" -> sharedPreferences.setParam("use_system_step", value)
            "distance_unit" -> sharedPreferences.setParam("distance_unit", value)
            context.getString(R.string.settings_key_notification) -> sharedPreferences.setParam(
                context.getString(R.string.settings_key_notification),
                value,
            )

            context.getString(R.string.settings_key_sounds) -> sharedPreferences.setParam(
                context.getString(
                    R.string.settings_key_sounds,
                ),
                value,
            )

            context.getString(R.string.settings_key_vibration) -> sharedPreferences.setParam(
                context.getString(
                    R.string.settings_key_vibration,
                ),
                value,
            )

            context.getString(R.string.settings_key_language) -> sharedPreferences.setParam(
                context.getString(
                    R.string.settings_key_language,
                ),
                value,
            )

            context.getString(R.string.settings_key_region) -> sharedPreferences.setParam(
                context.getString(
                    R.string.settings_key_region,
                ),
                value,
            )
        }
    }

}
