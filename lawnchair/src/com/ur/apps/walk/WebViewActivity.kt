package com.ur.apps.walk

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.android.launcher3.R
import com.android.launcher3.databinding.ActivityWebviewBinding

/**
 * WebViewActivity - 用于显示网页内容的Activity
 * 支持传入URL和标题参数
 */
class WebViewActivity : BaseActivity() {
    private lateinit var binding: ActivityWebviewBinding
    private var url: String? = null
    private var title: String? = null

    companion object {
        /**
         * 创建启动WebViewActivity的Intent
         * @param context 上下文
         * @param url 要加载的URL
         * @param title 可选的页面标题
         * @return 配置好的Intent
         */
        fun createIntent(context: Context, url: String, title: String? = null): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                putExtra(context.getString(R.string.web_view_url_extra), url)
                title?.let { putExtra(context.getString(R.string.web_view_title_extra), it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取Intent参数
        url = intent.getStringExtra(getString(R.string.web_view_url_extra))
        title = intent.getStringExtra(getString(R.string.web_view_title_extra))

        // 设置工具栏
        setupToolbar()

        // 配置WebView
        setupWebView()

        // 加载URL
        loadUrl()
    }

    private fun setupToolbar() {
        // 设置标题
        if (!title.isNullOrEmpty()) {
            binding.layoutToolbar.tvTitle.text = title
        }

        // 设置返回按钮
        binding.layoutToolbar.btnBack.setOnClickListener {
            // 如果WebView可以返回上一页，则返回上一页
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                finish()
            }
        }

        // 设置刷新按钮
        binding.layoutToolbar.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // 配置WebView设置
        binding.webView.settings.apply {
            // 启用JavaScript
            javaScriptEnabled = true
            // 设置缓存模式
            cacheMode = WebSettings.LOAD_DEFAULT
            // 启用DOM存储
            domStorageEnabled = true
            // 支持缩放
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            // 加载图片
            loadsImagesAutomatically = true
            // 使用宽视图端口
            useWideViewPort = true
            // 调整到屏幕大小
            loadWithOverviewMode = true
        }

        // 设置WebViewClient
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // 显示进度条
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 隐藏进度条
                binding.progressBar.visibility = View.GONE

                // 如果没有设置标题，使用网页标题
                if (title.isNullOrEmpty() && view?.title != null) {
                    binding.layoutToolbar.tvTitle.text = view.title
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // 隐藏进度条
                binding.progressBar.visibility = View.GONE
                // 显示错误信息
                Toast.makeText(
                    this@WebViewActivity,
                    getString(R.string.error_loading_url),
                    Toast.LENGTH_SHORT
                ).show()
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // 判断是否为 Telegram URL
                return if (url.startsWith("tg:")) {
                    try {
                        // 使用 Intent 打开 Telegram
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        view.context.startActivity(intent)
                        true
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(view.context, getString(R.string.no_app_to_handle_action), Toast.LENGTH_SHORT).show()
                        true
                    }
                } else {
                    // 其他网址交给 WebView 加载
                    view.loadUrl(url)
                    false
                }
            }
        }

        // 设置WebChromeClient以处理进度更新
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                // 更新进度条
                binding.progressBar.progress = newProgress

                // 如果加载完成，隐藏进度条
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadUrl() {
        if (url.isNullOrEmpty()) {
            // 如果URL为空，显示错误并关闭Activity
            Toast.makeText(
                this,
                getString(R.string.error_loading_url),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } else {
            // 加载URL
            binding.webView.loadUrl(url!!)
        }
    }

    override fun onBackPressed() {
        // 处理返回键：如果WebView可以返回上一页，则返回上一页
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        // 清理WebView
        binding.webView.apply {
            stopLoading()
            // 修复：不再将webViewClient和webChromeClient设置为null
            setWebViewClient(WebViewClient())
            setWebChromeClient(WebChromeClient())
            clearHistory()
            destroy()
        }
        super.onDestroy()
    }
}
