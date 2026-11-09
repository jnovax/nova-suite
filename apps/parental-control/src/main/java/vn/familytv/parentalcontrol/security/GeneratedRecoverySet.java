package vn.familytv.parentalcontrol.security;

import java.util.List;

public final class GeneratedRecoverySet {
    private final List<String> plainCodes;
    private final String saltBase64;
    private final List<RecoveryCodeRecord> records;

    public GeneratedRecoverySet(List<String> plainCodes, String saltBase64, List<RecoveryCodeRecord> records) {
        this.plainCodes = plainCodes;
        this.saltBase64 = saltBase64;
        this.records = records;
    }

    public List<String> getPlainCodes() {
        return plainCodes;
    }

    public String getSaltBase64() {
        return saltBase64;
    }

    public List<RecoveryCodeRecord> getRecords() {
        return records;
    }
}
