package com.ur.apps.walk.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.android.launcher3.R

/**
 * 统一样式的对话框工具类
 */
class DialogUtils {
    companion object {
        /**
         * 创建昵称设置对话框
         */
        fun showNicknameDialog(
            context: Context,
            currentNickname: String,
            onSave: (String) -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置内容视图
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_nickname_custom, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)!!.addView(contentView)

            // 获取输入框并设置当前昵称
            val editNickname = contentView.findViewById<EditText>(R.id.edit_nickname)!!
            editNickname.setText(currentNickname)
            editNickname.setSelection(currentNickname.length)

            // 设置保存按钮点击事件
            dialog.findViewById<TextView>(R.id.btn_save)?.setOnClickListener {
                val nickname = editNickname.text.toString().trim()
                when {
                    nickname.isEmpty() -> {
                        // 可以添加提示或处理
                    }

                    nickname.length > 12 -> {
                        // 可以添加提示或处理
                    }

                    else -> {
                        onSave(nickname)
                        dialog.dismiss()
                    }
                }
            }

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * 创建性别选择对话框
         */
        fun showGenderDialog(
            context: Context,
            currentGender: Int,
            onSave: (Int) -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置内容视图
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_gender_custom, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)

            // 获取性别选项
            val tvMale = contentView.findViewById<TextView>(R.id.tv_gender_male)!!
            val tvFemale = contentView.findViewById<TextView>(R.id.tv_gender_female)!!
//            val tvOther = contentView.findViewById<TextView>(R.id.tv_gender_other)

            // 设置当前选择的性别样式
            when (currentGender) {
                0 -> { // 男性
                    tvMale.setTextColor(
                        MaterialColors.getColor(
                            context,
                            com.google.android.material.R.attr.colorOnSurface,
                            context.getThemeColor(
                                com.google.android.material.R.attr.colorOnSurface
                            )
                        )
                    )
                    tvMale.textSize = 18f
                    tvMale.typeface = android.graphics.Typeface.DEFAULT_BOLD

                    tvFemale.setTextColor(context.getColor(android.R.color.darker_gray))!!
                    tvFemale.textSize = 16f
                    tvFemale.typeface = android.graphics.Typeface.DEFAULT

//                    tvOther.setTextColor(context.getColor(android.R.color.darker_gray))
//                    tvOther.textSize = 16f
//                    tvOther.typeface = android.graphics.Typeface.DEFAULT
                }

                1 -> { // 女性
                    tvMale.setTextColor(context.getColor(android.R.color.darker_gray))!!
                    tvMale.textSize = 16f
                    tvMale.typeface = android.graphics.Typeface.DEFAULT

                    tvFemale.setTextColor(
                        MaterialColors.getColor(
                            context,
                            com.google.android.material.R.attr.colorOnSurface,
                            context.getThemeColor(
                                com.google.android.material.R.attr.colorOnSurface
                            )
                        )
                    )
                    tvFemale.textSize = 18f
                    tvFemale.typeface = android.graphics.Typeface.DEFAULT_BOLD

//                    tvOther.setTextColor(context.getColor(android.R.color.darker_gray))
//                    tvOther.textSize = 16f
//                    tvOther.typeface = android.graphics.Typeface.DEFAULT
                }

                2 -> { // 其他
                    tvMale.setTextColor(context.getColor(android.R.color.darker_gray))
                    tvMale.textSize = 16f
                    tvMale.typeface = android.graphics.Typeface.DEFAULT

                    tvFemale.setTextColor(context.getColor(android.R.color.darker_gray))
                    tvFemale.textSize = 16f
                    tvFemale.typeface = android.graphics.Typeface.DEFAULT

//                    tvOther.setTextColor(
//                        MaterialColors.getColor(
//                            context,
//                            com.google.android.material.R.attr.colorOnSurface,
//                            context.getColor(R.color.colorOnSurface)
//                        )
//                    )
//                    tvOther.textSize = 18f
//                    tvOther.typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            }

            // 设置性别选项点击事件
            tvMale.setOnClickListener {
                onSave(0) // 男性
                dialog.dismiss()
            }

            tvFemale.setOnClickListener {
                onSave(1) // 女性
                dialog.dismiss()
            }

//            tvOther.setOnClickListener {
//                onSave(2) // 其他
//                dialog.dismiss()
//            }

            // 隐藏保存按钮，因为点击选项直接保存
            dialog.findViewById<TextView>(R.id.btn_save)?.visibility = View.GONE

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * 创建身高选择对话框
         */
        fun showHeightDialog(
            context: Context,
            currentHeight: Int,
            isCm: Boolean,
            onSave: (Int, Boolean) -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置内容视图 - 这里需要创建一个新的布局文件dialog_height_custom.xml
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_height_selection, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)

            // 获取控件
            val heightPicker =
                contentView.findViewById<android.widget.NumberPicker>(R.id.picker_height)!!
            val heightDecimalPicker =
                contentView.findViewById<android.widget.NumberPicker>(R.id.picker_height_decimal)!!
            val radioGroup =
                contentView.findViewById<android.widget.RadioGroup>(R.id.radio_group_unit)!!
            val tvUnit = contentView.findViewById<TextView>(R.id.tv_unit)!!
            val radioCm = contentView.findViewById<android.widget.RadioButton>(R.id.radio_cm)!!
            val radioFt = contentView.findViewById<android.widget.RadioButton>(R.id.radio_ft)!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                heightPicker?.textColor = context.getThemeColor(
                    com.google.android.material.R.attr.colorOnSurface
                )
                heightDecimalPicker?.textColor = context.getThemeColor(
                    com.google.android.material.R.attr.colorOnSurface
                )
            }

            // 设置当前单位选择
            var currentIsCm = isCm
            radioCm?.isChecked = currentIsCm
            radioFt?.isChecked = !currentIsCm

            // 更新选择器显示
            fun updatePickerForCm() {
                heightPicker?.minValue = context.getString(R.string.height_min_cm).toInt()
                heightPicker?.maxValue = context.getString(R.string.height_max_cm).toInt()
                heightPicker?.value = currentHeight
                heightDecimalPicker?.visibility = View.GONE
                tvUnit?.text = context.getString(R.string.height_unit_cm)
            }

            fun updatePickerForFt() {
                // 将厘米转换为英尺和英寸
                val cmToFtRate = context.getString(R.string.height_cm_to_ft_rate).toFloat()
                val cmToInchRate = context.getString(R.string.height_cm_to_inch_rate).toFloat()
                val totalInches = (currentHeight / cmToInchRate).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12

                heightPicker?.minValue = context.getString(R.string.height_min_ft).toInt()
                heightPicker?.maxValue = context.getString(R.string.height_max_ft).toInt()
                heightPicker?.value = feet

                heightDecimalPicker?.visibility = View.VISIBLE
                heightDecimalPicker?.minValue = context.getString(R.string.height_min_inches).toInt()
                heightDecimalPicker?.maxValue = context.getString(R.string.height_max_inches).toInt()
                heightDecimalPicker?.value = inches

                tvUnit?.text = context.getString(R.string.height_unit_ft)
            }

            // 初始化选择器
            if (currentIsCm) {
                updatePickerForCm()
            } else {
                updatePickerForFt()
            }

            // 设置单位切换监听
            radioGroup?.setOnCheckedChangeListener { _, checkedId ->
                currentIsCm = checkedId == R.id.radio_cm
                if (currentIsCm) {
                    updatePickerForCm()
                } else {
                    updatePickerForFt()
                }
            }

            // 设置保存按钮点击事件
            dialog.findViewById<TextView>(R.id.btn_save)?.setOnClickListener {
                // 根据单位类型计算身高
                val height = if (currentIsCm) {
                    heightPicker.value
                } else {
                    // 将英尺英寸转换为厘米
                    val feet = heightPicker.value
                    val inches = heightDecimalPicker.value
                    (feet * 30.48 + inches * 2.54).toInt()
                }

                onSave(height, currentIsCm)
                dialog.dismiss()
            }

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * 创建体重选择对话框
         */
        fun showWeightDialog(
            context: Context,
            currentWeight: Float,
            isKg: Boolean,
            onSave: (Float, Boolean) -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置内容视图
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_weight_selection, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)

            // 获取控件
            val weightPicker =
                contentView.findViewById<android.widget.NumberPicker>(R.id.picker_weight)!!
            val weightDecimalPicker =
                contentView.findViewById<android.widget.NumberPicker>(R.id.picker_weight_decimal)!!
            val radioGroup =
                contentView.findViewById<android.widget.RadioGroup>(R.id.radio_group_unit)!!
            val tvUnit = contentView.findViewById<TextView>(R.id.tv_unit)!!
            val radioKg = contentView.findViewById<android.widget.RadioButton>(R.id.radio_kg)!!
            val radioLbs = contentView.findViewById<android.widget.RadioButton>(R.id.radio_lbs)!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                weightPicker.textColor = context.getThemeColor(
                    com.google.android.material.R.attr.colorOnSurface
                )
                weightDecimalPicker.textColor = context.getThemeColor(
                    com.google.android.material.R.attr.colorOnSurface
                )
            }

            // 设置当前单位选择
            var currentIsKg = isKg
            radioKg.isChecked = currentIsKg
            radioLbs.isChecked = !currentIsKg

            // 更新选择器显示
            fun updatePickerForKg() {
                // 设置整数部分选择器
                weightPicker?.minValue = context.getString(R.string.weight_min_kg).toInt()
                weightPicker?.maxValue = context.getString(R.string.weight_max_kg).toInt()

                // 设置小数部分选择器
                weightDecimalPicker?.minValue = 0
                weightDecimalPicker?.maxValue = 9

                val weightInt = currentWeight.toInt()
                val weightDecimal = ((currentWeight - weightInt) * 10).toInt()

                weightPicker?.value = weightInt
                weightDecimalPicker?.value = weightDecimal

                tvUnit?.text = context.getString(R.string.weight_unit_kg)
            }

            fun updatePickerForLbs() {
                // 将千克转换为磅
                val kgToLbsRate = context.getString(R.string.weight_kg_to_lbs_rate).toFloat()
                val weightLbs = currentWeight * kgToLbsRate
                val weightInt = weightLbs.toInt()
                val weightDecimal = ((weightLbs - weightInt) * 10).toInt()

                // 设置整数部分选择器
                weightPicker?.minValue =
                    context.getString(R.string.weight_min_lbs).toInt() // 30kg约等于66磅
                weightPicker?.maxValue =
                    context.getString(R.string.weight_max_lbs).toInt() // 200kg约等于440磅

                // 设置小数部分选择器
                weightDecimalPicker?.minValue = 0
                weightDecimalPicker?.maxValue = 9

                weightPicker?.value = weightInt
                weightDecimalPicker?.value = weightDecimal

                tvUnit?.text = context.getString(R.string.weight_unit_lbs)
            }

            // 初始化选择器
            if (currentIsKg) {
                updatePickerForKg()
            } else {
                updatePickerForLbs()
            }

            // 设置单位切换监听
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                currentIsKg = checkedId == R.id.radio_kg
                if (currentIsKg) {
                    updatePickerForKg()
                } else {
                    updatePickerForLbs()
                }
            }

