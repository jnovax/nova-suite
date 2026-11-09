package vn.familytv.parentalcontrol.security;

import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityStoreTest {
    private final Map<String, Object> memoryStorage = new HashMap<>();

    private final SecurityStore.SimpleKeyValueStore fakePrefs = new SecurityStore.SimpleKeyValueStore() {
        @Override
        public String getString(String key) {
            return (String) memoryStorage.get(key);
        }

        @Override
        public void putString(String key, String value) {
            memoryStorage.put(key, value);
        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            Boolean val = (Boolean) memoryStorage.get(key);
            return val != null ? val : def;
        }

        @Override
        public void putBoolean(String key, boolean value) {
            memoryStorage.put(key, value);
        }

        @Override
        public int getInt(String key, int def) {
            Integer val = (Integer) memoryStorage.get(key);
            return val != null ? val : def;
        }

        @Override
        public void putInt(String key, int value) {
            memoryStorage.put(key, value);
        }

        @Override
        public long getLong(String key, long def) {
            Long val = (Long) memoryStorage.get(key);
            return val != null ? val : def;
        }

        @Override
        public void putLong(String key, long value) {
            memoryStorage.put(key, value);
        }
    };

    private final VaultCipher fakeCipher = new VaultCipher() {
        @Override
        public String encrypt(String plainText) {
            return "ENC:" + plainText;
        }

        @Override
        public String decrypt(String cipherText) {
            return cipherText.startsWith("ENC:") ? cipherText.substring(4) : cipherText;
        }
    };

    private final SecurityStore store = new SecurityStore(fakePrefs, fakeCipher);

    @Test
    public void testStoreAndRetrieveSecret() {
        Assert.assertFalse(store.isEnrolled());
        store.saveTotpSecret("JBSWY3DPEHPK3PXP");
        Assert.assertEquals("JBSWY3DPEHPK3PXP", store.getTotpSecret());
        Assert.assertTrue(store.isEnrolled());
    }

    @Test
    public void testStoreAndConsumeRecoveryCodes() {
        List<RecoveryCodeRecord> records = new ArrayList<>();
        records.add(new RecoveryCodeRecord("hash1", false));
        records.add(new RecoveryCodeRecord("hash2", false));

        store.saveRecoverySet("salt123", records);
        Assert.assertEquals("salt123", store.getRecoverySalt());
        Assert.assertEquals(2, store.getRecoveryRecords().size());

        store.markRecoveryRecordUsed("hash1", 1700000000L);
        List<RecoveryCodeRecord> updated = store.getRecoveryRecords();
        boolean foundHash1Used = false;
        boolean foundHash2Used = false;
        for (RecoveryCodeRecord r : updated) {
            if ("hash1".equals(r.getHash()) && r.isUsed()) foundHash1Used = true;
            if ("hash2".equals(r.getHash()) && r.isUsed()) foundHash2Used = true;
        }
        Assert.assertTrue(foundHash1Used);
        Assert.assertFalse(foundHash2Used);
    }

    @Test
    public void testDefaultPinAuthModeAndVerification() {
        Assert.assertEquals(AuthMode.PIN, store.getAuthMode());
        Assert.assertFalse(store.isCustomPin());
        Assert.assertTrue(store.verifyPin("000000"));
        Assert.assertFalse(store.verifyPin("123456"));
    }

    @Test
    public void testSaveCustomPin() {
        store.saveCustomPin("123456");
        Assert.assertEquals(AuthMode.PIN, store.getAuthMode());
        Assert.assertTrue(store.isCustomPin());
        Assert.assertTrue(store.verifyPin("123456"));
        Assert.assertFalse(store.verifyPin("000000"));
    }

    @Test
    public void testEnableAndDisableTotp() {
        List<RecoveryCodeRecord> records = new ArrayList<>();
        records.add(new RecoveryCodeRecord("hash1", false));
        records.add(new RecoveryCodeRecord("hash2", false));

        store.enableTotp("JBSWY3DPEHPK3PXP", "salt123", records);
        Assert.assertEquals(AuthMode.TOTP, store.getAuthMode());
        Assert.assertEquals("JBSWY3DPEHPK3PXP", store.getTotpSecret());
        Assert.assertEquals("salt123", store.getRecoverySalt());
        Assert.assertEquals(2, store.getRecoveryRecords().size());

        store.disableTotp("654321");
        Assert.assertEquals(AuthMode.PIN, store.getAuthMode());
        Assert.assertNull(store.getTotpSecret());
        Assert.assertNull(store.getRecoverySalt());
        Assert.assertTrue(store.getRecoveryRecords().isEmpty());
        Assert.assertTrue(store.isCustomPin());
        Assert.assertTrue(store.verifyPin("654321"));
    }
}
