package com.ur.apps.walk.step.bean;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "step")
public class StepData {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String today;
    private String step;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "StepData{" + "id=" + id + ", today='" + today + '\'' + ", step='" + step + '\'' + '}';
    }
}
