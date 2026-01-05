package com.ur.apps.walk.step.constants;

public class StepConstants {
    // 日期格式常量
    public static final String DATE_FORMAT_FULL = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    
    // 通知相关常量
    public static final int NOTIFICATION_ID_STEP = 100;
    public static final int NOTIFICATION_ID_REMIND = 200;
    
    // 步数存储相关常量
    public static final int DEFAULT_SAVE_INTERVAL_MS = 30 * 1000; // 30秒
    public static final int SCREEN_OFF_SAVE_INTERVAL_MS = 60 * 1000; // 60秒
    
    // SharedPreferences 相关常量
    public static final String PREF_NAME = "share_date";
    public static final String PREF_KEY_ACHIEVE_TIME = "achieveTime";
    public static final String PREF_KEY_PLAN_WALK = "planWalk_QTY";
    public static final String PREF_KEY_REMIND = "remind";
    
    // 默认值
    public static final String DEFAULT_ACHIEVE_TIME = "21:00";
    public static final String DEFAULT_PLAN_WALK = "7000";
    public static final String DEFAULT_REMIND = "1";
    public static final String MIDNIGHT_TIME = "00:00";
    
    // Android API版本常量
    public static final int MIN_STEP_COUNTER_API_LEVEL = 19; // Android 4.4
    
    // 通知文本常量
    public static final String NOTIFICATION_STEP_TEXT = "今日步数%d步";
    public static final String NOTIFICATION_REMIND_TEXT = "距离目标还差%d步，加油！";
    public static final int STEP_PERMISSON_CPDE = 1001;
    public static final int NOTIFICATION_PERMISSION_CODE = 100002;

    private StepConstants() {
        // 私有构造函数防止实例化
    }
}