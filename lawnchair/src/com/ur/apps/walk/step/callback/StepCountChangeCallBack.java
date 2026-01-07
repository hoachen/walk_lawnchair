package com.ur.apps.walk.step.callback;


import com.ur.apps.walk.step.bean.ExerciseStats;

public interface StepCountChangeCallBack {
    /**
     * 更新UI步数
     *
     * @param stepCount 步数
     */
    void onStepChange(ExerciseStats stepCount);

    void onStepReachPeriod(int hundred_level);
}
