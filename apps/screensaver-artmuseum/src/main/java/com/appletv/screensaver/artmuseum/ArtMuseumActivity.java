package com.appletv.screensaver.artmuseum;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

public class ArtMuseumActivity extends Activity {
    private ArtMuseumView museumView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        overridePendingTransition(0, 0);

        museumView = new ArtMuseumView(this);
        setContentView(museumView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (museumView != null) museumView.start();
    }

    @Override
    protected void onPause() {
        if (museumView != null) museumView.stop();
        super.onPause();
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            finish();
            overridePendingTransition(0, 0);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        finish();
        overridePendingTransition(0, 0);
    }
}
