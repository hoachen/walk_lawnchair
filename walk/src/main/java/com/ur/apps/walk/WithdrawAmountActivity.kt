package com.ur.apps.walk

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sg.response.FormField
import com.ur.apps.utils.URLog
import com.ur.apps.walk.adapter.WithDrawAmountAdapter
import com.ur.apps.walk.databinding.ActivityWithdrawAmountBinding
import com.ur.apps.walk.utils.RegionHelper
import com.ur.apps.walk.viewmodel.WithdrawAmountViewModel
import com.ur.apps.walk.widget.FancyDialog

/**
 * 提现金额输入和用户信息输入页面
 */

private const val TAG = "WithdrawAmountActivity"

class WithdrawAmountActivity : BaseActivity() {
    private lateinit var binding: ActivityWithdrawAmountBinding
    private lateinit var viewModel: WithdrawAmountViewModel

    private lateinit var adapter: WithDrawAmountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithdrawAmountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[WithdrawAmountViewModel::class.java]

        viewModel.handleInputWithDrawData(intent.getStringExtra("withdraw_data"))

        // 设置关闭按钮
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 设置提交按钮
        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }

        observeViewModel()
        // 初始化RecyclerView
        initFormRecyclerView()
        initDocumentTypeSpinner()

        viewModel.loadWithdrawData()
    }

//    /**
//     * 初始化国家码
//     */
//    private fun initCountryCode() {
//        // 使用CountryCodeDialogFragment提供的默认国家码
//        binding.tvCountryCode.text = when (RegionHelper.getRegion(this)) {
//            RegionHelper.ID -> "+62"
//            RegionHelper.US -> "+1"
//            RegionHelper.BR -> "+55"
//            else -> "+62"
//        }
//    }

    /**
     * 初始化文档类型Spinner
     */
    private fun initDocumentTypeSpinner() {
        adapter.spinnerListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val field: FormField? = parent?.tag as? FormField
                    field?.options?.let {
                        val value = it[position].value ?: ""
                        URLog.i(TAG, "document type select($value)")
                        viewModel.formFieldMap.put(
                            field.fieldKey,
                            Pair(
                                field,
                                value
                            )
                        )
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // 不处理
                }
            }
    }
//
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 观察余额信息
        viewModel.balance.observe(this) { balance ->
            val region = RegionHelper.getRegion(this)
            binding.tvWithdrawalAmount.text = getString(R.string.currency_format, region, balance)
        }

        // 观察金币数量
        viewModel.coins.observe(this) { coins ->
            binding.tvCoinAmount.text = getString(R.string.coins_format, coins)
        }

        viewModel.formFields.observe(this) { formFields ->
            adapter.updateFields(formFields)
        }

        viewModel.withdrawSuccess.observe(this) {
            FancyDialog(this).setOnCloseListener {
                if (!isFinishing) {
                    finish()
                }
            }.setOnCashOutListener {
                val telUrl = it.tag.toString()
                if (telUrl.isNotEmpty()) {
                    val intent = WebViewActivity.createIntent(
                        context = this,
                        url = telUrl
                    )
                    this.startActivity(intent)
                }
            }.show()
        }
    }

    /**
     * 初始化表单RecyclerView
     */
    private fun initFormRecyclerView() {
        binding.recyclerViewForm.layoutManager = LinearLayoutManager(this)
        adapter = WithDrawAmountAdapter { field, value ->
            // 处理字段值变化
            // 这里可以更新ViewModel中的字段值
            // 例如：viewModel.updateFieldValue(field.fieldKey, value)
            when (field.type) {
                FormField.SELECT -> {
                    // nothing to to here
                }

                else -> {
                    URLog.i(TAG, "field setup key(${field.fieldKey})  value($value)")
                    viewModel.formFieldMap.put(
                        field.fieldKey, Pair(field, value)
                    )
                }
            }
        }
        binding.recyclerViewForm.adapter = adapter


        // 观察表单字段数据变化
        // 假设ViewModel中有formFields LiveData
        // viewModel.formFields.observe(this) { fields ->
        //     adapter.updateFields(fields)
        // }
    }

    /**
     * 验证输入并提交
     */
    private fun validateAndSubmit() {
        var withUnFillupKey = false
        val formFieldMap: MutableMap<String, Pair<FormField, String>> = mutableMapOf()

        viewModel.formFieldMap.forEach {
            URLog.i(TAG, "key(${it.key}) value(${it.value})")
            if (it.value.second.isEmpty()) {
                if (it.value.first.required == 1) {
                    android.widget.Toast.makeText(
                        this,
                        getString(R.string.key_should_not_empty, it.key),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    withUnFillupKey = true
                }
            } else {
                formFieldMap.put(
                    it.key, it.value
                )
            }
        }
        if (withUnFillupKey) {
            return
        }
        // 提交处理
        viewModel.handleWithdraw(formFieldMap)
    }
}
