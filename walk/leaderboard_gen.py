import json
import random
from typing import List, Dict, Any
import os

# 定义国家列表
COUNTRIES = [
   "United States","Brazil","Indonesia",
]

# 定义昵称风格列表（用于生成随机昵称）
NICKNAME_STYLES = [
    "Anonymous", "User_", "Gamer", "Walker", "Step", "Fit", "Health", 
    "Run", "Active", "Move", "Tracker", "Pace", "Miles", "Journey", 
    "Path", "_Fan", "Star", "Pro", "Master", "Champion", "King", "Queen",
    "Ninja", "Warrior", "Hero", "Legend", "Beast", "Machine", "Lover", "Addict"
]

# 用户性格特征（决定数据生成方式）
USER_TRAITS = [
    "daily_active",      # 每天都活跃，短期表现好
    "weekend_active",    # 周末活跃，7天表现稳定
    "monthly_consistent", # 月活跃，长期表现稳定
    "sporadic",          # 偶尔活跃，表现不稳定
    "declining",         # 活跃度下降
    "increasing",        # 活跃度上升
    "stable"             # 稳定表现
]

def generate_avatar_url(user_id: int) -> str:
    """生成用户头像URL"""
    return f"https://api.dicebear.com/7.x/avataaars/png?seed=d941b7350bf70a{user_id}ad3aec9cb9f779d4e4"

def generate_nickname(user_id: int) -> str:
    """生成随机昵称"""
    if random.random() < 0.1:  # 10%的概率使用Anonymous
        return "Anonymous"
        
    if random.random() < 0.4:  # 40%的概率使用带数字的昵称
        nickname_base = random.choice(NICKNAME_STYLES)
        if nickname_base == "Anonymous":
            nickname_base = random.choice([s for s in NICKNAME_STYLES if s != "Anonymous"])
        
        # 生成2-4位数字后缀
        number = random.randint(10, 9999)
        return f"{nickname_base}{number}"
    
    # 50%的概率使用组合昵称
    prefix = random.choice(["", "The", "Mr", "Ms", "Dr", "Sir", "Lady", "Cool", "Super", "Mega", "Ultra"])
    middle = random.choice(NICKNAME_STYLES)
    if middle == "Anonymous":
        middle = random.choice([s for s in NICKNAME_STYLES if s != "Anonymous"])
    
    suffix = random.choice(["", "Pro", "Master", "99", "X", "23", "Jr", "Sr", "_official"])
    
    nickname = middle
    if prefix and random.random() < 0.7:
        nickname = f"{prefix}{middle}"
    if suffix and random.random() < 0.6:
        nickname = f"{nickname}{suffix}"
    
    return nickname.replace(" ", "")

def create_user_base(count: int = 150) -> List[Dict[str, Any]]:
    """创建基础用户池"""
    users = []
    for i in range(1, count + 1):
        # 基础活跃度（1-100）
        base_activity = random.randint(30, 100)
        
        # 用户特性
        trait = random.choice(USER_TRAITS)
        
        # 波动系数（0.1-0.5）
        fluctuation = random.uniform(0.1, 0.5)
        
        users.append({
            "id": str(i),
            "nickname": generate_nickname(i),
            "avatarUrl": generate_avatar_url(i),
            "country": random.choice(COUNTRIES),
            "base_activity": base_activity,
            "trait": trait,
            "fluctuation": fluctuation
        })
    
    return users

def generate_coins_for_day(user: Dict[str, Any]) -> int:
    """生成用户单日金币数量"""
    base = user["base_activity"] * 300  # 基础金币
    
    # 根据特性调整
    if user["trait"] == "daily_active":
        base *= 1.2
    elif user["trait"] == "weekend_active" and random.random() < 0.3:  # 周末活跃用户有30%几率爆发
        base *= 1.5
    elif user["trait"] == "sporadic" and random.random() < 0.2:  # 偶尔活跃用户有20%几率爆发
        base *= 2.0
    elif user["trait"] == "declining":
        base *= 0.8
    
    # 添加随机波动
    fluctuation = 1.0 + (random.random() * 2 - 1) * user["fluctuation"]
    
    # 最终金币数量
    coins = int(base * fluctuation)
    return max(coins, 1000)  # 确保最少有1000金币

def generate_coins_for_week(user: Dict[str, Any], day_coins: int) -> int:
    """根据用户特性和单日金币生成周金币数量"""
    # 基础周金币（大约是日金币的4-7倍）
    multiplier = random.uniform(4, 7)
    
    # 根据特性调整
    if user["trait"] == "weekend_active":
        multiplier *= 1.3
    elif user["trait"] == "daily_active":
        multiplier *= 1.1
    elif user["trait"] == "declining":
        multiplier *= 0.9
    elif user["trait"] == "increasing":
        multiplier *= 1.2
    
    # 最终周金币
    return int(day_coins * multiplier)

def generate_coins_for_month(user: Dict[str, Any], week_coins: int) -> int:
    """根据用户特性和周金币生成月金币数量"""
    # 基础月金币（大约是周金币的3-4倍）
    multiplier = random.uniform(3, 4)
    
    # 根据特性调整
    if user["trait"] == "monthly_consistent":
        multiplier *= 1.3
    elif user["trait"] == "stable":
        multiplier *= 1.2
    elif user["trait"] == "declining":
        multiplier *= 0.8
    elif user["trait"] == "increasing":
        multiplier *= 1.4
    
    # 最终月金币
    return int(week_coins * multiplier)

def generate_user_pool(user_count: int = 150) -> List[Dict[str, Any]]:
    """生成单一的用户池数据，包含所有用户及其在不同时间段的金币数据"""
    # 创建用户基础池
    users = create_user_base(user_count)
    
    # 生成每个用户在不同时间段的金币数据
    user_pool = []
    for user in users:
        day_coins = generate_coins_for_day(user)
        week_coins = generate_coins_for_week(user, day_coins)
        month_coins = generate_coins_for_month(user, week_coins)
        
        user_pool.append({
            "userId": user["id"],
            "nickname": user["nickname"],
            "avatarUrl": user["avatarUrl"],
            "country": user["country"],
            "coinsYesterday": day_coins,
            "coins7Days": week_coins,
            "coins30Days": month_coins
        })
    
    return user_pool

def save_user_pool(user_pool: List[Dict[str, Any]], output_dir: str, filename: str = "user_pool.json"):
    """保存用户池数据到JSON文件"""
    os.makedirs(output_dir, exist_ok=True)
    
    # 保存用户池数据
    with open(os.path.join(output_dir, filename), "w", encoding="utf-8") as f:
        json.dump(user_pool, f, indent=2, ensure_ascii=False)

def main():
    # 生成用户池数据
    user_pool = generate_user_pool(150)
    
    # 保存到文件
    save_user_pool(user_pool, "src/main/assets")
    
    print("用户池数据生成完成！文件保存在app/src/main/assets/user_pool.json")

if __name__ == "__main__":
    main()
