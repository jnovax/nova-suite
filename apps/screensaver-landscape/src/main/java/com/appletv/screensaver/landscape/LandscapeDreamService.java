package com.appletv.screensaver.landscape;

import android.service.dreams.DreamService;
import android.view.KeyEvent;

public class LandscapeDreamService extends DreamService {

    private LandscapeView landscapeView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true);
        setFullscreen(true);
        setScreenBright(true);

        landscapeView = new LandscapeView(this);
        setContentView(landscapeView);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (landscapeView != null) {
            landscapeView.start();
        }
    }

    @Override
    public void onDreamingStopped() {
        if (landscapeView != null) {
            landscapeView.stop();
        }
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (landscapeView != null) {
            landscapeView.stop();
            landscapeView = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && landscapeView != null) {
            if (landscapeView.handleRemoteKey(event.getKeyCode())) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
