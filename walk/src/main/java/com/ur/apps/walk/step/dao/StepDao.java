package com.ur.apps.walk.step.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.ur.apps.walk.step.bean.StepData;

import java.util.List;

@Dao
public interface StepDao {
    @Insert
    void insert(StepData stepData);

    @Delete
    void delete(StepData stepData);

    @Update
    void update(StepData stepData);

    @Query("SELECT * FROM step")
    List<StepData> getAllSteps();

    @Query("SELECT * FROM step WHERE today = :date")
    StepData getStepsByDate(String date);

    @Query("DELETE FROM step")
    void deleteAll();
}