package com.sg.response

/**
 * 提现记录响应
 */
data class WithdrawalRecordResponse(
    var record: List<WithdrawalRecordItem>  // 提现记录列表
) {
    /**
     * 判断记录列表是否为空
     */
    fun isEmpty(): Boolean {
        return record.isEmpty()
    }
    
    /**
     * 获取记录数量
     */
    fun getCount(): Int {
        return record.size
    }
} 