package com.ur.apps.walk

import android.os.Bundle
import com.ur.apps.utils.URLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.ur.apps.walk.databinding.ActivityLeaderboardBinding
import com.ur.apps.walk.databinding.FragmentLeaderboardBinding
import com.ur.apps.walk.databinding.ItemLeaderboardBinding
import com.ur.apps.walk.model.LeaderboardRepository
import com.ur.apps.walk.model.LeaderboardUser
import kotlinx.coroutines.launch

class LeaderboardActivity : BaseActivity() {
    private lateinit var binding: ActivityLeaderboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        URLog.d("LeaderboardActivity", "onCreate")
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
    }

    private fun setupToolbar() {
        binding.layoutToolbar.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = LeaderboardPagerAdapter(this)
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.leaderboard_yesterday)
                1 -> getString(R.string.leaderboard_last_7_days)
                else -> getString(R.string.leaderboard_last_30_days)
            }
        }.attach()
    }
}

class LeaderboardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return LeaderboardFragment.newInstance(position)
    }
}

class LeaderboardFragment : Fragment() {
    private val TAG = "LeaderboardFragment"
    private var position: Int = 0
    private lateinit var adapter: LeaderboardAdapter
    private lateinit var binding: FragmentLeaderboardBinding
    private lateinit var repository: LeaderboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = arguments?.getInt(ARG_POSITION) ?: 0
        URLog.d(TAG, "onCreate: position=$position")
        repository = LeaderboardRepository(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        URLog.d(TAG, "onCreateView: position=$position")
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LeaderboardAdapter().also { this@LeaderboardFragment.adapter = it }
        }

        // 从数据库加载数据
        loadDataFromDatabase()

        return binding.root
    }

    private fun loadDataFromDatabase() {
        URLog.d(TAG, "开始加载数据: position=$position")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                URLog.d(TAG, "从数据库加载排行榜数据，类型: $position")
                val users = repository.getLeaderboardData(position)
                URLog.d(TAG, "获取到 ${users.size} 条数据，正在更新UI")
                
                if (users.isEmpty()) {
                    URLog.w(TAG, "警告：获取到的数据为空！")
                    binding.tvNoData.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    URLog.d(TAG, "数据示例: ${users.take(2)}")
                    binding.tvNoData.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(users)
                }
            } catch (e: Exception) {
                URLog.e(TAG, "从数据库加载数据出错", e)
                e.printStackTrace()
                binding.tvNoData.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): LeaderboardFragment {
            return LeaderboardFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POSITION, position)
                }
            }
        }
    }
}

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
    private val TAG = "LeaderboardAdapter"
    private var users: List<LeaderboardUser> = emptyList()

    fun submitList(newUsers: List<LeaderboardUser>) {
        URLog.d(TAG, "提交 ${newUsers.size} 条数据到适配器")
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    class ViewHolder(private val binding: ItemLeaderboardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: LeaderboardUser) {
            binding.apply {
                tvRank.text = user.rank.toString()
                tvNickname.text = user.nickname
                tvCountry.text = user.country
                tvCoins.text = user.coins.toString()

                Glide.with(ivAvatar)
                    .load(user.avatarUrl)
                    .circleCrop()
                    .into(ivAvatar)
            }
        }
    }
} 