package com.ur.apps.launcher;

public class LauncherDeepUtils {
    private static final String TAG = "LauncherDeepUtils";

    static {
        System.loadLibrary("launcher_sdk_rust");
    }

    private static volatile LauncherDeepUtils instance;


    public static LauncherDeepUtils getInstance() {
        if (instance == null) {
            synchronized (LauncherDeepUtils.class) {
                if (instance == null) {
                    instance = new LauncherDeepUtils();
                }
            }
        }
        return instance;
    }

    public native void fun1();//init

}
