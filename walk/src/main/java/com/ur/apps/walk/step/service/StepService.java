package com.ur.apps.walk.step.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.ur.apps.walk.BuildConfig;
import com.ur.apps.analysis.td.TDAnalyticsManager;
import com.ur.apps.bubble.BubbleWindowManager;
import com.ur.apps.utils.URLog;
import com.ur.apps.walk.MainActivityRecycler;
import com.ur.apps.walk.R;
import com.ur.apps.walk.constants.StatisticConstants;
import com.ur.apps.walk.step.accelerometer.StepCount;
import com.ur.apps.walk.step.accelerometer.StepValuePassListener;
import com.ur.apps.walk.step.bean.ExerciseStats;
import com.ur.apps.walk.step.bean.StepData;
import com.ur.apps.walk.step.callback.StepCountChangeCallBack;
import com.ur.apps.walk.step.constants.NotificationConstants;
import com.ur.apps.walk.step.constants.StepConstants;
import com.ur.apps.walk.step.manager.LockScreenManager;
import com.ur.apps.walk.step.utils.DbUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


public class StepService extends Service implements SensorEventListener {

    private String TAG = "StepService";
    private static final boolean DBG = true; // 调试标志

    public static final String HITED_COUNT = "hited_count";
    public static final String HITED_COUNT_KEY = "hited_count_key";
    /**
     * 默认为30秒进行一次存储
     */
    private static int duration = StepConstants.DEFAULT_SAVE_INTERVAL_MS;
    /**
     * 当前的日期
     */
    private static String CURRENT_DATE = "";
    /**
     * 传感器管理对象
     */
    private SensorManager sensorManager;
    /**
     * 广播接受者
     */
    private BroadcastReceiver mBatInfoReceiver;
    /**
     * 保存记步计时器
     */
    private TimeCount time;
    /**
     * 当前所走的步数
     */
    private int CURRENT_STEP;
    /**
     * 当前运动距离（米）
     */
    private double CURRENT_DISTANCE;
    /**
     * 当前消耗卡路里（千卡）
     */
    private double CURRENT_CALORIES;
    /**
     * 当前运动时长（分钟）
     */
    private int CURRENT_DURATION;
    /**
     * 开始运动的时间
     */
    private long startTime;
    /**
     * 计步传感器类型  Sensor.TYPE_STEP_COUNTER或者Sensor.TYPE_STEP_DETECTOR
     */
    private static int stepSensorType = -1;
    /**
     * 每次第一次启动记步服务时是否从系统中获取了已有的步数记录
     */
    private boolean hasRecord = false;
    /**
     * 系统中获取到的已有的步数
     */
    private int hasStepCount = 0;
    /**
     * 上一次的步数
     */
    private int previousStepCount = 0;
    /**
     * 通知管理对象
     */
    private NotificationManager mNotificationManager;
    /**
     * 加速度传感器中获取的步数
     */
    private StepCount mStepCount;
    /**
     * IBinder对象，向Activity传递数据的桥梁
     */
    private StepBinder stepBinder = new StepBinder();
    /**
     * 通知构建者
     */
    private NotificationCompat.Builder mBuilder;
    private int notifyId_Step = NotificationConstants.NOTIFICATION_ID_STEP;

    /**
     * 锁屏管理器
     */
    private LockScreenManager mLockScreenManager;

    private BubbleWindowManager mBubbleWindowManager;

    @Override
    public void onCreate() {
        super.onCreate();
        URLog.d(TAG, "onCreate()");
        // 初始化锁屏管理器
        mLockScreenManager = new LockScreenManager(this);
        mBubbleWindowManager = new BubbleWindowManager(this);
        // 先初始化其他数据，但不立即启动前台服务
        CURRENT_DATE = getTodayDate();
        startTime = System.currentTimeMillis();
        DbUtils.createDb(this);
        initBroadcastReceiver();
        createNotificationChannel();
        initTodayData();
        startStepDetector();
        startTimeCount();
    }

