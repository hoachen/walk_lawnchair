package com.ur.apps.walk.step.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ur.apps.walk.step.bean.StepData;
import com.ur.apps.walk.step.dao.StepDao;

@Database(entities = {StepData.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "walk_app.db";
    private static volatile AppDatabase instance;

    public abstract StepDao stepDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}