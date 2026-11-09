package vn.familytv.parentalcontrol.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class RecoveryCodeManager {
    private static final SecureRandom RANDOM = new SecureRandom();

    private RecoveryCodeManager() {}

    public static GeneratedRecoverySet generateSet() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltBase64 = Base32.encode(salt).replace("=", "");

        LinkedHashSet<String> codes = new LinkedHashSet<>();
        while (codes.size() < 6) {
            int num = 100000 + RANDOM.nextInt(900000);
            codes.add(Integer.toString(num));
        }

        List<String> plainCodes = new ArrayList<>(codes);
        List<RecoveryCodeRecord> records = new ArrayList<>();
        for (String code : plainCodes) {
            records.add(new RecoveryCodeRecord(hash(code, saltBase64)));
        }
        return new GeneratedRecoverySet(plainCodes, saltBase64, records);
    }

    public static final class VerifyResult {
        public final boolean success;
        public final List<RecoveryCodeRecord> updatedRecords;

        public VerifyResult(boolean success, List<RecoveryCodeRecord> updatedRecords) {
            this.success = success;
            this.updatedRecords = updatedRecords;
        }

        public boolean component1() {
            return success;
        }

        public List<RecoveryCodeRecord> component2() {
            return updatedRecords;
        }
    }

    public static VerifyResult verifyAndConsume(
            String inputCode,
            String saltBase64,
            List<RecoveryCodeRecord> currentRecords,
            long nowEpochMs
    ) {
        if (inputCode == null) {
            return new VerifyResult(false, currentRecords);
        }
        String clean = inputCode.trim();
        if (clean.length() != 6) {
            return new VerifyResult(false, currentRecords);
        }
        for (int i = 0; i < clean.length(); i++) {
            if (!Character.isDigit(clean.charAt(i))) {
                return new VerifyResult(false, currentRecords);
            }
        }

        String targetHash = hash(clean, saltBase64);
        int matchedIndex = -1;
        for (int i = 0; i < currentRecords.size(); i++) {
            RecoveryCodeRecord rec = currentRecords.get(i);
            if (rec.getHash().equals(targetHash) && !rec.isUsed()) {
                matchedIndex = i;
                break;
            }
        }

        if (matchedIndex == -1) {
            return new VerifyResult(false, currentRecords);
        }

        List<RecoveryCodeRecord> updated = new ArrayList<>(currentRecords);
        RecoveryCodeRecord old = updated.get(matchedIndex);
        updated.set(matchedIndex, old.copy(true, nowEpochMs));
        return new VerifyResult(true, updated);
    }

    private static String hash(String code, String saltBase64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltBase64.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
