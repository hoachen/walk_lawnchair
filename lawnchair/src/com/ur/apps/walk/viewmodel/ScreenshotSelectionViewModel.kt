package com.ur.apps.walk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ur.apps.walk.model.ScreenshotOption
import com.android.launcher3.Repository.ScreenshotRepository
import kotlinx.coroutines.launch

/**
 * 截图选择ViewModel
 */
class ScreenshotSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScreenshotRepository(application)

    // 截图选项列表
    private val _screenshotOptions = MutableLiveData<List<ScreenshotOption>>(emptyList())
    val screenshotOptions: LiveData<List<ScreenshotOption>> = _screenshotOptions

    // 当前选中的选项
    private val _selectedOption = MutableLiveData<ScreenshotOption?>()
    val selectedOption: LiveData<ScreenshotOption?> = _selectedOption

    // 初始化
    init {
        loadScreenshotOptions()
    }

    /**
     * 加载截图选项数据
     */
    fun loadScreenshotOptions() {
        viewModelScope.launch {
            val options = repository.getScreenshotOptions()
            val defaultOption = options.find { it.isDefault } ?: options.firstOrNull()

            // 设置当前选中的选项
            val updatedOptions = options.map { option ->
                option.copy(isSelected = option.id == defaultOption?.id)
            }

            _screenshotOptions.postValue(updatedOptions)
            _selectedOption.postValue(defaultOption)
        }
    }

    /**
     * 选择截图选项
     */
    fun selectOption(option: ScreenshotOption) {
        val currentOptions = _screenshotOptions.value?.toMutableList() ?: mutableListOf()
        currentOptions.forEach { it.isSelected = it.id == option.id }
        _screenshotOptions.value = currentOptions
        _selectedOption.value = option
    }
}
