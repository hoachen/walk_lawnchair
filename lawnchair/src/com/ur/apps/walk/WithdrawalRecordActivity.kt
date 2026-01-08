package com.ur.apps.walk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sg.ApiClient
import com.sg.BaseResponse
import com.sg.request.WithdrawalRecordRequest
import com.sg.response.WithdrawalRecordItem
import com.sg.response.WithdrawalRecordResponse
import com.ur.apps.walk.adapter.WithdrawalRecordAdapter
import com.android.launcher3.databinding.ActivityWithdrawalRecordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.android.launcher3.R

/**
 * 提现记录页面
 */
private const val TAG = "WithdrawalRecordActivity"
private const val MOCK = false

class WithdrawalRecordActivity : BaseActivity() {
    private lateinit var binding: ActivityWithdrawalRecordBinding
    private lateinit var adapter: WithdrawalRecordAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyLayout: LinearLayout

    // 当前页码
    private var currentPage = 1

    // 每页数量
    private val pageSize = 100

    // 是否加载中
    private var isLoading = false

    companion object {
        /**
         * 启动提现记录页面
         */
        fun start(context: Context) {
            val intent = Intent(context, WithdrawalRecordActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithdrawalRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupToolbar()
        setupRecyclerView()

        // 加载数据
        loadData(true)
    }

    private fun initViews() {
        recyclerView = binding.recyclerView
        progressBar = binding.progressBar
        emptyLayout = binding.layoutEmpty
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = WithdrawalRecordAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 设置滚动监听，实现加载更多
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

//                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//                val visibleItemCount = layoutManager.childCount
//                val totalItemCount = layoutManager.itemCount
//                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
//
//                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
//                    && firstVisibleItemPosition >= 0
//                ) {
//                    // 滑动到底部，加载更多
//                    loadMoreData()
//                }
            }
        })
    }

    /**
     * 加载数据
     * @param isRefresh 是否是刷新
     */
    private fun loadData(isRefresh: Boolean) {
        if (isRefresh) {
            currentPage = 1
            adapter.clear()
        }

        // 显示加载进度
        showLoading()

        // 构建请求
        val request = WithdrawalRecordRequest(
            pageNum = currentPage,
            pageSize = pageSize,
            status = WithdrawalRecordRequest.STATUS_ALL // 始终请求所有状态
        )

        // 调用API获取提现记录
        val withdrawalService = ApiClient.getWithdrawalService(this)
        withdrawalService.getRecords(request)
            .enqueue(object : Callback<BaseResponse<WithdrawalRecordResponse>> {
                override fun onResponse(
                    call: Call<BaseResponse<WithdrawalRecordResponse>>,
                    response: Response<BaseResponse<WithdrawalRecordResponse>>
                ) {
                    // 隐藏加载进度
                    hideLoading()

                    if (response.isSuccessful) {
                        val baseResponse = response.body()
                        baseResponse?.data?.let { recordResponse ->
                            handleRecordResponse(recordResponse, isRefresh)
                        } ?: run {
                            // 数据为空
                            showEmpty()
                        }
                    } else {
                        // 请求失败
                        showError(getString(R.string.withdrawal_record_load_failed))
                    }
                }

                override fun onFailure(
                    call: Call<BaseResponse<WithdrawalRecordResponse>>,
                    t: Throwable
                ) {
                    // 隐藏加载进度
                    hideLoading()
                    // 请求失败
                    showError(getString(R.string.withdrawal_record_network_error))
                }
            })
    }

    /**
     * 加载更多数据
     */
    private fun loadMoreData() {
        currentPage++
        loadData(false)
    }

    /**
     * 处理提现记录响应
     */
    private fun handleRecordResponse(response: WithdrawalRecordResponse, isRefresh: Boolean) {
        if (MOCK) {
            response.record = listOf(
                WithdrawalRecordItem(
                    amount = 11.0,
                    coin = 111,
                    status = 0,
                    createTime = "2011-01-01 01:01",
                    orderId = "1111",
                    currencyCode = "IDR",
                    commissionSwitch = 0,
                    commissionAmount = 1.1,
                    commissionCoin = 1,
                    countryCode = "ID",
                ), WithdrawalRecordItem(
                    amount = 11.0,
                    coin = 111,
                    status = 1,
                    createTime = "2011-01-01 01:01",
                    orderId = "1111",
                    currencyCode = "IDR",
                    commissionSwitch = 0,
                    commissionAmount = 1.1,
                    commissionCoin = 1,
                    countryCode = "ID",
                ), WithdrawalRecordItem(
                    amount = 11.0,
                    coin = 111,
                    status = 2,
                    createTime = "2011-01-01 01:01",
                    orderId = "1111",
                    currencyCode = "IDR",
                    commissionSwitch = 0,
                    commissionAmount = 1.1,
                    commissionCoin = 1,
                    countryCode = "ID",
                ), WithdrawalRecordItem(
                    amount = 11.0,
                    coin = 111,
                    status = 3,
                    createTime = "2011-01-01 01:01",
                    orderId = "1111",
                    currencyCode = "IDR",
                    commissionSwitch = 0,
                    commissionAmount = 1.1,
                    commissionCoin = 1,
                    countryCode = "ID",
                ), WithdrawalRecordItem(
                    amount = 11.0,
                    coin = 111,
                    status = 4,
                    createTime = "2011-01-01 01:01",
                    orderId = "1111",
                    currencyCode = "IDR",
                    commissionSwitch = 0,
                    commissionAmount = 1.1,
                    commissionCoin = 1,
                    countryCode = "ID",
                )
            )
        }
        if (response.isEmpty()) {
            if (isRefresh) {
                // 如果是刷新且没有数据，显示空状态
                showEmpty()
            } else {
                // 如果是加载更多但没有更多数据，显示没有更多数据的提示
                Toast.makeText(this, getString(R.string.withdrawal_record_no_more), Toast.LENGTH_SHORT).show()
                // 恢复页码
                currentPage--
            }
            return
        }

        // 隐藏空状态
        hideEmpty()

        // 获取记录列表
        val records = response.record

        if (isRefresh) {
            // 刷新数据
            adapter.setRecords(records)
        } else {
            // 追加数据
            val currentRecords = adapter.getRecords().toMutableList()
            currentRecords.addAll(records)
            adapter.setRecords(currentRecords)
        }
    }

    /**
     * 显示加载中
     */
    private fun showLoading() {
        isLoading = true
        progressBar.visibility = View.VISIBLE
    }

    /**
     * 隐藏加载中
     */
    private fun hideLoading() {
        isLoading = false
        progressBar.visibility = View.GONE
    }

    /**
     * 显示空状态
     */
    private fun showEmpty() {
        recyclerView.visibility = View.GONE
        emptyLayout.visibility = View.VISIBLE
    }

    /**
     * 隐藏空状态
     */
    private fun hideEmpty() {
        recyclerView.visibility = View.VISIBLE
        emptyLayout.visibility = View.GONE
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
