package com.ur.apps.walk.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.ur.apps.utils.URLog
import android.view.View
import android.view.Window
import android.widget.TextView
import com.sg.UserManager
import com.android.launcher3.R
import com.ur.apps.walk.WebViewActivity

private const val TAG = "FancyDialog"

class FancyDialog(context: Context) : Dialog(context) {

    private var closeListener: (() -> Unit)? = null
    private var btnGoListener: ((view: View) -> Unit)? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_fancy)
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        // 初始化控件
        setupViews()
    }

    private fun setupViews() {

        // 设置关闭按钮点击事件
        val closeButton = findViewById<View>(R.id.fake_close_click)
        closeButton.setOnClickListener {
            closeListener?.invoke()
            dismiss()
        }

        val telegramUrl = findViewById<TextView>(R.id.telegram_url)
        val telUrl = UserManager.instance.getGroupUrl() ?: ""
        URLog.i(TAG, "telUrl is : $telUrl")
        telegramUrl.text = telUrl
        telegramUrl.tag = telUrl
        telegramUrl.setOnClickListener {
            val intent = WebViewActivity.createIntent(
                context = this@FancyDialog.context,
                url = telUrl
            )
            this@FancyDialog.context.startActivity(intent)
        }

        val btnGo = findViewById<View>(R.id.btn_go)
        btnGo.setOnClickListener {
            btnGoListener?.invoke(telegramUrl)
        }
    }

    /**
     * 设置关闭按钮点击监听器
     */
    fun setOnCloseListener(listener: () -> Unit): FancyDialog {
        this.closeListener = listener
        return this
    }

    /**
     * 设置提现按钮点击监听器
     */
    fun setOnCashOutListener(listener: (view: View) -> Unit): FancyDialog {
        this.btnGoListener = listener
        return this
    }

}
