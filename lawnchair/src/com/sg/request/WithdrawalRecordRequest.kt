package com.sg.request

/**
 * 提现记录查询请求
 */
data class WithdrawalRecordRequest(
    val pageNum: Int,      // 查询页码
    val pageSize: Int,     // 查询每页数量
    val status: Int = 0    // 查询的订单状态，默认0，0：无 1:已完成 2：中间
) {
    companion object {
        // 状态过滤常量
        const val STATUS_ALL = 0        // 所有状态
        const val STATUS_COMPLETED = 1  // 已完成
        const val STATUS_PROCESSING = 2 // 处理中
    }
} 