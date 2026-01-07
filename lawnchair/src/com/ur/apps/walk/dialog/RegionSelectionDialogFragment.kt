package com.ur.apps.walk.dialog

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.ur.apps.walk.adapter.RegionOptionsAdapter
import com.ur.apps.walk.model.RegionOption
import com.ur.apps.walk.viewmodel.RegionSelectionViewModel
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * 区域选择对话框
 */
class RegionSelectionDialogFragment : DialogFragment() {
    private lateinit var viewModel: RegionSelectionViewModel
    private lateinit var adapter: RegionOptionsAdapter

    // 区域选择回调接口
    interface OnRegionSelectedListener {
        fun onRegionSelected(regionCode: RegionOption)
    }

    private var listener: OnRegionSelectedListener? = null

    fun setOnRegionSelectedListener(listener: OnRegionSelectedListener) {
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
        return inflater.inflate(R.layout.dialog_region_selection, container, false)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[RegionSelectionViewModel::class.java]

        // 设置关闭按钮
        view.findViewById<ImageView>(R.id.btn_close).setOnClickListener {
            dismiss()
        }

        // 初始化RecyclerView
        setupRecyclerView(view)

        // 设置确认按钮
        view.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            viewModel.selectedRegion.value?.let { region ->
                listener?.onRegionSelected(region)
            }
            dismiss()
        }

        // 显示进度条
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE

        // 观察数据变化
        observeViewModel(progressBar)
    }

    private fun setupRecyclerView(view: View) {
        adapter = RegionOptionsAdapter { option ->
            // 当用户点击选项时，通知ViewModel更新选中状态
            viewModel.selectRegion(option)
        }

        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@RegionSelectionDialogFragment.adapter
        }
    }

    private fun observeViewModel(progressBar: ProgressBar) {
        // 观察区域选项列表
        viewModel.regionOptions.observe(viewLifecycleOwner) { options ->
            adapter.submitList(options)
            // 数据加载完成，隐藏进度条
            Handler(Looper.getMainLooper()).postDelayed({progressBar.visibility = View.GONE},500)
        }

        // 观察选中的区域
        viewModel.selectedRegion.observe(viewLifecycleOwner) { selectedRegion ->
            selectedRegion?.let {
                // 更新适配器中的选中状态
                adapter.updateSelection(it)
            }
        }
    }

    companion object {
        fun newInstance(): RegionSelectionDialogFragment {
            return RegionSelectionDialogFragment()
        }
    }
}
