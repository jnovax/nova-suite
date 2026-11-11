package com.appletv.screensaver.landscape;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class LandscapeActivity extends Activity {

    private LandscapeView landscapeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        landscapeView = new LandscapeView(this);
        setContentView(landscapeView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (landscapeView != null) {
            landscapeView.start();
        }
    }

    @Override
    protected void onPause() {
        if (landscapeView != null) {
            landscapeView.stop();
        }
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (landscapeView != null && landscapeView.handleRemoteKey(keyCode)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
