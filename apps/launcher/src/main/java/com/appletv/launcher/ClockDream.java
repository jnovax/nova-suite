package com.appletv.launcher;

import android.service.dreams.DreamService;

/** System entry point; the component name remains stable for existing Android settings. */
public class ClockDream extends DreamService {
    private ScreensaverView display;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        display = new ScreensaverView(this);
        setContentView(display);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        display.start();
    }

    @Override public void onDreamingStopped() {
        if (display != null) display.stop();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if (display != null) display.stop();
        super.onDetachedFromWindow();
    }
}
