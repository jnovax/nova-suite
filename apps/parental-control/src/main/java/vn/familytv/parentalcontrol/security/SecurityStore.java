package vn.familytv.parentalcontrol.security;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecurityStore {
    public interface SimpleKeyValueStore {
        String getString(String key);
        void putString(String key, String value);
        boolean getBoolean(String key, boolean def);
        void putBoolean(String key, boolean value);
        int getInt(String key, int def);
        void putInt(String key, int value);
        long getLong(String key, long def);
        void putLong(String key, long value);
    }

    public static class SharedPrefsStorage implements SimpleKeyValueStore {
        private final SharedPreferences prefs;

        public SharedPrefsStorage(SharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override
        public String getString(String key) {
            return prefs.getString(key, null);
        }

        @Override
        public void putString(String key, String value) {
            prefs.edit().putString(key, value).apply();
        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            return prefs.getBoolean(key, def);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            prefs.edit().putBoolean(key, value).apply();
        }

        @Override
        public int getInt(String key, int def) {
            return prefs.getInt(key, def);
        }

        @Override
        public void putInt(String key, int value) {
            prefs.edit().putInt(key, value).apply();
        }

        @Override
        public long getLong(String key, long def) {
            return prefs.getLong(key, def);
        }

        @Override
        public void putLong(String key, long value) {
            prefs.edit().putLong(key, value).apply();
        }
    }

    private static final String PREFS_NAME = "family_time_security";
    private static final String KEY_ENCRYPTED_SECRET = "encrypted_totp_secret";
    private static final String KEY_IS_ENROLLED = "is_enrolled";
    private static final String KEY_RECOVERY_SALT = "recovery_salt";
    private static final String KEY_RECOVERY_RECORDS = "recovery_records";
    private static final String KEY_AUTH_MODE = "auth_mode";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_IS_CUSTOM_PIN = "is_custom_pin";
    private static final String DEFAULT_PIN = "000000";
    private static final String DEFAULT_PIN_SALT = "DEFAULT_SALT_FAMILY_TIME";

    private final SimpleKeyValueStore storage;
    private final VaultCipher cipher;

    public SecurityStore(SimpleKeyValueStore storage, VaultCipher cipher) {
        this.storage = storage;
        this.cipher = cipher;
    }

    public static SecurityStore fromContext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new SecurityStore(new SharedPrefsStorage(prefs), new AndroidKeyStoreCipher());
    }

    public boolean isEnrolled() {
        return storage.getBoolean(KEY_IS_ENROLLED, false);
    }

    public AuthMode getAuthMode() {
        String modeStr = storage.getString(KEY_AUTH_MODE);
        if (modeStr != null) {
            try {
                return AuthMode.valueOf(modeStr);
            } catch (Exception ignored) {
                return AuthMode.PIN;
            }
        }
        if (storage.getBoolean(KEY_IS_ENROLLED, false) && storage.getString(KEY_ENCRYPTED_SECRET) != null) {
            return AuthMode.TOTP;
        }
        return AuthMode.PIN;
    }

    public boolean isCustomPin() {
        return storage.getBoolean(KEY_IS_CUSTOM_PIN, false);
    }

    private static String hashPin(String pin, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((salt + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifyPin(String candidatePin) {
        String salt = storage.getString(KEY_PIN_SALT);
        String hash = storage.getString(KEY_PIN_HASH);
        if (salt == null || hash == null) {
            salt = DEFAULT_PIN_SALT;
            hash = hashPin(DEFAULT_PIN, salt);
            storage.putString(KEY_PIN_SALT, salt);
            storage.putString(KEY_PIN_HASH, hash);
            storage.putBoolean(KEY_IS_CUSTOM_PIN, false);
        }
        return hash.equals(hashPin(candidatePin, salt));
    }

    public void saveCustomPin(String newPin) {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        String salt = Base32.encode(randomBytes);
        if (salt.length() > 16) {
            salt = salt.substring(0, 16);
        }
        String hash = hashPin(newPin, salt);
        storage.putString(KEY_PIN_SALT, salt);
        storage.putString(KEY_PIN_HASH, hash);
        storage.putBoolean(KEY_IS_CUSTOM_PIN, true);
        storage.putString(KEY_AUTH_MODE, AuthMode.PIN.name());
        storage.putBoolean(KEY_IS_ENROLLED, true);
    }

    public void saveTotpSecret(String secretBase32) {
        String encrypted = cipher.encrypt(secretBase32);
        storage.putString(KEY_ENCRYPTED_SECRET, encrypted);
        storage.putBoolean(KEY_IS_ENROLLED, true);
    }

    public void enableTotp(String secretBase32, String salt, List<RecoveryCodeRecord> records) {
        saveTotpSecret(secretBase32);
        saveRecoverySet(salt, records);
        storage.putString(KEY_AUTH_MODE, AuthMode.TOTP.name());
        storage.putBoolean(KEY_IS_ENROLLED, true);
    }

    public void disableTotp(String fallbackPin) {
        storage.putString(KEY_ENCRYPTED_SECRET, "");
        storage.putString(KEY_RECOVERY_SALT, "");
        storage.putString(KEY_RECOVERY_RECORDS, "");
        saveCustomPin(fallbackPin);
        storage.putString(KEY_AUTH_MODE, AuthMode.PIN.name());
    }

    public String getTotpSecret() {
        String encrypted = storage.getString(KEY_ENCRYPTED_SECRET);
        if (encrypted == null || encrypted.trim().isEmpty()) {
            return null;
        }
        try {
            return cipher.decrypt(encrypted);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveRecoverySet(String saltBase64, List<RecoveryCodeRecord> records) {
        storage.putString(KEY_RECOVERY_SALT, saltBase64);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            RecoveryCodeRecord rec = records.get(i);
            sb.append(rec.getHash()).append(",").append(rec.isUsed()).append(",").append(rec.getUsedAtEpochMs() != null ? rec.getUsedAtEpochMs() : 0);
            if (i < records.size() - 1) {
                sb.append(";");
            }
        }
        storage.putString(KEY_RECOVERY_RECORDS, sb.toString());
    }

    public String getRecoverySalt() {
        String salt = storage.getString(KEY_RECOVERY_SALT);
        return (salt != null && !salt.trim().isEmpty()) ? salt : null;
    }

    public List<RecoveryCodeRecord> getRecoveryRecords() {
        String raw = storage.getString(KEY_RECOVERY_RECORDS);
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] entries = raw.split(";");
        List<RecoveryCodeRecord> list = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                String hash = parts[0];
                boolean used = Boolean.parseBoolean(parts[1]);
                long usedAt = Long.parseLong(parts[2]);
                list.add(new RecoveryCodeRecord(hash, used, usedAt > 0 ? usedAt : null));
            }
        }
        return list;
    }

    public void markRecoveryRecordUsed(String hash, long epochMs) {
        List<RecoveryCodeRecord> current = getRecoveryRecords();
        List<RecoveryCodeRecord> updated = new ArrayList<>();
        for (RecoveryCodeRecord rec : current) {
            if (rec.getHash().equals(hash)) {
                updated.add(rec.copy(true, epochMs));
            } else {
                updated.add(rec);
            }
        }
        String salt = getRecoverySalt();
        saveRecoverySet(salt != null ? salt : "", updated);
    }
}
