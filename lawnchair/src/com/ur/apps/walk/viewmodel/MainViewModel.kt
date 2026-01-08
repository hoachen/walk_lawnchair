package com.ur.apps.walk.viewmodel

import android.app.Application
import com.ur.apps.utils.URLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sg.UserManager
import com.ur.apps.walk.repository.RegionRepository
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val regionRepo: RegionRepository = RegionRepository(application)

    /**
     * 加载提现选项数据
     */
    fun refreshUserInfo() {
        viewModelScope.launch {
            UserManager.instance.refreshUserInfo()
            val regions = regionRepo.getRegionOptions()
            URLog.i(TAG, "refreshUserInfo: ($regions)")
        }
    }


    override fun onCleared() {
        super.onCleared()

    }

}
