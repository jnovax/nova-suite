package vn.familytv.parentalcontrol.security;

public interface VaultCipher {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
