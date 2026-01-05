package com.ur.apps.walk.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sg.response.WithdrawalRecordItem
import com.ur.apps.walk.R

/**
 * 提现记录适配器
 */
class WithdrawalRecordAdapter(private val context: Context) : 
    RecyclerView.Adapter<WithdrawalRecordAdapter.ViewHolder>() {
    
    private val recordList = mutableListOf<WithdrawalRecordItem>()
    
    // 状态颜色映射
    private val statusColors = mapOf(
        WithdrawalRecordItem.STATUS_PENDING_REVIEW to Color.parseColor("#FF9800"),      // 橙色 - 待审核
        WithdrawalRecordItem.STATUS_REVIEW_PASSED to Color.parseColor("#2196F3"),       // 蓝色 - 审核通过
        WithdrawalRecordItem.STATUS_REVIEW_FAILED to Color.parseColor("#F44336"),       // 红色 - 审核失败
        WithdrawalRecordItem.STATUS_WITHDRAWAL_SUCCESS to Color.parseColor("#4CAF50"),  // 绿色 - 提现成功
        WithdrawalRecordItem.STATUS_WITHDRAWAL_EXCEPTION to Color.parseColor("#F44336") // 红色 - 提现异常
    )
    
    /**
     * 设置提现记录列表
     */
    fun setRecords(records: List<WithdrawalRecordItem>) {
        recordList.clear()
        recordList.addAll(records)
        notifyDataSetChanged()
    }
    
    /**
     * 获取提现记录列表
     */
    fun getRecords(): List<WithdrawalRecordItem> = recordList
    
    /**
     * 判断列表是否为空
     */
    fun isEmpty(): Boolean = recordList.isEmpty()
    
    /**
     * 清空列表
     */
    fun clear() {
        recordList.clear()
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_withdrawal_record, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recordList[position]
        holder.bind(item)
    }
    
    override fun getItemCount(): Int = recordList.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvCoin: TextView = itemView.findViewById(R.id.tv_coin)
        private val tvCommission: TextView = itemView.findViewById(R.id.tv_commission)
        private val tvCreateTime: TextView = itemView.findViewById(R.id.tv_create_time)
        
        fun bind(item: WithdrawalRecordItem) {
            // 设置订单ID
            tvOrderId.text = context.getString(R.string.withdrawal_record_order, item.orderId)
            
            // 设置状态
            tvStatus.text = getStatusText(item.status)
            tvStatus.setBackgroundColor(statusColors[item.status] ?: Color.GRAY)
            
            // 设置金额
            tvAmount.text = "${item.currencyCode}${item.amount}"
            
            // 设置金币
            tvCoin.text = context.getString(R.string.withdrawal_record_coins, item.coin)
            
            // 设置手续费信息
            if (item.hasCommission()) {
                tvCommission.visibility = View.VISIBLE
                tvCommission.text = context.getString(
                    R.string.withdrawal_record_commission,
                    item.currencyCode,
                    item.commissionAmount,
                    item.commissionCoin
                )
            } else {
                tvCommission.visibility = View.GONE
            }
            
            // 设置创建时间
            tvCreateTime.text = item.createTime
        }
        
        /**
         * 获取状态文本
         */
        private fun getStatusText(status: Int): String {
            return when (status) {
                WithdrawalRecordItem.STATUS_PENDING_REVIEW -> context.getString(R.string.withdrawal_status_pending)
                WithdrawalRecordItem.STATUS_REVIEW_PASSED -> context.getString(R.string.withdrawal_status_passed)
                WithdrawalRecordItem.STATUS_REVIEW_FAILED -> context.getString(R.string.withdrawal_status_failed)
                WithdrawalRecordItem.STATUS_WITHDRAWAL_SUCCESS -> context.getString(R.string.withdrawal_status_success)
                WithdrawalRecordItem.STATUS_WITHDRAWAL_EXCEPTION -> context.getString(R.string.withdrawal_status_exception)
                else -> context.getString(R.string.withdrawal_status_unknown)
            }
        }
    }
} 