package vn.familytv.parentalcontrol.screentime;

import android.content.SharedPreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScreenTimeStoreTest {
    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> storage = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(storage);
        }

        @Override
        public String getString(String key, String defValue) {
            Object val = storage.get(key);
            return val instanceof String ? (String) val : defValue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object val = storage.get(key);
            return val instanceof Set ? (Set<String>) val : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = storage.get(key);
            return val instanceof Integer ? (Integer) val : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object val = storage.get(key);
            return val instanceof Long ? (Long) val : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object val = storage.get(key);
            return val instanceof Float ? (Float) val : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object val = storage.get(key);
            return val instanceof Boolean ? (Boolean) val : defValue;
        }

        @Override
        public boolean contains(String key) {
            return storage.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor(storage);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private static class FakeEditor implements Editor {
            private final Map<String, Object> target;
            private final Map<String, Object> pending = new HashMap<>();
            private final Set<String> toRemove = new HashSet<>();
            private boolean shouldClear = false;

            public FakeEditor(Map<String, Object> target) {
                this.target = target;
            }

            @Override
            public Editor putString(String key, String value) {
                if (key != null) pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                if (key != null) pending.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                if (key != null) pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                if (key != null) pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                if (key != null) pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                if (key != null) pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                if (key != null) toRemove.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                shouldClear = true;
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (shouldClear) target.clear();
                for (String k : toRemove) {
                    target.remove(k);
                }
                target.putAll(pending);
            }
        }
    }

    private FakeSharedPreferences fakePrefs;
    private ScreenTimeStore store;
    private ScreenTimePolicy policy;

    @Before
    public void setup() {
        fakePrefs = new FakeSharedPreferences();
        store = new ScreenTimeStore(fakePrefs);
        policy = new ScreenTimePolicy();
    }

    @Test
    public void testDefaultHours() {
        Assert.assertEquals(2, store.getWeekdayHours());
        Assert.assertEquals(3, store.getWeekendHours());
    }

    @Test
    public void testSetHoursCoercedBetween1And4() {
        store.setWeekdayHours(0);
        Assert.assertEquals(1, store.getWeekdayHours());

        store.setWeekdayHours(5);
        Assert.assertEquals(4, store.getWeekdayHours());

        store.setWeekendHours(-1);
        Assert.assertEquals(1, store.getWeekendHours());

        store.setWeekendHours(10);
        Assert.assertEquals(4, store.getWeekendHours());
    }

    @Test
    public void testDailyLimitHoursForWeekdayAndWeekend() {
        Calendar weekdayCal = Calendar.getInstance();
        weekdayCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        Assert.assertEquals(2, store.getDailyLimitHours(weekdayCal, policy));

        Calendar weekendCal = Calendar.getInstance();
        weekendCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        Assert.assertEquals(3, store.getDailyLimitHours(weekendCal, policy));
    }

    @Test
    public void testCheckAndResetDay() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2026, Calendar.NOVEMBER, 8);
        store.setUsedSec(1000);
        store.setExtensionSec(500);

        boolean reset1 = store.checkAndResetDay(cal1, policy);
        Assert.assertTrue(reset1);
        Assert.assertEquals("2026-11-08", store.getLastTrackedDate());
        Assert.assertEquals(0, store.getUsedSec());
        Assert.assertEquals(0, store.getExtensionSec());

        // Same day -> no reset
        store.setUsedSec(1200);
        boolean resetSame = store.checkAndResetDay(cal1, policy);
        Assert.assertFalse(resetSame);
        Assert.assertEquals(1200, store.getUsedSec());

        // Next day -> reset
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2026, Calendar.NOVEMBER, 9);
        boolean reset2 = store.checkAndResetDay(cal2, policy);
        Assert.assertTrue(reset2);
        Assert.assertEquals("2026-11-09", store.getLastTrackedDate());
        Assert.assertEquals(0, store.getUsedSec());
    }

    @Test
    public void testRemainingAndExtensionSec() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // 2 hours = 7200s
        store.setWeekdayHours(2);

        Assert.assertEquals(7200, store.getTotalLimitSec(cal, policy));
        Assert.assertEquals(7200, store.getRemainingSec(cal, policy));

        store.setUsedSec(3000);
        Assert.assertEquals(4200, store.getRemainingSec(cal, policy));

        store.addExtensionSec(1800); // + 30 mins
        Assert.assertEquals(9000, store.getTotalLimitSec(cal, policy));
        Assert.assertEquals(6000, store.getRemainingSec(cal, policy));
    }
}