            // 设置保存按钮点击事件
            dialog.findViewById<TextView>(R.id.btn_save)?.setOnClickListener {
                // 根据单位类型计算体重
                val weight = if (currentIsKg) {
                    weightPicker.value + weightDecimalPicker.value / 10f
                } else {
                    // 将磅转换为千克
                    val lbs = weightPicker.value + weightDecimalPicker.value / 10f
                    (lbs / 2.20462f)
                }

                onSave(weight, currentIsKg)
                dialog.dismiss()
            }

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * 创建隐私政策对话框
         * https://sites.google.com/view/sedeanalizprivacypolicy
         */
        @SuppressLint("SetJavaScriptEnabled")
        fun showPrivacyDialog(
            context: Context,
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            val parent = dialog.findViewById<FrameLayout>(R.id.content_container);
            // 设置内容视图
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_privacy_policy, parent, false)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)

            // 获取WebView控件和进度条
            val webView = contentView.findViewById<WebView>(R.id.webview_privacy)!!
            val progressBar = contentView.findViewById<ProgressBar>(R.id.progress_bar)!!

            // 确保WebView背景为白色
            webView.setBackgroundColor(android.graphics.Color.WHITE)

            // 配置WebView
            webView.settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
            }

            // 设置WebViewClient以处理页面加载
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar.visibility = View.VISIBLE
                    // 在页面开始加载时设置背景为白色
                    view?.setBackgroundColor(android.graphics.Color.WHITE)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                    // 在页面加载完成后确保背景为白色
                    view?.setBackgroundColor(android.graphics.Color.WHITE)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let { view?.loadUrl(it) }
                    return true
                }
            }

            // 加载隐私政策URL
            webView.loadUrl(context.getString(R.string.privacy_policy_url))

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            // 隐藏保存按钮
            dialog.findViewById<TextView>(R.id.btn_save)?.text = context.getString(R.string.ok)

            // 设置关闭按钮点击事件
            dialog.findViewById<TextView>(R.id.btn_save)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * 创建基础对话框
         */
        private fun createBaseDialog(context: Context): Dialog {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_custom_style)
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)

            // 设置窗口背景为透明，避免圆角处显示白色背景
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            return dialog
        }

        /**
         * 创建语言选择对话框
         */
        fun showLanguageSelectionDialog(
            context: Context,
            currentLanguage: String,
            languages: List<LocaleHelper.LanguageItem>,
            onLanguageSelected: (String) -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置标题
            dialog.findViewById<TextView>(R.id.tv_dialog_title)?.apply {
                text = context.getString(R.string.settings_language)
                visibility = View.VISIBLE
            }

            // 设置内容视图
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_language_selection, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)

            // 获取布局中的RecyclerView
            val recyclerView =
                contentView.findViewById<RecyclerView>(R.id.recycler_view)!!
            recyclerView.layoutManager = LinearLayoutManager(context)

            // 创建适配器
            val adapter =
                object : RecyclerView.Adapter<LanguageViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): LanguageViewHolder {
                        val itemView = LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_language, parent, false)
                        return LanguageViewHolder(itemView)
                    }

                    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
                        val language = languages[position]
                        holder.bind(language, language.code == currentLanguage)
                        holder.itemView.setOnClickListener {
                            onLanguageSelected(language.code)
                            dialog.dismiss()
                        }
                    }

                    override fun getItemCount() = languages.size
                }

            recyclerView.adapter = adapter

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            // 隐藏保存按钮，因为点击语言项直接选择
            dialog.findViewById<TextView>(R.id.btn_save)?.visibility = View.GONE

            dialog.show()
        }

        /**
         * 语言选项的ViewHolder
         */
        private class LanguageViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            private val tvLanguage: TextView = itemView.findViewById(R.id.tv_language)!!
            private val ivCheck: ImageView = itemView.findViewById(R.id.iv_check)!!

            fun bind(
                language: LocaleHelper.LanguageItem,
                isSelected: Boolean
            ) {
                tvLanguage?.text = language.name
                ivCheck?.visibility = if (isSelected) View.VISIBLE else View.GONE
            }
        }

        /**
         * 创建区域选择对话框
         */
        fun showRegionSelectionDialog(
            context: Context,
            currentRegion: String,
            regions: List<RegionHelper.RegionItem>,
            onRegionSelected: (String) -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置标题
            dialog.findViewById<TextView>(R.id.tv_dialog_title)?.apply {
                text = context.getString(R.string.settings_region)
                visibility = View.VISIBLE
            }

            // 设置内容视图
            val contentView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_region_selection_setting_item, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)

            // 获取布局中的RecyclerView
            val recyclerView =
                contentView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)!!
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

            // 创建适配器
            val adapter =
                object : androidx.recyclerview.widget.RecyclerView.Adapter<RegionViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): RegionViewHolder {
                        val itemView = LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_region, parent, false)
                        return RegionViewHolder(itemView)
                    }

                    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
                        val region = regions[position]
                        holder.bind(region, region.code == currentRegion)
                        holder.itemView.setOnClickListener {
                            onRegionSelected(region.code)
                            dialog.dismiss()
                        }
                    }

                    override fun getItemCount() = regions.size
                }

            recyclerView?.adapter = adapter

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }

            // 隐藏保存按钮，因为点击区域项直接选择
            dialog.findViewById<TextView>(R.id.btn_save)?.visibility = View.GONE

            dialog.show()
        }

        /**
         * 区域选项的ViewHolder
         */
        private class RegionViewHolder(itemView: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val tvRegion: TextView = itemView.findViewById(R.id.tv_region)!!
            private val ivCheck: ImageView = itemView.findViewById(R.id.iv_check)!!

            fun bind(region: RegionHelper.RegionItem, isSelected: Boolean) {
                tvRegion.text = region.name
                ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            }
        }

        /**
         * 创建激励广告对话框
         */
        fun showInspirationDialog(
            context: Context,
            onConfirmSelected: () -> Unit
        ) {
            // 创建对话框
            val dialog = createBaseDialog(context)

            // 设置内容视图
            val contentView =
                LayoutInflater.from(context).inflate(R.layout.dialog_inspiration_ad, null)
            dialog.findViewById<FrameLayout>(R.id.content_container)?.addView(contentView)
            dialog.setCanceledOnTouchOutside(false)

            // 设置关闭按钮点击事件
            dialog.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
                dialog.dismiss()
            }
            // 设置confirm按钮点击事件
            dialog.findViewById<View>(R.id.btn_confirm)?.setOnClickListener {
                onConfirmSelected.invoke()
                dialog.dismiss()
            }
            // 隐藏保存按钮，因为对话框中有自己的按钮
            dialog.findViewById<TextView>(R.id.btn_save)?.visibility = View.GONE
            dialog.show()
        }

    }
}
