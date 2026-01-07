package com.ur.apps.walk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.android.launcher3.databinding.ItemTaskItemBinding
import com.ur.apps.walk.model.TaskModel

/**
 * 横向任务列表的Adapter
 */
class TaskItemAdapter(
    private val onItemClickListener: MainItemClickListener,
    private var currentDistance: Int = 0
) : RecyclerView.Adapter<TaskItemAdapter.TaskItemViewHolder>() {

    private val tasks = mutableListOf<TaskModel>()

    fun submitList(newTasks: List<TaskModel>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    fun updateCurrentDistance(newDistance: Int) {
        currentDistance = newDistance
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskItemViewHolder {
        val binding = ItemTaskItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskItemViewHolder(binding, currentDistance)
    }

    override fun onBindViewHolder(holder: TaskItemViewHolder, position: Int) {
        holder.bind(tasks[position], currentDistance)
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskItemViewHolder(
        private val binding: ItemTaskItemBinding,
        private var currentDistance: Int = 0
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskModel, currentDistance: Int) {
            this.currentDistance = currentDistance

            // 设置任务标题（步数）- 根据HTML设计，显示格式如"1,000"
            binding.tvTaskTitle.text = formatStepCount(task.stepGoal)

            // 设置任务副标题（根据步数显示不同的描述）
            binding.tvTaskSubtitle.text = getTaskDescription(task.stepGoal)

            // 计算实际进度（当前步数 / 目标步数）
            val actualProgress = if (task.stepGoal > 0) {
                (currentDistance.toFloat() / task.stepGoal).coerceIn(0f, 1f)
            } else {
                0f
            }

            // 设置进度条宽度（根据实际进度）
            // 使用post确保在布局完成后获取宽度
            binding.progressTrack.post {
                val trackWidth = binding.progressTrack.width
                val progressWidth = (actualProgress * trackWidth).toInt()
                val layoutParams = binding.progressValue.layoutParams
                layoutParams.width = progressWidth
                binding.progressValue.layoutParams = layoutParams
            }

            // 设置操作按钮（根据HTML设计）
            updateButtonState(task, currentDistance)

            // 设置卡片背景状态
            updateCardBackground(task)

            // 设置点击事件
            binding.btnAction.setOnClickListener {
                if (task.completed && !task.claimed) {
                    onItemClickListener.onTaskClaimClick(task.id, task.stepGoal)
                } else if (!task.completed) {
                    // 提示用户继续步行
                    val remaining = task.stepGoal - currentDistance
                    // 这里可以显示提示或跳转到相关页面
                }
            }

            // 设置整个item的点击事件
            binding.taskItemContainer.setOnClickListener {
                if (task.completed && !task.claimed) {
                    onItemClickListener.onTaskClaimClick(task.id, task.stepGoal)
                }
            }
        }

        private fun updateButtonState(task: TaskModel, currentDistance: Int) {

            // 根据material.html中的三个卡片状态：
            // 1. 第一个卡片：已领取状态 (.t-card.done) - task.completed && task.claimed
            // 2. 第二个卡片：等待领取/高亮状态 (.t-card.highlight) - task.completed && !task.claimed 或 progress >= 0.8f
            // 3. 第三个卡片：挑战中/默认状态 (.t-card) - 其他情况

            if (task.claimed) {
                // 第一个卡片：已领取状态 (.t-card.done .btn)
                // 按钮文案: "已领取"
                binding.btnAction.text = itemView.context.getString(R.string.task_item_claimed)
                binding.btnAction.isEnabled = false
                // 按钮背景: #F1F5F9
                binding.btnAction.setBackgroundResource(R.drawable.button_background_disabled)
                // 按钮文字颜色: #94A3B8
                binding.btnAction.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                // 按钮阴影: none
                binding.btnAction.elevation = 0f
            } else if (task.completed) {
                // 第二个卡片：等待领取/高亮状态 (.t-card.highlight .btn)
                // 按钮文案: "领取奖励"
                binding.btnAction.text = itemView.context.getString(R.string.task_item_claim)
                binding.btnAction.isEnabled = true
                // 按钮背景: var(--md-theme-tertiaryGradient) (黄色渐变)
                binding.btnAction.setBackgroundResource(R.drawable.task_button_yellow_gradient)
                // 按钮文字颜色: #fff
                binding.btnAction.setTextColor(android.graphics.Color.WHITE)
                // 按钮阴影: 0 4px 10px rgba(255, 191, 71, 0.3)
                binding.btnAction.elevation = 4f
            } else {
                // 第三个卡片：挑战中/默认状态 (.t-card .btn) - 以html为绝对标准
                // 按钮文案: "加油"
                binding.btnAction.text = itemView.context.getString(R.string.task_locked_keep_going)

                // 根据html绝对标准，第三个卡片的按钮始终使用相同样式
                // 按钮背景: rgba(0,214,143,0.1) - 对应 task_button_primary_gradient.xml
                binding.btnAction.setBackgroundResource(R.drawable.task_button_primary_gradient)
                // 按钮文字颜色: var(--md-theme-primary) - 对应 R.color.md_theme_primary
                binding.btnAction.setTextColor(itemView.context.getColor(R.color.md_theme_primary))
                // 按钮阴影: 无
                binding.btnAction.elevation = 0f
                // 按钮可点击状态: 根据html中的 cursor: pointer，应该是可点击的
                binding.btnAction.isEnabled = true
            }
        }


        private fun formatStepCount(steps: Int): String {
            return if (steps >= 1000) {
                String.format("%,d", steps)
            } else {
                steps.toString()
            }
        }

        private fun updateCardBackground(task: TaskModel) {

            // 重置所有状态
            binding.taskItemContainer.isSelected = false
            binding.taskItemContainer.isActivated = false
            binding.taskItemContainer.isEnabled = true

            // 根据material.html中的三个卡片状态：
            // 1. 第一个卡片：已领取状态 (.t-card.done) - task.completed && task.claimed
            // 2. 第二个卡片：等待领取/高亮状态 (.t-card.highlight) - task.completed && !task.claimed 或 progress >= 0.8f
            // 3. 第三个卡片：挑战中/默认状态 (.t-card) - 其他情况

            if (task.claimed) {
                // 第一个卡片：已领取状态 (.t-card.done)
                binding.taskItemContainer.isActivated = true
                // 徽章背景: #D1FAE5, 文字颜色: #059669
                binding.tvTaskBadge.setBackgroundResource(R.drawable.task_badge_completed_background)
                // 进度条颜色: #10B981 (100%)
                binding.progressValue.setBackgroundResource(R.drawable.progress_value_completed_background)
                // 进度条轨道背景: #F1F5F9
                binding.progressTrack.setBackgroundResource(R.drawable.progress_track_background)
            } else if (task.completed) {
                // 第二个卡片：等待领取/高亮状态 (.t-card.highlight)
                binding.taskItemContainer.isSelected = true
                // 徽章背景: #FEF3C7, 文字颜色: #D97706
                binding.tvTaskBadge.setBackgroundResource(R.drawable.task_badge_highlight_background)
                // 进度条颜色: #F59E0B (80%)
                binding.progressValue.setBackgroundResource(R.drawable.progress_value_highlight_background)
                // 进度条轨道背景: #F1F5F9
                binding.progressTrack.setBackgroundResource(R.drawable.progress_track_background)
            } else {
                // 第三个卡片：挑战中/默认状态 (.t-card) - 以html为绝对标准
                // 卡片背景: #fff (纯白色) - 通过 task_card_background.xml 的默认状态实现
                // 徽章背景: var(--md-theme-surfaceContainer) - 对应 task_badge_background.xml
                binding.tvTaskBadge.setBackgroundResource(R.drawable.task_badge_background)
                // 徽章文字颜色: var(--text-sub) - 对应 R.color.md_theme_onSurfaceVariant

                // 进度条填充色: #CBD5E1 (根据html内联样式绝对标准)
                binding.progressValue.setBackgroundResource(R.drawable.progress_value_default_background)

                // 进度条轨道背景: #F1F5F9 (绝对标准)
                binding.progressTrack.setBackgroundResource(R.drawable.progress_track_background)
            }
        }

        private fun getTaskDescription(stepGoal: Int): String {
            // 根据步数目标获取任务描述（更简洁的文本，适合多语言）
            return when {
                stepGoal <= 100 -> itemView.context.getString(R.string.task_desc_100)
                stepGoal <= 500 -> itemView.context.getString(R.string.task_desc_500)
                stepGoal <= 1000 -> itemView.context.getString(R.string.task_desc_1000)
                stepGoal <= 2000 -> itemView.context.getString(R.string.task_desc_2000)
                stepGoal <= 3000 -> itemView.context.getString(R.string.task_desc_3000)
                stepGoal <= 4000 -> itemView.context.getString(R.string.task_desc_4000)
                stepGoal <= 5000 -> itemView.context.getString(R.string.task_desc_5000)
                stepGoal <= 7000 -> itemView.context.getString(R.string.task_desc_7000)
                else -> itemView.context.getString(R.string.task_desc_else)
            }
        }

    }
}
