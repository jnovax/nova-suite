package vn.familytv.parentalcontrol.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class AndroidKeyStoreCipher implements VaultCipher {
    private final String keyAlias;
    private final KeyStore keyStore;

    public AndroidKeyStoreCipher() {
        this("FamilyTimeMasterKey");
    }

    public AndroidKeyStoreCipher(String keyAlias) {
        this.keyAlias = keyAlias;
        try {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            this.keyStore.load(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        if (!keyStore.containsAlias(keyAlias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            );
            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(
                            keyAlias,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                    )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build()
            );
            return keyGenerator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(keyAlias, null)).getSecretKey();
    }

    @Override
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base32.encode(combined).replace("=", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] combined = Base32.decode(cipherText);
            if (combined.length < 12) {
                throw new IllegalArgumentException("Ciphertext too short");
            }
            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, 12);
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