    /**
     * 获取当天日期
     *
     * @return
     */
    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(StepConstants.DATE_FORMAT_FULL);
        return sdf.format(date);
    }

    /**
     * 初始化通知栏
     */
    private void initNotification() {
        updateNotification();
        URLog.d(TAG, "initNotification()");
    }

    /**
     * 初始化当天的步数
     */
    private void initTodayData() {
        DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENT_DATE}, list -> {
            URLog.i(TAG, "initTodayData: today " + list.size());
            if (list.isEmpty()) {
                CURRENT_STEP = 0;
            } else if (list.size() == 1) {
                URLog.i(TAG, "StepData=" + list.get(0).toString());
                CURRENT_STEP = Integer.parseInt(list.get(0).getStep());
            } else {
                URLog.v(TAG, "出错了！");
            }
            if (mStepCount != null) {
                mStepCount.setSteps(CURRENT_STEP);
            }
            calculateStats();
            updateNotification();

            // 数据加载完成后尝试启动前台服务
            ensureForegroundService();

            updateCalBack();
        });
    }

    /**
     * 注册广播
     */
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
//        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出"关机"对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者"关机"对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //监听日期变化
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    URLog.d(TAG, "screen on");
                    JSONObject json = new JSONObject();
                    try {
                        json.put(StatisticConstants.SERVICE, "StepService");
                    } catch (JSONException ignored) {
                    }
                    TDAnalyticsManager.INSTANCE.reportTrackEvent(
                            StatisticConstants.SCREEN_ON, json
                    );
                    // 屏幕亮起时处理锁屏逻辑
                    if (mLockScreenManager != null) {
                        mLockScreenManager.onScreenOn();
                    }
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    URLog.d(TAG, "screen off");
                    JSONObject json = new JSONObject();
                    try {
                        json.put(StatisticConstants.SERVICE, "StepService");
                    } catch (JSONException ignored) {
                    }
                    TDAnalyticsManager.INSTANCE.reportTrackEvent(
                            StatisticConstants.SCREEN_OFF, json
                    );
                    // 屏幕关闭时处理锁屏逻辑
                    if (mLockScreenManager != null) {
                        mLockScreenManager.onScreenOff();
                    }
                    //改为60秒一存储
                    duration = StepConstants.SCREEN_OFF_SAVE_INTERVAL_MS;
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    URLog.d(TAG, "screen unlock");
                    JSONObject json = new JSONObject();
                    try {
                        json.put(StatisticConstants.SERVICE, "StepService");
                    } catch (JSONException ignored) {
                    }
                    TDAnalyticsManager.INSTANCE.reportTrackEvent(
                            StatisticConstants.SCREEN_UNLOCK, json
                    );
                    if (mLockScreenManager != null) {
                        mLockScreenManager.onUserPresent();
                    }
