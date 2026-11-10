package vn.familytv.parentalcontrol.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

public class AudioFocusInterrupter {
    private static final String TAG = "FamilyTime_Audio";

    private final AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean hasFocus = false;
    private boolean wasMuted = false;

    public AudioFocusInterrupter(Context context) {
        this.audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    public synchronized boolean acquireFocus() {
        if (hasFocus) return true;
        if (audioManager == null) return false;

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

        AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(change -> Log.i(TAG, "AUDIOFOCUS_CHANGED change=" + change))
                .build();

        int result = audioManager.requestAudioFocus(request);
        hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        focusRequest = request;
        Log.i(TAG, "AUDIOFOCUS_REQUEST result=" + result + " granted=" + hasFocus);

        try {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
            Log.i(TAG, "DISPATCH_MEDIA_PAUSE sent");
        } catch (Exception ignored) {}

        try {
            boolean isAlreadyMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
            if (!isAlreadyMuted) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
                wasMuted = true;
                Log.i(TAG, "STREAM_MUSIC muted with FLAG_SHOW_UI");
            }
        } catch (Exception ignored) {}

        return hasFocus;
    }

    public synchronized void releaseFocus() {
        if (!hasFocus && !wasMuted) return;
        if (audioManager == null) return;

        if (focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
            Log.i(TAG, "AUDIOFOCUS_ABANDONED");
        }
        focusRequest = null;
        hasFocus = false;

        if (wasMuted) {
            try {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
                Log.i(TAG, "STREAM_MUSIC unmuted with FLAG_SHOW_UI");
            } catch (Exception ignored) {}
            wasMuted = false;
        }

        try {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
            Log.i(TAG, "DISPATCH_MEDIA_PLAY sent");
        } catch (Exception ignored) {}
    }

    public synchronized boolean hasFocus() {
        return hasFocus;
    }
}
