package com.ur.apps.walk

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sg.UserManager
import com.sg.model.UserInfo
import com.ur.apps.ad.topon.TopOnAdLoader
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.android.launcher3.databinding.ActivityCoinTaskBinding
import com.android.launcher3.databinding.ItemCoinTaskBinding
import com.ur.apps.walk.model.TaskModel
import com.ur.apps.walk.step.manager.StepManager

/**
 * 赚取金币任务页面
 */
class CoinTaskActivity : BaseRewardActivity() {

    private lateinit var binding: ActivityCoinTaskBinding
    private lateinit var stepManager: StepManager
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoinTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initStepManager()
        setupToolbar()
        setupRecyclerView()
        TDAnalyticsManager.reportButtonShow(
            adType = "Reward",
            adPosition = "任务_激励",
            adPositionType = "激励",
            placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
        )
    }

    private fun initViews() {
        binding.layoutToolbar.apply {
            coinCircleButton.tvCoins.setOnClickListener {
                val intent = Intent(this@CoinTaskActivity, WithdrawActivity::class.java)
                startActivity(intent)
            }
            val userInfo = UserManager.instance.getUserInfo()
            userInfo?.let {
                coinCircleButton.tvCoinsNums.text = it.udCoin.toString()
            }
        }
    }

    private fun initStepManager() {
        stepManager = StepManager.getInstance(this)
    }

    private fun setupToolbar() {
        binding.layoutToolbar.apply {
            btnHome.setOnClickListener {
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter()
        binding.rvTaskList.apply {
            layoutManager = LinearLayoutManager(this@CoinTaskActivity)
            adapter = taskAdapter
        }

        // 加载并显示任务
        updateTaskList()
    }

    private fun updateTaskList() {
        val todaySteps = stepManager.getTodaySteps()
        val updatedTasks = TaskModel.updateTaskCompletion(this, todaySteps)
        taskAdapter.submitList(updatedTasks)
    }

    override fun handleUserInfoChanged(userInfo: UserInfo) {
        super.handleUserInfoChanged(userInfo)
        // 显示金币的数量
        binding.layoutToolbar.coinCircleButton.tvCoinsNums.text = userInfo.udCoin.toString()
    }

    /**
     * 处理任务操作（模拟奖励领取逻辑）
     */
    private fun handleTaskAction(taskId: Int, targetDistance: Int, currentDistance: Int) {
        if (currentDistance >= targetDistance) {
            showRewardAd()
        } else {
            // 任务未完成，提示用户继续步行
            val remaining = targetDistance - currentDistance
            // 使用资源字符串，支持多语言
            showToast(getString(R.string.task_remaining_steps, remaining))
        }
    }


    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * 任务列表适配器
     */
    inner class TaskAdapter : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

        private val tasks = mutableListOf<TaskModel>()

        fun submitList(newTasks: List<TaskModel>) {
            tasks.clear()
            tasks.addAll(newTasks)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val binding = ItemCoinTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TaskViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(tasks[position])
        }

        override fun getItemCount(): Int = tasks.size

        inner class TaskViewHolder(private val binding: ItemCoinTaskBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(task: TaskModel) {
                // 设置任务标题
                binding.tvTaskTitle.text = getTaskTitle(task)

                // 根据任务状态设置UI
                val todaySteps = stepManager.getTodaySteps()
                val isCompleted = todaySteps >= task.stepGoal

                if (isCompleted) {
                    // 任务已完成
                    if (task.claimed) {
                        binding.root.alpha = 0.4f
                        // 已领取：按钮显示已领取状态
                        binding.btnAction.setImageResource(R.drawable.ic_check_circle)
                    } else {
                        // 已完成但未领取：按钮显示可领取状态
                        binding.root.alpha = 1f
                        binding.btnAction.setImageResource(R.drawable.ic_play)
                    }
                } else {
                    // 任务未完成
                    binding.root.alpha = 0.7f
                    binding.btnAction.setImageResource(R.drawable.ic_play)
                }

                // 设置点击事件
                binding.taskItem.setOnClickListener {
                    handleTaskAction(task.id, task.stepGoal, todaySteps)
                }
            }

            private fun getTaskTitle(task: TaskModel): String {
                return when (task.id) {
                    1 -> itemView.context.getString(R.string.task_1_text, task.stepGoal)
                    2 -> itemView.context.getString(R.string.task_2_text, task.stepGoal)
                    3 -> itemView.context.getString(R.string.task_3_text, task.stepGoal)
                    else -> "Task ${task.id}: By Walking ${task.stepGoal} steps"
                }
            }
        }
    }
}
