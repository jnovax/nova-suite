package com.appletv.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

/** In-launcher 4K Wallpaper slideshow entry point. */
public final class ScreenSaverActivity extends Activity {
    private ScreensaverView display;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        display = new ScreensaverView(this);
        setContentView(display);
        display.start();
        overridePendingTransition(0, 0);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            finish();
            overridePendingTransition(0, 0);
            return true;
        }
        return true;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            finish();
            overridePendingTransition(0, 0);
        }
        return true;
    }

    @Override protected void onPause() {
        super.onPause();
        if (display != null) display.stop();
        finish();
        overridePendingTransition(0, 0);
    }
}
