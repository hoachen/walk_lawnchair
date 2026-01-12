package com.ur.apps.walk

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.ur.apps.walk.adapter.WithdrawOptionsAdapter
import com.android.launcher3.databinding.ActivityWithdrawBinding
import com.ur.apps.walk.dialog.RegionSelectionDialogFragment
import com.ur.apps.walk.model.RegionOption
import com.ur.apps.walk.model.RegionUi
import com.ur.apps.walk.utils.RegionHelper
import com.ur.apps.walk.viewmodel.WithdrawViewModel
import com.android.launcher3.R
import com.ur.apps.walk.dialog.RateUsDialog
import com.ur.apps.walk.step.utils.SharedPreferencesUtils

/**
 * 提现页面
 */
class WithdrawActivity : BaseActivity() {
    private lateinit var binding: ActivityWithdrawBinding
    private lateinit var viewModel: WithdrawViewModel
    private lateinit var adapter: WithdrawOptionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithdrawBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[WithdrawViewModel::class.java]

        // 设置返回按钮
        binding.layoutToolbar.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        // 设置提现记录按钮
        binding.layoutToolbar.findViewById<View>(R.id.btn_records)?.setOnClickListener {
            // 跳转到提现记录页面
            WithdrawalRecordActivity.start(this)
        }

        // 初始化RecyclerView
        setupRecyclerViewArea()

        // 设置国家切换按钮
        binding.cardCountrySelector.findViewById<View>(R.id.region_area)?.setOnClickListener {
            showRegionSelectionDialog()
        }
        // 设置国家切换按钮
        binding.cardCountrySelector.findViewById<View>(R.id.btn_switch)?.setOnClickListener {
            showRegionSelectionDialog()
        }

        // 设置提现按钮
        binding.btnWithdraw.setOnClickListener {
            handleWithdraw()
        }

        // 观察数据变化
        observeViewModel()
    }

    /**
     * 初始化RecyclerView
     */
    private fun setupRecyclerViewArea() {

        binding.withdrawOptionLoading.visibility = View.VISIBLE
        adapter = WithdrawOptionsAdapter { option ->
            // 当用户点击选项时，通知ViewModel更新选中状态
            viewModel.selectOption(option)
        }

        binding.recyclerWithdrawOptions.apply {
            layoutManager = GridLayoutManager(this@WithdrawActivity, 2)
            adapter = this@WithdrawActivity.adapter
        }
    }

    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 观察余额信息
        viewModel.balance.observe(this) { balance ->
            binding.cardBalance.findViewById<TextView>(R.id.tv_balance_amount)?.text =
                getString(R.string.currency_format, balance.first, balance.second)
        }

        // 观察金币数量
        viewModel.coins.observe(this) { coins ->
            binding.cardBalance.findViewById<TextView>(R.id.tv_coin_amount)?.text =
                getString(R.string.coins_format, coins)
        }

        // 观察提现选项列表
        viewModel.withdrawOptions.observe(this) { options ->
            adapter.submitList(options)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.withdrawOptionLoading.visibility = View.GONE
                binding.recyclerWithdrawOptions.visibility = View.VISIBLE
            }, 500)
        }

        // 观察当前区域
        viewModel.currentRegion.observe(this) { region ->
            updateRegionUI(region)
        }
        viewModel.toast.observe(this) {
            Toast.makeText(this@WithdrawActivity, it, Toast.LENGTH_LONG).show()
        }

        // 观察选中的提现选项
        viewModel.selectedOption.observe(this) { selectedOption ->
            selectedOption?.let {
                // 更新适配器中的选中状态
                adapter.updateSelection(it)

                binding.withdrawDisplay.visibility = View.VISIBLE
                binding.withdrawDisplay.text = it.amount.toString()
                // 启用或禁用提现按钮
                val currentCoins = viewModel.coins.value ?: 0L
                binding.btnWithdraw.isEnabled = currentCoins >= it.coinsRequired

                // 更新按钮样式以反映可用性
                if (binding.btnWithdraw.isEnabled) {
                    binding.btnWithdraw.alpha = 1.0f
                } else {
                    binding.btnWithdraw.alpha = 0.5f
                }
            }
        }
    }

    /**
     * 更新区域UI显示
     */
    private fun updateRegionUI(regionCode: RegionUi) {
        // 根据区域代码设置国旗图标
//        val flagResId = when (regionCode.lowercase()) {
//            "us" -> R.drawable.ic_flag_us
//            "cn" -> R.drawable.ic_flag_cn
//            "br" -> R.drawable.ic_flag_br
//            "id" -> R.drawable.ic_flag_id
//            else -> R.drawable.ic_avatar_placeholder
//        }

        Glide.with(this)
            .load(regionCode.nationalFlagUrl)
            .centerInside()
            .into(binding.cardCountrySelector.findViewById(R.id.img_flag))

        // 设置区域名称
        val regionName = RegionHelper.getRegionName(this, regionCode.countryCode)
        binding.cardCountrySelector.findViewById<TextView>(R.id.tv_country)?.text = regionName
    }

    /**
     * 显示区域选择对话框
     */
    private fun showRegionSelectionDialog() {
        val dialog = RegionSelectionDialogFragment.newInstance()
        dialog.setOnRegionSelectedListener(object :
            RegionSelectionDialogFragment.OnRegionSelectedListener {
            override fun onRegionSelected(region: RegionOption) {
                // 更新ViewModel中的区域
                binding.withdrawOptionLoading.visibility = View.VISIBLE
                binding.recyclerWithdrawOptions.visibility = View.INVISIBLE
                viewModel.updateRegion(region)
            }
        })
        dialog.show(supportFragmentManager, "region_selection")
    }

    /**
     * 处理提现操作
     */
    private fun handleWithdraw() {
        val selectedItem = viewModel.selectedOption.value
        if (selectedItem == null) {
            Toast.makeText(this, R.string.please_select_withdraw_option, Toast.LENGTH_SHORT).show()
            return
        } else {
            val withdrawData = Gson().toJson(viewModel.buildWithDrawData(selectedItem))
            // 跳转到提现详情输入页面
            val intent = Intent(this, WithdrawAmountActivity::class.java)
            intent.putExtra("withdraw_data", withdrawData);
            startActivity(intent)
        }
    }
}
