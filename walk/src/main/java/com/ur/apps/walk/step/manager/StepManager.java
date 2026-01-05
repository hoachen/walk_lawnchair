package com.ur.apps.walk.step.manager;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import com.ur.apps.utils.URLog;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ur.apps.walk.step.bean.ExerciseStats;
import com.ur.apps.walk.step.bean.StepData;
import com.ur.apps.walk.step.callback.StepCountChangeCallBack;
import com.ur.apps.walk.step.utils.DbUtils;
import com.ur.apps.walk.step.service.StepService;
import com.ur.apps.walk.step.constants.StepConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StepManager {
    private static final String TAG = "StepManager";
    private static volatile StepManager instance;
    private Context context;
    private StepService stepService;
    private boolean isBound = false;
    private StepCountChangeCallBack stepCallback;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepService.StepBinder binder = (StepService.StepBinder) service;
            stepService = binder.getService();
            isBound = true;
            if (stepCallback != null) {
                stepService.registerCallback(stepCallback);
                stepService.updateCalBack();
            }
            URLog.i(TAG, "onServiceConnected: ");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stepService = null;
            isBound = false;
            URLog.i(TAG, "onServiceDisconnected: " + name);
        }
    };

    private StepManager(Context context) {
        this.context = context.getApplicationContext();
        bindService();
    }

    public static StepManager getInstance(Context context) {
        URLog.i(TAG, "getInstance: " + instance);
        if (instance == null) {
            URLog.i(TAG, "getInstance: " + instance);
            synchronized (StepManager.class) {
                URLog.i(TAG, "getInstance " + instance);
                if (instance == null) {
                    instance = new StepManager(context);
                }
            }
        }
        return instance;
    }

    private void bindService() {
        URLog.i(TAG, "bindService: bindService");
        Intent intent = new Intent(context, StepService.class);
        boolean bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        URLog.i(TAG, "bindService result: " + bound);
    }

    public void unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }
    }

    // 获取今日步数
    public ExerciseStats getTodayStats() {
        if (isBound && stepService != null) {
            int steps = stepService.getStepCount();
            double distance = stepService.getDistance();
            double calories = stepService.getCalories();
            long duration = stepService.getDuration();

            ExerciseStats stats = new ExerciseStats(steps, distance, calories, duration);
            return stats;
        }
        return new ExerciseStats(0, 0.0, 0.0, 0);
    }

    public int getTodaySteps() {
        return getTodayStats().getSteps();
    }

    public double getTodayDistance() {
        return getTodayStats().getDistance();
    }

    // 检查是否有ACTIVITY_RECOGNITION权限
    public boolean checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // 请求ACTIVITY_RECOGNITION权限
    public void requestActivityRecognitionPermission(android.app.Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, StepConstants.STEP_PERMISSON_CPDE);
        }
    }

    // 重启计步器
    public void restartStepDetector() {
        if (isBound && stepService != null) {
            stepService.startStepDetector();
        }
    }

    public void requestUpdateCallBack(){
        if (isBound && stepService != null) {
            stepService.updateCalBack();
        }
    }

    public double getTodayCalories() {
        return getTodayStats().getCalories();
    }

    public long getTodayDuration() {
        return getTodayStats().getDuration();
    }

    public void registerCallback(StepCountChangeCallBack callback) {
        this.stepCallback = callback;
        if (isBound && stepService != null) {
            stepService.registerCallback(callback);
        }
    }

    public void unregisterCallback(StepCountChangeCallBack callback) {
        if (isBound && stepService != null) {
            stepService.registerCallback(callback);
        }
    }

    // 获取指定天数的平均步数
    private double getAverageSteps(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days + 1); // +1 是为了包含今天

        SimpleDateFormat sdf = new SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault());
        String startDate = sdf.format(calendar.getTime());

        List<StepData> stepDataList = DbUtils.getQueryAll(StepData.class);
        if (stepDataList.isEmpty()) {
            return 0.0;
        }

        int totalSteps = 0;
        int validDays = 0;

        for (StepData stepData : stepDataList) {
            if (stepData.getToday().compareTo(startDate) >= 0) {
                totalSteps += Integer.parseInt(stepData.getStep());
                validDays++;
            }
        }

        return validDays > 0 ? (double) totalSteps / validDays : 0.0;
    }


    // 获取7天平均步数
    public double getWeekAverageSteps() {
        return getAverageSteps(7);
    }

    // 获取30天平均步数
    public double getMonthAverageSteps() {
        return getAverageSteps(30);
    }

    // 获取步数最多的一天
    public StepData getMaxStepsDay() {
        List<StepData> stepDataList = DbUtils.getQueryAll(StepData.class);
        if (stepDataList.isEmpty()) {
            return null;
        }

        return Collections.max(stepDataList, (a, b) -> Integer.parseInt(a.getStep()) - Integer.parseInt(b.getStep()));
    }

    // 检查是否有通知权限
    public boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // 请求通知权限
    public void requestNotificationPermission(android.app.Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, StepConstants.NOTIFICATION_PERMISSION_CODE);
        }
    }

    // 检查是否已经忽略电池优化
    public boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    // 请求忽略电池优化
    public void requestIgnoreBatteryOptimizations(android.app.Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            activity.startActivity(intent);
        }
    }

    /**
     * 安全启动步数服务（应在用户与应用交互后调用）
     * 这个方法应该在主 Activity 中调用，而不是在 Application 创建时调用
     */
    public void startStepService() {
        // 使用静态方法安全启动服务
        StepService.startServiceSafely(context);
        
        // 如果之前没有绑定，尝试绑定服务
        if (!isBound) {
            bindService();
        }
    }

    /**
     * 检查服务是否在运行且稳定
     * @return true表示服务在运行且稳定，false表示服务需要重启
     */
    private boolean isServiceRunningAndStable() {
        // 检查绑定状态
        if (!isBound) {
            URLog.i(TAG, "服务未绑定，需要重启");
            return false;
        }
        
        // 检查服务实例是否存在
        if (stepService == null) {
            URLog.i(TAG, "服务实例为空，需要重启");
            return false;
        }
        
        // 可以添加更多稳定性检查，例如：
        // 1. 检查服务是否响应（通过简单的方法调用）
        // 2. 检查最近是否有服务异常断开记录
        // 3. 检查传感器是否正常工作等
        
        URLog.i(TAG, "服务正在运行且稳定");
        return true;
    }

    /**
     * 在服务启动失败后重试
     * 这个方法应该在获取到必要权限后调用
     * 优化：只在服务确实有问题时才重启，避免不必要的重启
     */
    public void retryStartService() {
        URLog.i(TAG, "检查服务状态并决定是否需要重试");
        
        // 首先检查服务是否已经在运行且稳定
        if (isServiceRunningAndStable()) {
            URLog.i(TAG, "服务已经在运行且稳定，无需重启");
            return;
        }
        
        URLog.i(TAG, "服务需要重启，开始重试流程");
        
        // 先解绑再重新绑定，强制重新创建服务
        if (isBound) {
            try {
                context.unbindService(serviceConnection);
                isBound = false;
                URLog.i(TAG, "成功解绑服务");
            } catch (Exception e) {
                URLog.e(TAG, "解绑服务失败: " + e.getMessage());
            }
        }
        
        // 先用 stopService 停止可能存在的服务
        try {
            Intent stopIntent = new Intent(context, StepService.class);
            boolean stopped = context.stopService(stopIntent);
            URLog.i(TAG, "停止现有服务结果: " + stopped);
        } catch (Exception e) {
            URLog.e(TAG, "停止服务失败: " + e.getMessage());
        }
        
        // 延迟一小段时间再重新启动，确保服务完全停止
        try {
            Thread.sleep(200); // 稍微增加延迟，确保服务完全停止
        } catch (InterruptedException e) {
            // 忽略
        }
        
        // 重新启动服务并绑定
        StepService.startServiceSafely(context);
        bindService();
        
        URLog.i(TAG, "服务重启流程完成");
    }
}
