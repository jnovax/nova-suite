package vn.familytv.parentalcontrol.ipc;

import android.content.Context;
import android.content.Intent;

public class ModeBroadcastSender {
    public static final String ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_SESSION_REMAINING_SEC = "extra_session_remaining_sec";
    public static final String EXTRA_IS_QUIZ_ACTIVE = "extra_is_quiz_active";

    private final Context context;

    public ModeBroadcastSender(Context context) {
        this.context = context;
    }

    public void sendModeChanged(Mode mode, int sessionRemainingSec, boolean isQuizActive) {
        Intent intent = createIntent(mode, sessionRemainingSec, isQuizActive);
        context.sendBroadcast(intent);
    }

    public static Intent createIntent(Mode mode, int sessionRemainingSec, boolean isQuizActive) {
        return new ModeIntent(mode, sessionRemainingSec, isQuizActive);
    }

    private static class ModeIntent extends Intent {
        private final Mode mode;
        private final int sessionRemainingSec;
        private final boolean isQuizActive;

        public ModeIntent(Mode mode, int sessionRemainingSec, boolean isQuizActive) {
            super(ACTION_MODE_CHANGED);
            this.mode = mode;
            this.sessionRemainingSec = sessionRemainingSec;
            this.isQuizActive = isQuizActive;
            try {
                putExtra(EXTRA_MODE, mode != null ? mode.name() : null);
                putExtra(EXTRA_SESSION_REMAINING_SEC, sessionRemainingSec);
                putExtra(EXTRA_IS_QUIZ_ACTIVE, isQuizActive);
            } catch (RuntimeException ignored) {
                // In local unit tests without Robolectric, Android framework methods throw "Method ... not mocked".
            }
        }

        @Override
        public String getAction() {
            return ACTION_MODE_CHANGED;
        }

        @Override
        public String getStringExtra(String name) {
            if (EXTRA_MODE.equals(name)) {
                return mode != null ? mode.name() : null;
            }
            try {
                return super.getStringExtra(name);
            } catch (RuntimeException e) {
                return null;
            }
        }

        @Override
        public int getIntExtra(String name, int defaultValue) {
            if (EXTRA_SESSION_REMAINING_SEC.equals(name)) {
                return sessionRemainingSec;
            }
            try {
                return super.getIntExtra(name, defaultValue);
            } catch (RuntimeException e) {
                return defaultValue;
            }
        }

        @Override
        public boolean getBooleanExtra(String name, boolean defaultValue) {
            if (EXTRA_IS_QUIZ_ACTIVE.equals(name)) {
                return isQuizActive;
            }
            try {
                return super.getBooleanExtra(name, defaultValue);
            } catch (RuntimeException e) {
                return defaultValue;
            }
        }
    }
}
