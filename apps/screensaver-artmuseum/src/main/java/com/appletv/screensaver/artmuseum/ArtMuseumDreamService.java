package com.appletv.screensaver.artmuseum;

import android.service.dreams.DreamService;
import android.view.ViewGroup;

public class ArtMuseumDreamService extends DreamService {
    private ArtMuseumView museumView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        museumView = new ArtMuseumView(this);
        setContentView(museumView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (museumView != null) museumView.start();
    }

    @Override
    public void onDreamingStopped() {
        if (museumView != null) museumView.stop();
        super.onDreamingStopped();
    }
}
