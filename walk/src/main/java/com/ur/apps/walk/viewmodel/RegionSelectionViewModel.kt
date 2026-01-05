package com.ur.apps.walk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ur.apps.walk.model.RegionOption
import com.ur.apps.walk.repository.RegionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 区域选择ViewModel
 */
class RegionSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RegionRepository(application)

    // 区域选项列表
    private val _regionOptions = MutableLiveData<List<RegionOption>>(emptyList())
    val regionOptions: LiveData<List<RegionOption>> = _regionOptions

    // 当前选中的区域
    private val _selectedRegion = MutableLiveData<RegionOption?>()
    val selectedRegion: LiveData<RegionOption?> = _selectedRegion

    // 初始化
    init {
        loadRegionOptions()
    }

    /**
     * 加载区域选项数据
     */
    fun loadRegionOptions() {
        viewModelScope.launch {
            val options = repository.getRegionOptions()
            val currentRegionCode = repository.getCurrentRegion(getApplication())

            // 设置当前选中的区域
            val updatedOptions = options.map { option ->
                option.copy(isSelected = option.code == currentRegionCode)
            }

            _regionOptions.postValue(updatedOptions)
            _selectedRegion.postValue(updatedOptions.find { it.isSelected })
        }
    }

    /**
     * 选择区域
     */
    fun selectRegion(option: RegionOption) {
        val currentOptions = _regionOptions.value?.toMutableList() ?: mutableListOf()
        currentOptions.forEach { it.isSelected = it.id == option.id }
        _regionOptions.value = currentOptions
        _selectedRegion.value = option
    }

}