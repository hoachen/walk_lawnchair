package com.ur.apps.walk.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.ur.apps.walk.databinding.DialogCountryCodeBinding
import com.ur.apps.walk.utils.getThemeColor

/**
 * 国家代码选择对话框
 */
class CountryCodeDialogFragment : DialogFragment() {
    private var _binding: DialogCountryCodeBinding? = null
    private val binding get() = _binding!!

    private var listener: OnCountryCodeSelectedListener? = null
    private var currentCode: String = DEFAULT_CODE

    companion object {
        private const val ARG_CURRENT_CODE = "current_code"
        private const val DEFAULT_CODE = "+1" // 默认使用列表中的第一个国家码

        // 国家代码数据 - 移到这里使其可被静态访问
        private val COUNTRY_CODES = listOf(
            CountryCode("+1", "USA"),
            CountryCode("+62", "Indonesia"),
            CountryCode("+55", "Country Code3"),
        )

        fun newInstance(currentCode: String = DEFAULT_CODE): CountryCodeDialogFragment {
            val fragment = CountryCodeDialogFragment()
            val args = Bundle()
            args.putString(ARG_CURRENT_CODE, currentCode)
            fragment.arguments = args
            return fragment
        }

        /**
         * 获取所有可用的国家代码列表
         */
        fun getCountryCodes(): List<CountryCode> = COUNTRY_CODES

        /**
         * 获取默认的国家代码
         */
        fun getDefaultCountryCode(): String = DEFAULT_CODE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_CURRENT_CODE)?.let {
            currentCode = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCountryCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置对话框宽度
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 设置关闭按钮
        binding.btnCloseCountryCode.setOnClickListener {
            dismiss()
        }

        // 初始化国家代码列表
        setupCountryCodes()
    }

    private fun setupCountryCodes() {
        binding.rgCountryCodes.removeAllViews()

        COUNTRY_CODES.forEachIndexed { index, countryCode ->
            val radioButton = RadioButton(context)
            radioButton.id = View.generateViewId()
            radioButton.text = "${countryCode.name}       ${countryCode.code}"
            // 检查是否是当前选中的国家码
            radioButton.isChecked = countryCode.code == currentCode
            context?.let {
                radioButton.setTextColor(
                    it.getThemeColor(
                        com.google.android.material.R.attr.colorOnSurface
                    )
                )
            }

            radioButton.setPadding(32, 32, 32, 32)

            radioButton.setOnClickListener {
                listener?.onCountryCodeSelected(countryCode.code)
                dismiss()
            }

            binding.rgCountryCodes.addView(radioButton)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnCountryCodeSelectedListener(listener: OnCountryCodeSelectedListener) {
        this.listener = listener
    }

    /**
     * 国家代码选择监听器
     */
    interface OnCountryCodeSelectedListener {
        fun onCountryCodeSelected(code: String)
    }

    /**
     * 国家代码数据类
     */
    data class CountryCode(
        val code: String,
        val name: String
    )
} 