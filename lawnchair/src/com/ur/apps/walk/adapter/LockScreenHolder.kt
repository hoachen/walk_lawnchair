package com.ur.apps.walk.adapter

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.walk.constants.StatisticConstants
import com.android.launcher3.databinding.ItemLockerAdBinding
import com.ur.apps.walk.model.MainItem
import org.json.JSONObject


/**
 * BannerAd ViewHolder
 */
class LockScreenAdViewHolder(private val binding: ItemLockerAdBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val TAG = "BannerAdViewHolder"

    fun bind(activity: Activity, item: MainItem.LockerAdItem) {
        binding.adContainer.visibility = View.VISIBLE
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.AD_SHOW_SCREEN_ON,
            JSONObject().put(StatisticConstants.FRAGMENT, "MinimalFragment")
        )
        LockAdManager.instance.showAd(activity, binding.adContainer)
    }

    fun unbind() {
        // 注销掉

    }

}