//                    save();
                    //改为30秒一存储
                    duration = StepConstants.DEFAULT_SAVE_INTERVAL_MS;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    URLog.i(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                    //保存一次
                    save();
                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    URLog.i(TAG, " receive ACTION_SHUTDOWN");
                    save();
                } else if (Intent.ACTION_DATE_CHANGED.equals(action)) {//日期变化步数重置为0
//                    Logger.d("重置步数" + StepDcretor.CURRENT_STEP);
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    //时间变化步数重置为0
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_TICK.equals(action)) {//日期变化步数重置为0
//                    Logger.d("重置步数" + StepDcretor.CURRENT_STEP);
                    save();
                    isNewDay();
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter, Context.RECEIVER_EXPORTED);
    }


    /**
     * 监听晚上0点变化初始化数据
     */
    private void isNewDay() {
        String time = StepConstants.MIDNIGHT_TIME;
        if (time.equals(new SimpleDateFormat("HH:mm").format(new Date())) || !CURRENT_DATE.equals(getTodayDate())) {
            initTodayData();
        }
    }


    /**
     * 开始保存记步数据
     */
    private void startTimeCount() {
        if (time == null) {
            time = new TimeCount(duration, 1000);
        }
        time.start();
    }

    /**
     * 更新步数通知
     */
    private void updateNotification() {
        if (mBuilder != null) {
            mBuilder.setContentText(getString(R.string.today_step_count, CURRENT_STEP));
            Notification notification = mBuilder.build();

            // 只更新通知，不触发前台服务
            try {
                mNotificationManager.notify(notifyId_Step, notification);
                if (DBG) URLog.d(TAG, "updateNotification()");
            } catch (Exception e) {
                URLog.e(TAG, "更新通知失败: " + e.getMessage());
            }
        }
    }

    public void updateCalBack() {
        calculateStats();
        int hundred_level = CURRENT_STEP / 100;
        if (CURRENT_STEP != 0 && hundred_level != 0) {
            //100的整数倍表示走了100步了
            URLog.i(TAG, "reach step each period");
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(HITED_COUNT, Context.MODE_PRIVATE);
            int hitLevel = preferences.getInt(HITED_COUNT_KEY, 0);
            if (hitLevel != hundred_level) {
                URLog.i(TAG, "callbackStepEachPeriod " + hitLevel);
                callbackStepEachPeriod(hundred_level);
                preferences.edit().putInt(HITED_COUNT_KEY, hundred_level).apply();
            } else {
                URLog.i(TAG, "callbackStepEachPeriod not reach");
            }
        } else if (CURRENT_STEP < 20) {
            //每天的20步重置
            URLog.i(TAG, "reset step count");
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(HITED_COUNT, Context.MODE_PRIVATE);
            preferences.edit().putInt(HITED_COUNT_KEY, 0).apply();
        }
        for (StepCountChangeCallBack stepCountChangeCallBack : mCallBackList) {
            ExerciseStats stats = new ExerciseStats(CURRENT_STEP, CURRENT_DISTANCE, CURRENT_CALORIES, CURRENT_DURATION);
            if (stepCountChangeCallBack != null) {
                stepCountChangeCallBack.onStepChange(stats);
                URLog.i(TAG, "update call back " + stats);
            }

        }
    }


    private List<StepCountChangeCallBack> mCallBackList = new CopyOnWriteArrayList<>();

    /**
     * 注册UI更新监听
     *
     * @param paramICallback
     */
    public void registerCallback(StepCountChangeCallBack paramICallback) {
        mCallBackList.add(paramICallback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return stepBinder;
    }

    /**
     * 向Activity传递数据的纽带
     */
    public class StepBinder extends Binder {

        /**
         * 获取当前service对象
         *
         * @return StepService
         */
        public StepService getService() {
            return StepService.this;
        }
    }

    /**
     * 获取当前步数
     *
     * @return
     */
    public int getStepCount() {
        return CURRENT_STEP;
    }

    public double getDistance() {
        return CURRENT_DISTANCE;
    }

    public double getCalories() {
        return CURRENT_CALORIES;
    }

    public long getDuration() {
        return CURRENT_DURATION;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        URLog.d(TAG, "onStartCommand()");
        // 在 onStartCommand 中尝试启动前台服务，可能多次调用
        ensureForegroundService();
        tryShowBubbleWindow();
        return START_STICKY;
    }

    private void tryShowBubbleWindow() {
        // 构造参数：创建一个 1像素的保活窗口
        Map<String, Object> params = new HashMap<>();
        // 设置宽高为 1像素 (自动装箱为 Integer)
        params.put("width", BuildConfig.DEBUG ?  10 : 1);
        params.put("height", BuildConfig.DEBUG ?  10 : 1);
        // 设置位置
        params.put("gravity", "smart_random");
        // 关键：不可触摸，不获取焦点
        // 使用 Arrays.asList 快速构建 List<String>
        params.put("layoutParamFlags", Arrays.asList(
                    "FLAG_NOT_FOCUSABLE"
        ));
        // 启动窗口
        mBubbleWindowManager.addOrUpdateWindow(params);
    }

    private void removeBubbleWindow() {
        mBubbleWindowManager.removeWindow();
    }

    /**
     * 获取传感器实例
     */
    public void startStepDetector() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (sensorManager != null) {
                    sensorManager = null;
                }
                // 检查是否有ACTIVITY_RECOGNITION权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(StepService.this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                    URLog.e(TAG, "Missing ACTIVITY_RECOGNITION permission");
                    return;
                }
                // 获取传感器管理器的实例
                sensorManager = (SensorManager) StepService.this.getSystemService(SENSOR_SERVICE);
                //android4.4以后可以使用计步传感器
                int VERSION_CODES = Build.VERSION.SDK_INT;
                if (VERSION_CODES >= StepConstants.MIN_STEP_COUNTER_API_LEVEL) {
                    addCountStepListener();
                } else {
                    addBasePedometerListener();
                }
            }
        }).start();

    }

    /**
     * 添加传感器监听
     * 1. TYPE_STEP_COUNTER API的解释说返回从开机被激活后统计的步数，当重启手机后该数据归零，
     * 该传感器是一个硬件传感器所以它是低功耗的。
     * 为了能持续的计步，请不要反注册事件，就算手机处于休眠状态它依然会计步。
     * 当激活的时候依然会上报步数。该sensor适合在长时间的计步需求。
     * <p>
     * 2.TYPE_STEP_DETECTOR翻译过来就是走路检测，
     * API文档也确实是这样说的，该sensor只用来监监测走步，每次返回数字1.0。
     * 如果需要长事件的计步请使用TYPE_STEP_COUNTER。
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            URLog.v(TAG, "Sensor.TYPE_STEP_COUNTER");
            sensorManager.registerListener(StepService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            URLog.v(TAG, "Sensor.TYPE_STEP_DETECTOR");
            sensorManager.registerListener(StepService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            URLog.v(TAG, "Count sensor not available!");
            addBasePedometerListener();
        }
    }

    /**
     * 传感器监听回调
     * 记步的关键代码
     * 1. TYPE_STEP_COUNTER API的解释说返回从开机被激活后统计的步数，当重启手机后该数据归零，
     * 该传感器是一个硬件传感器所以它是低功耗的。
     * 为了能持续的计步，请不要反注册事件，就算手机处于休眠状态它依然会计步。
     * 当激活的时候依然会上报步数。该sensor适合在长时间的计步需求。
     * <p>
     * 2.TYPE_STEP_DETECTOR翻译过来就是走路检测，
     * API文档也确实是这样说的，该sensor只用来监监测走步，每次返回数字1.0。
     * 如果需要长事件的计步请使用TYPE_STEP_COUNTER。
     *
     * @param
     */
    private void calculateStats() {
        // 假设每步平均步幅0.7米
        double stepLength = 0.7;
        // 计算距离（米）
        CURRENT_DISTANCE = CURRENT_STEP * stepLength / 1000;

        // 计算消耗的卡路里（根据距离和平均体重60kg计算）
        // 假设每公里消耗60千卡
        CURRENT_CALORIES = (CURRENT_DISTANCE) * 60;
        CURRENT_DISTANCE = Math.round(CURRENT_DISTANCE * 100.0) / 100.0;
        CURRENT_CALORIES = Math.round(CURRENT_CALORIES * 100.0) / 100.0;

        // 根据步数计算运动时长（分钟）
        CURRENT_DURATION = (int) ((CURRENT_STEP * 0.6) / 60);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (stepSensorType == Sensor.TYPE_STEP_COUNTER) {
            //获取当前传感器返回的临时步数
            int tempStep = (int) event.values[0];
            //首次如果没有获取手机系统中已有的步数则获取一次系统中APP还未开始记步的步数
            if (!hasRecord) {
                hasRecord = true;
                hasStepCount = tempStep;
            } else {
                //获取APP打开到现在的总步数=本次系统回调的总步数-APP打开之前已有的步数
                int thisStepCount = tempStep - hasStepCount;
                //本次有效步数=（APP打开后所记录的总步数-上一次APP打开后所记录的总步数）
                int thisStep = thisStepCount - previousStepCount;
                //总步数=现有的步数+本次有效步数
                CURRENT_STEP += (thisStep);
                //记录最后一次APP打开到现在的总步数
                previousStepCount = thisStepCount;
            }
            URLog.d(TAG, "tempStep" + tempStep);
        } else if (stepSensorType == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0) {
                CURRENT_STEP++;
            }
        }
        calculateStats();
        updateNotification();
        updateCalBack();
    }

    /**
     * 通过加速度传感器来记步
     */
    private void addBasePedometerListener() {
        mStepCount = new StepCount();
        mStepCount.setSteps(CURRENT_STEP);
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean isAvailable = sensorManager.registerListener(mStepCount.getStepDetector(), sensor, SensorManager.SENSOR_DELAY_UI);
        mStepCount.initListener(new StepValuePassListener() {
            @Override
            public void stepChanged(int steps) {
                CURRENT_STEP = steps;
                updateNotification();
                updateCalBack();
                calculateStats();
            }
        });
        if (isAvailable) {
            URLog.v(TAG, "加速度传感器可以使用");
        } else {
            URLog.v(TAG, "加速度传感器无法使用");
        }
    }

    private void callbackStepEachPeriod(int hundred_level) {
        URLog.i(TAG, "callbackStepEachPeriod");
        for (StepCountChangeCallBack stepCountChangeCallBack : mCallBackList) {
            if (stepCountChangeCallBack != null) {
                stepCountChangeCallBack.onStepReachPeriod(hundred_level);
                URLog.i(TAG, "update call back reach period ");
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    /**
     * 保存记步数据
     */
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            save();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

    }

    /**
     * 保存记步数据
     */
    private void save() {
        int tempStep = CURRENT_STEP;

        DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENT_DATE}, list -> {
            if (list.size() == 0 || list.isEmpty()) {
                StepData data = new StepData();
                data.setToday(CURRENT_DATE);
                data.setStep(tempStep + "");
                DbUtils.insert(data);
            } else if (list.size() == 1) {
                StepData data = list.get(0);
                data.setStep(tempStep + "");
                DbUtils.update(data);
            } else {
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        URLog.i(TAG, "onDestroy: service onDestroy");

        // 1. 取消计时器
        if (time != null) {
            time.cancel();
            time = null;
        }

        // 2. 取消传感器监听
        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(this);
                if (mStepCount != null && mStepCount.getStepDetector() != null) {
                    sensorManager.unregisterListener(mStepCount.getStepDetector());
                }
            } catch (Exception e) {
                URLog.e(TAG, "取消传感器监听失败: " + e.getMessage());
            }
        }

        // 3. 取消广播接收器
        try {
            unregisterReceiver(mBatInfoReceiver);
        } catch (Exception e) {
            // 忽略可能已经取消注册的异常
            URLog.w(TAG, "取消广播接收器失败（可能已取消）: " + e.getMessage());
        }

        // 4. 清理StepCount资源
        if (mStepCount != null) {
            try {
                mStepCount.clearListener();
            } catch (Exception e) {
                URLog.e(TAG, "清理StepCount资源失败: " + e.getMessage());
            }
        }

        // 5. 清理回调列表
        if (mCallBackList != null) {
            mCallBackList.clear();
        }

        // 6. 清理锁屏管理器
        if (mLockScreenManager != null) {
            try {
                mLockScreenManager.cleanup();
            } catch (Exception e) {
                URLog.e(TAG, "清理锁屏管理器失败: " + e.getMessage());
            }
        }

        // 7. 取消前台服务并移除通知
        try {
            stopForeground(true);
            if (mNotificationManager != null) {
                mNotificationManager.cancel(notifyId_Step);
            }
        } catch (Exception e) {
            URLog.e(TAG, "取消前台服务失败: " + e.getMessage());
        }

        // 8. 清理其他资源
        mBuilder = null;

        // 9. 确保服务完全停止
        stopSelf();

        removeBubbleWindow();

        URLog.i(TAG, "onDestroy: 服务资源清理完成");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * 创建通知渠道，在 Android 8.0+ 必需
     */
    private void createNotificationChannel() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 创建通知渠道（Android 8.0及以上版本需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NotificationConstants.CHANNEL_ID,
                    NotificationConstants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(NotificationConstants.CHANNEL_DESCRIPTION);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 确保服务以前台服务方式运行
     * 将在不同生命周期入口点调用
     */
    private void ensureForegroundService() {
        if (mBuilder == null) {
            // 创建通知
            Intent notificationIntent = new Intent(this, MainActivityRecycler.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            mBuilder = new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_ID)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getString(R.string.today_step_count, CURRENT_STEP))
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher);
        } else {
            // 更新通知内容
            mBuilder.setContentText(getString(R.string.today_step_count, CURRENT_STEP));
        }

        Notification notification = mBuilder.build();

        try {
            // 在 Android 12+ 上可能会抛出异常，需要安全处理
            URLog.d(TAG, "尝试启动前台服务...");
            startForeground(notifyId_Step, notification);
            URLog.d(TAG, "前台服务启动成功");
        } catch (Exception e) {
            URLog.e(TAG, "启动前台服务失败: " + e.getMessage());
            // 对于前台服务异常，我们可以尝试作为普通服务运行
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 通知用户需要手动启用权限
                try {
                    mNotificationManager.notify(
                            NotificationConstants.NOTIFICATION_ID_PERMISSION,
                            new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_ID)
                                    .setContentTitle("需要权限")
                                    .setContentText("请打开应用设置并允许后台运行和通知权限")
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .build()
                    );
                } catch (Exception notificationEx) {
                    URLog.e(TAG, "显示权限通知也失败: " + notificationEx.getMessage());
                }
            }
        }
    }

    /**
     * 安全地启动步数服务，适用于 Android 12 及以上版本
     *
     * @param context 上下文
     */
    public static void startServiceSafely(Context context) {
        Intent intent = new Intent(context, StepService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (S) 及以上版本，检查前台条件
            try {
                // 使用 startForegroundService 方法启动
                URLog.d("StepService", "准备使用 startForegroundService 启动服务");
                context.startForegroundService(intent);
                URLog.d("StepService", "使用 startForegroundService 启动服务成功");
            } catch (Exception e) {
                URLog.e("StepService", "启动前台服务失败: " + e.getMessage());
                try {
                    // 使用普通服务启动作为后备
                    context.startService(intent);
                    URLog.d("StepService", "使用 startService 启动服务");
                } catch (Exception e2) {
                    URLog.e("StepService", "使用 startService 也失败: " + e2.getMessage());
                }
            }
        } else {
            // Android 12 以下版本
            context.startService(intent);
            URLog.d("StepService", "使用 startService 启动服务");
        }
    }

}
