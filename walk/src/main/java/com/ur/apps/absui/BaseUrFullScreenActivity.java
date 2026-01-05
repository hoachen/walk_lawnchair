package com.ur.apps.absui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseUrFullScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupWindow(this.getWindow());
        super.onCreate(savedInstanceState);
        setupStatusBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // 设置状态栏颜色
        window.setStatusBarColor(ContextCompat.getColor(this,
                android.R.color.transparent));

    }

    protected void setupWindow(Window window) {
        // 设置窗口背景
    }

    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
} 