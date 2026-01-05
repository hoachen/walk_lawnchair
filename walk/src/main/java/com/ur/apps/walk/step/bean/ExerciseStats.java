package com.ur.apps.walk.step.bean;

import androidx.annotation.NonNull;

public class ExerciseStats {
    private int steps;
    private double distance;
    private double calories;
    private long duration;

    public ExerciseStats(int steps, double distance, double calories, long duration) {
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
        this.duration = duration;
    }

    public int getSteps() {
        return steps;
    }

    public double getDistance() {
        return distance;
    }

    public double getCalories() {
        return calories;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "ExerciseStats{" +
                "steps=" + steps +
                ", distance=" + distance +
                ", calories=" + calories +
                ", duration=" + duration +
                '}';
    }
}