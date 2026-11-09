package vn.familytv.parentalcontrol.security;

import org.junit.Assert;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

public class RecoveryCodeManagerTest {
    @Test
    public void testGenerateSetCreates6Distinct6DigitCodes() {
        GeneratedRecoverySet set = RecoveryCodeManager.generateSet();
        Assert.assertEquals(6, set.getPlainCodes().size());
        Assert.assertEquals(6, set.getRecords().size());
        Set<String> uniqueCodes = new HashSet<>(set.getPlainCodes());
        Assert.assertEquals(6, uniqueCodes.size());
        for (String code : set.getPlainCodes()) {
            Assert.assertEquals(6, code.length());
            for (char c : code.toCharArray()) {
                Assert.assertTrue(Character.isDigit(c));
            }
        }
        for (RecoveryCodeRecord record : set.getRecords()) {
            Assert.assertFalse(record.isUsed());
            Assert.assertNull(record.getUsedAtEpochMs());
        }
    }

    @Test
    public void testVerifyAndConsumeSuccessAndSingleUse() {
        GeneratedRecoverySet set = RecoveryCodeManager.generateSet();
        String firstCode = set.getPlainCodes().get(0);
        RecoveryCodeManager.VerifyResult res1 = RecoveryCodeManager.verifyAndConsume(
                firstCode, set.getSaltBase64(), set.getRecords(), 1700000000L
        );
        Assert.assertTrue(res1.success);
        RecoveryCodeRecord usedRecord = null;
        for (RecoveryCodeRecord r : res1.updatedRecords) {
            if (r.isUsed()) {
                usedRecord = r;
                break;
            }
        }
        Assert.assertNotNull(usedRecord);
        Assert.assertEquals(Long.valueOf(1700000000L), usedRecord.getUsedAtEpochMs());

        // Try using the exact same code a second time
        RecoveryCodeManager.VerifyResult res2 = RecoveryCodeManager.verifyAndConsume(
                firstCode, set.getSaltBase64(), res1.updatedRecords, 1700000010L
        );
        Assert.assertFalse(res2.success);
    }

    @Test
    public void testVerifyRejectsInvalidCode() {
        GeneratedRecoverySet set = RecoveryCodeManager.generateSet();
        RecoveryCodeManager.VerifyResult res = RecoveryCodeManager.verifyAndConsume(
                "000000", set.getSaltBase64(), set.getRecords(), 1700000000L
        );
        Assert.assertFalse(res.success);
    }
}
