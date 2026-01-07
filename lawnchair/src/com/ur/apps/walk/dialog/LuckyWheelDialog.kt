package com.ur.apps.walk.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.ur.apps.utils.URLog
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.android.launcher3.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random
import kotlin.math.round

private const val TAG = "LuckyWheelDialog"

class LuckyWheelDialog(
    context: Context,
    private val onWheelFinished: (Dialog) -> Unit
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

    }


    private lateinit var wheelImageView: ImageView
    private lateinit var wheelPointerImageView: ImageView
    private lateinit var pointerFrame: FrameLayout
    private lateinit var performStart: TextView
    private lateinit var titleView: ImageView
    private lateinit var frameContainer: FrameLayout
    private var isSpinning = false
    private lateinit var fakeDisplayList: TextView

    // 奖励列表（转盘上每个区域对应的奖励）
    private val rewards = listOf(1, 5, 10, 1, 5, 10)

    // 宝箱扇区索引 (从0开始，所以1、3、5扇区对应索引为0、2、4)
    private val chestSectionIndices = listOf(0, 2, 4)

    // 随机生成器
    private val random = Random()

    // 用于动画状态跟踪的Handler
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        URLog.d(TAG, "onCreate: 开始创建对话框")
        setContentView(R.layout.dialog_lucky_wheel)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        initViews()
        URLog.d(TAG, "onCreate: 对话框创建完成")
    }

    private fun initViews() {
        URLog.d(TAG, "initViews: 开始初始化视图")
        try {
            wheelImageView = findViewById(R.id.iv_wheel)
            URLog.d(TAG, "initViews: 找到轮盘视图 = ${wheelImageView != null}")

            wheelPointerImageView = findViewById(R.id.iv_wheel_pointer)
            URLog.d(TAG, "initViews: 找到指针视图 = ${wheelPointerImageView != null}")

            pointerFrame = findViewById(R.id.pointer_frame)
            URLog.d(TAG, "initViews: 找到指针架视图 = ${pointerFrame != null}")

            performStart = findViewById(R.id.perform_start)
            URLog.d(TAG, "initViews: 找到按钮视图 = ${performStart != null}")

            titleView = findViewById(R.id.tv_lucky_wheel_title)
            URLog.d(TAG, "initViews: 找到标题视图 = ${titleView != null}")

            frameContainer = findViewById(R.id.frame_wheel_container)
            URLog.d(TAG, "initViews: 找到容器视图 = ${frameContainer != null}")

            fakeDisplayList = findViewById(R.id.fake_display_list)

            // 确保指针正确显示
            wheelPointerImageView.visibility = View.VISIBLE

            // 确保指针位于最上层
            pointerFrame.bringToFront()

            // 设置初始旋转角度为0确保初始状态正确
            wheelImageView.rotation = 0f
            URLog.d(TAG, "initViews: 设置初始轮盘旋转角度 = 0")

            // 延迟检查视图尺寸
            handler.postDelayed({
                checkViewDimensions()
            }, 300)

            // 设置随机用户中奖列表
            setupFakeWinningList()

            performStart.setOnClickListener {
                URLog.d(TAG, "initViews: 点击了旋转按钮，isSpinning = $isSpinning")
                if (!isSpinning) {
                    spinWheel()
                }
            }
            URLog.d(TAG, "initViews: 视图初始化完成")
        } catch (e: Exception) {
            URLog.e(TAG, "initViews: 初始化视图异常", e)
        }
    }

    /**
     * 检查视图尺寸，确保轮盘正确显示
     */
    private fun checkViewDimensions() {
        try {
            URLog.d(
                TAG,
                "checkViewDimensions: 轮盘视图尺寸 - 宽: ${wheelImageView.width}, 高: ${wheelImageView.height}"
            )
            URLog.d(
                TAG,
                "checkViewDimensions: 指针视图尺寸 - 宽: ${wheelPointerImageView.width}, 高: ${wheelPointerImageView.height}"
            )
            URLog.d(
                TAG,
                "checkViewDimensions: 容器视图尺寸 - 宽: ${frameContainer.width}, 高: ${frameContainer.height}"
            )
            URLog.d(
                TAG,
                "checkViewDimensions: 轮盘视图是否可见: ${wheelImageView.visibility == View.VISIBLE}"
            )
            URLog.d(
                TAG,
                "checkViewDimensions: 指针视图是否可见: ${wheelPointerImageView.visibility == View.VISIBLE}"
            )

            // 如果视图还没有完成测量，再次延迟检查
            if (wheelImageView.width == 0) {
                handler.postDelayed({
                    checkViewDimensions()
                }, 300)
            }
        } catch (e: Exception) {
            URLog.e(TAG, "checkViewDimensions: 检查视图尺寸异常", e)
        }
    }

    private fun spinWheel() {
        URLog.d(TAG, "spinWheel: 开始旋转轮盘流程")
        isSpinning = true
        performStart.isEnabled = false
        URLog.d(TAG, "spinWheel: 禁用旋转按钮")

        // 输出扇区和宝箱扇区分布情况
        URLog.d(TAG, "spinWheel: 扇区奖励分布情况: ${rewards.joinToString()}")
        URLog.d(TAG, "spinWheel: 宝箱扇区索引: ${chestSectionIndices.joinToString()}")

        // 从宝箱扇区索引中随机选择一个
        val targetChestIndex = random.nextInt(chestSectionIndices.size)
        val rewardIndex = chestSectionIndices[targetChestIndex]

        URLog.d(TAG, "spinWheel: 随机选择宝箱索引 = $targetChestIndex (扇区索引 = $rewardIndex)")

        // 计算旋转角度
        val sectionAngle = 360f / rewards.size // 每个扇区的角度

        // 计算扇区中心角度
        // 因为上方指针为0度，所以需要计算每个扇区中心相对于顶部的角度
        val sectionCenterAngle = (rewardIndex * sectionAngle) + (sectionAngle / 2)

        // 额外旋转3圈 (1080度) 再加上扇区中心角度
        val extraRotation = 360f * 3

        // 最终旋转角度 = 额外旋转 + 目标扇区中心角度
        // 注意：由于转盘顺时针旋转，但我们希望指针指向奖励，所以最终角度需要取反
        // 保证结果精确到小数点后1位，避免精度误差导致停在分割线上

        // 视觉校正：添加微小偏移，确保指针视觉上正确指向扇区中心
        // 根据观察，调整偏移量为15度，使指针正确指向扇区中心
        val visualOffset = 15.0f
        val finalRotation = (extraRotation + (360 - sectionCenterAngle) + visualOffset).let {
            // 将角度四舍五入到一位小数，确保精确度
            round(it * 10) / 10f
        }

        // 添加详细的角度计算日志
        URLog.d(TAG, "spinWheel: 详细角度计算:")
        URLog.d(TAG, "spinWheel: - 扇区总数: ${rewards.size}")
        URLog.d(TAG, "spinWheel: - 每个扇区角度: $sectionAngle°")
        URLog.d(TAG, "spinWheel: - 扇区 $rewardIndex 的中心角度: $sectionCenterAngle°")
        URLog.d(TAG, "spinWheel: - 额外旋转: $extraRotation°")
        URLog.d(TAG, "spinWheel: - 视觉偏移调整: $visualOffset°")
        URLog.d(
            TAG,
            "spinWheel: - 最终旋转角度: $finalRotation° = $extraRotation + (360 - $sectionCenterAngle) + $visualOffset"
        )
        URLog.d(TAG, "spinWheel: - 预期停止位置(标准化角度): ${finalRotation % 360}°")

        URLog.d(
            TAG,
            "spinWheel: 旋转参数计算完成 - 扇区角度=$sectionAngle, 选中宝箱扇区=$rewardIndex, " +
                    "扇区中心角度=$sectionCenterAngle, 最终角度=$finalRotation"
        )

        try {
            // 确保轮盘视图可见
            wheelImageView.visibility = View.VISIBLE

            // 确保指针位于最上层
            pointerFrame.bringToFront()

            // 检查轮盘当前旋转角度
            URLog.d(TAG, "spinWheel: 开始旋转前轮盘角度 = ${wheelImageView.rotation}")

            // 创建旋转动画
            val valueAnimator = ValueAnimator.ofFloat(0f, finalRotation)
            valueAnimator.duration = 3000 // 动画持续3秒
            valueAnimator.interpolator = AccelerateDecelerateInterpolator() // 先加速后减速的插值器
            URLog.d(
                TAG,
                "spinWheel: 创建ValueAnimator对象，设置持续时间3000ms，设置插值器AccelerateDecelerateInterpolator"
            )

            valueAnimator.addUpdateListener { animation ->
                try {
                    val value = animation.animatedValue as Float
                    wheelImageView.rotation = value
                    // 记录每一帧的旋转角度，但仅记录转盘每完整旋转一圈(360度)的时刻
                    if (value.toInt() % 360 == 0) {
                        URLog.d(TAG, "spinWheel: 动画更新 - 当前旋转角度 = $value")
                    }

                    // 记录动画最后几帧的角度变化，帮助诊断最终停止位置
                    val animatedFraction = animation.animatedFraction
                    if (animatedFraction > 0.95) {
                        URLog.d(
                            TAG,
                            "spinWheel: 动画接近结束 - 当前旋转角度=$value, 标准化角度=${value % 360}, 动画完成度=$animatedFraction"
                        )
                    }
                } catch (e: Exception) {
                    URLog.e(TAG, "spinWheel: 动画更新异常", e)
                }
            }

            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    URLog.d(TAG, "onAnimationStart: 轮盘旋转动画开始")
                }

                override fun onAnimationEnd(animation: Animator) {
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 轮盘旋转动画结束，最终旋转角度=${wheelImageView.rotation}"
                    )
                    isSpinning = false

                    // 打印最终停止时指向哪个扇区
                    val finalAngle = wheelImageView.rotation % 360
                    // 详细记录角度计算过程
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 最终角度计算 - 总角度=${wheelImageView.rotation}, 标准化角度=$finalAngle"
                    )
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 扇区边界角度: 0°, 60°, 120°, 180°, 240°, 300°, 360°"
                    )

                    // 考虑视觉偏移，计算实际指向的扇区
                    // 要与spinWheel方法中的visualOffset保持一致
                    val visualOffset = 15.0f
                    val adjustedAngle = (finalAngle - visualOffset + 360) % 360
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 应用视觉偏移后的角度=$adjustedAngle (原始角度$finalAngle - 偏移$visualOffset)"
                    )

                    // 使用调整后的角度计算扇区索引
                    val sectionIndex = ((360 - adjustedAngle) / sectionAngle).toInt() % rewards.size
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 计算得到的扇区索引=$sectionIndex (计算公式: ((360 - $adjustedAngle) / $sectionAngle).toInt() % ${rewards.size})"
                    )
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 最终指向扇区索引=$sectionIndex, 奖励=${rewards[sectionIndex]}"
                    )

                    // 添加更详细的日志，显示是否停在宝箱扇区
                    val isChestSection = chestSectionIndices.contains(sectionIndex)
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 最终停在扇区 $sectionIndex，是否为宝箱扇区: $isChestSection"
                    )
                    URLog.d(
                        TAG,
                        "onAnimationEnd: 预选中的宝箱扇区索引: $rewardIndex，对应奖励: ${rewards[rewardIndex]}"
                    )

                    // 显示结果对话框 - 使用扇区对应的奖励
                    showResultDialog(rewards[rewardIndex], rewardIndex)
                }

                override fun onAnimationCancel(animation: Animator) {
                    URLog.d(TAG, "onAnimationCancel: 轮盘旋转动画被取消")
                }
            })

            URLog.d(TAG, "spinWheel: 开始执行轮盘旋转动画")
            valueAnimator.start()
            URLog.d(TAG, "spinWheel: 动画已启动")
        } catch (e: Exception) {
            URLog.e(TAG, "spinWheel: 执行动画异常", e)
            isSpinning = false
            performStart.isEnabled = true
        }
    }

    private fun showResultDialog(reward: Int, sectionIndex: Int) {
        onWheelFinished(this)
    }

    private fun setupFakeWinningList() {
        val builder = SpannableStringBuilder()

        // 添加标题行
        val titleRow = createTitleRow()
        builder.append(titleRow)
        builder.append("\n")

        // 添加内容行
        val fakeUsers = generateFakeWinningRecords(15)
        for (user in fakeUsers) {
            builder.append(user)
            builder.append("\n")
        }

        fakeDisplayList.text = builder
    }

    private fun createTitleRow(): SpannableString {
        val titleText = "Time      Info             Amount  Type     T-ID"
        val spannableString = SpannableString(titleText)

        // 设置整个标题行为橙色
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#FF9800")), // 橙色
            0,
            titleText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    private fun generateFakeWinningRecords(count: Int): List<SpannableString> {
        val records = mutableListOf<SpannableString>()
        val random = Random()
        val cal = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // 支付类型列表
        val paymentTypes = listOf("Paypal", "DANA", "PayBank")

        // 假邮箱前缀列表
        val emailPrefixes = listOf(
            "car", "eri", "sha", "mic", "joy", "tom", "sam", "lex", "ana", "ben",
            "max", "eva", "leo", "kim", "joe", "tim", "jen", "roy", "amy", "dan"
        )

        // 假邮箱后缀列表
        val emailSuffixes = listOf(
            "e", "s", "k", "9", "a", "b", "c", "d", "x", "y",
            "z", "m", "n", "p", "r", "t", "v", "w", "g", "h"
        )

        // 邮箱域名列表
        val emailDomains = listOf("gmail.com", "outlook.com", "yahoo.com", "163.com", "qq.com")

        // 生成记录
        for (i in 0 until count) {
            cal.add(Calendar.MINUTE, -random.nextInt(3))
            val time = timeFormat.format(cal.time)

            // 生成随机邮箱（部分隐藏）
            val emailPrefix = emailPrefixes[random.nextInt(emailPrefixes.size)]
            val emailSuffix = emailSuffixes[random.nextInt(emailSuffixes.size)]
            val emailDomain = emailDomains[random.nextInt(emailDomains.size)]
            val email = "${emailPrefix}**${emailSuffix}@${emailDomain}"

            // 生成随机金额（$0.01 - $5.00）
            val amount = if (random.nextInt(10) > 7) {
                String.format("$%.2f", (random.nextInt(5) + 1).toFloat())
            } else {
                String.format("$%.2f", (random.nextInt(99) + 1).toFloat() / 100)
            }

            // 生成随机支付类型
            val paymentType = paymentTypes[random.nextInt(paymentTypes.size)]

            // 生成随机交易ID
            val prefix =
                if (paymentType == "Paypal") "P" else if (paymentType == "DANA") "D" else "T"
            val transactionId = "$prefix${20}****${random.nextInt(900) + 100}"

            // 构建记录字符串
            val record = "$time $email $amount $paymentType $transactionId"
            val spannableString = SpannableString(record)

            // 设置金额文本颜色为绿色（可选）
            val startAmount = record.indexOf('$')
            val endAmount = record.indexOf(' ', startAmount)
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#4CAF50")),
                startAmount,
                endAmount,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            records.add(spannableString)
        }

        return records
    }
}
