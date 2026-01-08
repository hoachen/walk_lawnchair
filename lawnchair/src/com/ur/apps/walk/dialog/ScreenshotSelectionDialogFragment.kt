package com.ur.apps.walk.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.ur.apps.walk.adapter.ScreenshotOptionsAdapter
import com.ur.apps.walk.model.ScreenshotOption
import com.ur.apps.walk.viewmodel.ScreenshotSelectionViewModel

/**
 * 截图选择对话框
 */
class ScreenshotSelectionDialogFragment : DialogFragment() {
    private lateinit var viewModel: ScreenshotSelectionViewModel
    private lateinit var adapter: ScreenshotOptionsAdapter

    // 截图选择回调接口
    interface OnScreenshotSelectedListener {
        fun onScreenshotSelected(option: ScreenshotOption)
    }

    private var listener: OnScreenshotSelectedListener? = null

    fun setOnScreenshotSelectedListener(listener: OnScreenshotSelectedListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Ur_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_screenshot_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[ScreenshotSelectionViewModel::class.java]

        // 设置关闭按钮
        view.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
            dismiss()
        }

        // 初始化RecyclerView
        setupRecyclerView(view)

        // 设置确认按钮
        view.findViewById<Button>(R.id.btn_confirm)?.setOnClickListener {
            viewModel.selectedOption.value?.let { option ->
                listener?.onScreenshotSelected(option)
            }
            dismiss()
        }

        // 观察数据变化
        observeViewModel()
    }

    private fun setupRecyclerView(view: View) {
        adapter = ScreenshotOptionsAdapter { option ->
            // 当用户点击选项时，通知ViewModel更新选中状态
            viewModel.selectOption(option)
        }

        view.findViewById<RecyclerView>(R.id.recycler_screenshot_options)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ScreenshotSelectionDialogFragment.adapter
        }
    }

    private fun observeViewModel() {
        // 观察截图选项列表
        viewModel.screenshotOptions.observe(viewLifecycleOwner) { options ->
            adapter.submitList(options)
        }

        // 观察选中的选项
        viewModel.selectedOption.observe(viewLifecycleOwner) { selectedOption ->
            selectedOption?.let {
                // 更新适配器中的选中状态
                adapter.updateSelection(it)
            }
        }
    }

    companion object {
        fun newInstance(): ScreenshotSelectionDialogFragment {
            return ScreenshotSelectionDialogFragment()
        }
    }
}
