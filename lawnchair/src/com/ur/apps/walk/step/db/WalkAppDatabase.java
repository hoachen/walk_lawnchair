package com.ur.apps.walk.step.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ur.apps.walk.step.bean.StepData;
import com.ur.apps.walk.step.dao.StepDao;

@Database(entities = {StepData.class}, version = 1, exportSchema = false)
public abstract class WalkAppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "walk_app.db";
    private static volatile WalkAppDatabase instance;

    public abstract StepDao stepDao();

    public static synchronized WalkAppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    WalkAppDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
