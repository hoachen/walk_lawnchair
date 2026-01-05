package com.ur.apps.walk.step.utils;

import android.content.Context;

import com.ur.apps.walk.WalkApplication;
import com.ur.apps.walk.step.db.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据库工具类，使用Room实现
 */
public class DbUtils {
    private static AppDatabase database;
    private static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public static void createDb(Context context) {
        if (database == null) {
            database = AppDatabase.getInstance(context);
        }
    }

    public static AppDatabase getDatabase() {
        DbUtils.createDb(WalkApplication.Companion.getContext());
        return database;
    }

    public static void insert(final Object t) {
        databaseExecutor.execute(() -> {
            if (t instanceof com.ur.apps.walk.step.bean.StepData) {
                getDatabase().stepDao().insert((com.ur.apps.walk.step.bean.StepData) t);
            }
        });
    }

    public static void insertAll(final List<?> list) {
        databaseExecutor.execute(() -> {
            if (!list.isEmpty() && list.get(0) instanceof com.ur.apps.walk.step.bean.StepData) {
                for (Object item : list) {
                    getDatabase().stepDao().insert((com.ur.apps.walk.step.bean.StepData) item);
                }
            }
        });
    }

    public interface QueryCallback<T> {
        void onResult(List<T> result);
    }

    public static <T> void getQueryAll(Class<T> cla, QueryCallback<T> callback) {
        databaseExecutor.execute(() -> {
            List<T> result = null;
            if (cla == com.ur.apps.walk.step.bean.StepData.class) {
                result = (List<T>) getDatabase().stepDao().getAllSteps();
            }
            final List<T> finalResult = result != null ? result : new java.util.ArrayList<>();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.onResult(finalResult);
            });
        });
    }

    public static <T> List<T> getQueryAll(Class<T> cla) {
        List<T> result = null;
        if (cla == com.ur.apps.walk.step.bean.StepData.class) {
            result = (List<T>) getDatabase().stepDao().getAllSteps();
        }
        return result != null ? result : new java.util.ArrayList<>();
    }

    public static <T> void getQueryByWhere(Class<T> cla, String field, String[] value, QueryCallback<T> callback) {
        databaseExecutor.execute(() -> {
            List<T> result = new java.util.ArrayList<>();
            if (cla == com.ur.apps.walk.step.bean.StepData.class && field.equals("today") && value.length > 0) {
                com.ur.apps.walk.step.bean.StepData data = getDatabase().stepDao().getStepsByDate(value[0]);
                if (data != null) {
                    result.add((T) data);
                }
            }
            final List<T> finalResult = result;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.onResult(finalResult);
            });
        });
    }

    public static <T> void getQueryByWhereLength(Class<T> cla, String field, String[] value, int start, int length, QueryCallback<T> callback) {
        getQueryByWhere(cla, field, value, callback);
    }

    public static void deleteAll(Class<?> cla) {
        databaseExecutor.execute(() -> {
            if (cla == com.ur.apps.walk.step.bean.StepData.class) {
                getDatabase().stepDao().deleteAll();
            }
        });
    }

    public static void deleteWhere(Class<?> cla, String field, String[] value, final Runnable onComplete) {
        databaseExecutor.execute(() -> {
            if (cla == com.ur.apps.walk.step.bean.StepData.class && field.equals("today") && value.length > 0) {
                com.ur.apps.walk.step.bean.StepData data = getDatabase().stepDao().getStepsByDate(value[0]);
                if (data != null) {
                    getDatabase().stepDao().delete(data);
                }
            }
            if (onComplete != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public static void update(final Object t) {
        databaseExecutor.execute(() -> {
            if (t instanceof com.ur.apps.walk.step.bean.StepData) {
                getDatabase().stepDao().update((com.ur.apps.walk.step.bean.StepData) t);
            }
        });
    }

    public static void updateAll(final List<?> list) {
        databaseExecutor.execute(() -> {
            if (!list.isEmpty() && list.get(0) instanceof com.ur.apps.walk.step.bean.StepData) {
                for (Object item : list) {
                    getDatabase().stepDao().update((com.ur.apps.walk.step.bean.StepData) item);
                }
            }
        });
    }


}
