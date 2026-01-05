package com.ur.apps.walk.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.sg.response.RewardImpResponse
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.walk.R

class RewardCoinDialog(
    context: Context,
    private val reward: RewardImpResponse,
    private val showBanner: Boolean = true, private val redeem: Boolean
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

    }

    private var onReWatchRewardAdListener: (() -> Unit)? = null
    private var onGetRedeemListener: (() -> Unit)? = null
    private var onTaskDoneClaimAdListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_task_done)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT);
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setupViews()
    }

    private fun setupViews() {
        val taskDoneIconTreasure = findViewById<ImageView>(R.id.task_done_icon_treasure)
        val taskDoneCoinCount = findViewById<TextView>(R.id.task_done_coin_count)
        val taskDoneCoinAsMoney = findViewById<TextView>(R.id.task_done_coin_as_money)
        taskDoneIconTreasure.setOnClickListener {
            //todo fix me to display total coin count
            //todo fix me to display coin as money
            taskDoneIconTreasure.setImageResource(R.drawable.task_done_icon_treasure_open)
        }

        val taskDoneClaim = findViewById<TextView>(R.id.task_done_claim)
        taskDoneClaim.paintFlags = taskDoneClaim.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        taskDoneClaim.setOnClickListener {
            onTaskDoneClaimAdListener?.invoke()
            dismiss()
        }
        taskDoneCoinCount.text = reward.rewardCoin
        taskDoneCoinAsMoney.text = "≈${reward.currencyCode} ${reward.rewardAmount}"
        val taskDoneWatchAdVideo = findViewById<TextView>(R.id.task_done_watch_ad_video)
        if (redeem) {
            taskDoneWatchAdVideo.isEnabled = true
            taskDoneWatchAdVideo.text = context.getString(R.string.go_get_redeem)
            taskDoneWatchAdVideo.setOnClickListener {
                onGetRedeemListener?.invoke()
                dismiss()
            }
        } else {
            taskDoneWatchAdVideo.isEnabled = (reward.doubleSwitch == "t")
            taskDoneWatchAdVideo.setOnClickListener {
                onReWatchRewardAdListener?.invoke()
                dismiss()
            }
        }
        val bannerAdContainer = findViewById<FrameLayout>(R.id.banner_ad_view_container)
        if (showBanner) {
            bannerAdContainer.visibility = View.VISIBLE
            AdLoaderManager.loadBannerAd(context, bannerAdContainer)
        } else {
            bannerAdContainer.visibility = View.GONE
        }
    }

    /**
     * 设置重新看激励视频的监听
     */
    fun setReWatchRewardAdListener(listener: () -> Unit): RewardCoinDialog {
        this.onReWatchRewardAdListener = listener
        return this
    }


    /**
     * 设置提现点击
     */
    fun setGetRedeemListener(listener: () -> Unit): RewardCoinDialog {
        this.onGetRedeemListener = listener
        return this
    }

    /**
     * 设置Claim回调
     */
    fun setOnTaskDoneClaimAdListener(listener: () -> Unit): RewardCoinDialog {
        this.onTaskDoneClaimAdListener = listener
        return this
    }

}