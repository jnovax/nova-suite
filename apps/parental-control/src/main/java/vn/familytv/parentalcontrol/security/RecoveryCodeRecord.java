package vn.familytv.parentalcontrol.security;

import java.util.Objects;

public final class RecoveryCodeRecord {
    private final String hash;
    private final boolean used;
    private final Long usedAtEpochMs;

    public RecoveryCodeRecord(String hash) {
        this(hash, false, null);
    }

    public RecoveryCodeRecord(String hash, boolean used) {
        this(hash, used, null);
    }

    public RecoveryCodeRecord(String hash, boolean used, Long usedAtEpochMs) {
        this.hash = hash;
        this.used = used;
        this.usedAtEpochMs = usedAtEpochMs;
    }

    public String getHash() {
        return hash;
    }

    public boolean isUsed() {
        return used;
    }

    public Long getUsedAtEpochMs() {
        return usedAtEpochMs;
    }

    public RecoveryCodeRecord copy(boolean used, Long usedAtEpochMs) {
        return new RecoveryCodeRecord(this.hash, used, usedAtEpochMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecoveryCodeRecord that = (RecoveryCodeRecord) o;
        return used == that.used &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(usedAtEpochMs, that.usedAtEpochMs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, used, usedAtEpochMs);
    }

    @Override
    public String toString() {
        return "RecoveryCodeRecord(hash=" + hash + ", used=" + used + ", usedAtEpochMs=" + usedAtEpochMs + ")";
    }
}
