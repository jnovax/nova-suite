package com.appletv.screensaver.flipclock;

import android.service.dreams.DreamService;
import android.view.ViewGroup;

public class FlipClockDreamService extends DreamService {
    private FliqloClockView clockView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        clockView = new FliqloClockView(this);
        setContentView(clockView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (clockView != null) clockView.start();
    }

    @Override
    public void onDreamingStopped() {
        if (clockView != null) clockView.stop();
        super.onDreamingStopped();
    }
}
