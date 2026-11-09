package vn.familytv.parentalcontrol.security;

import org.junit.Assert;
import org.junit.Test;

public class TotpEngineTest {
    private final String rfcSecretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    public void testGenerateCodeRfc6238Vectors() {
        Assert.assertEquals("287082", TotpEngine.generateCode(rfcSecretBase32, 59L));
        Assert.assertEquals("081804", TotpEngine.generateCode(rfcSecretBase32, 1111111109L));
        Assert.assertEquals("005924", TotpEngine.generateCode(rfcSecretBase32, 1234567890L));
    }

    @Test
    public void testVerifyCodeWindowTolerance() {
        long epochNow = 1000000L;
        String currentCode = TotpEngine.generateCode(rfcSecretBase32, epochNow);
        String prevCode = TotpEngine.generateCode(rfcSecretBase32, epochNow - 30L);
        String nextCode = TotpEngine.generateCode(rfcSecretBase32, epochNow + 30L);
        String tooOldCode = TotpEngine.generateCode(rfcSecretBase32, epochNow - 60L);
        String tooFutureCode = TotpEngine.generateCode(rfcSecretBase32, epochNow + 60L);

        Assert.assertTrue(TotpEngine.verifyCode(rfcSecretBase32, currentCode, epochNow, 1));
        Assert.assertTrue(TotpEngine.verifyCode(rfcSecretBase32, prevCode, epochNow, 1));
        Assert.assertTrue(TotpEngine.verifyCode(rfcSecretBase32, nextCode, epochNow, 1));
        Assert.assertFalse(TotpEngine.verifyCode(rfcSecretBase32, tooOldCode, epochNow, 1));
        Assert.assertFalse(TotpEngine.verifyCode(rfcSecretBase32, tooFutureCode, epochNow, 1));
        Assert.assertFalse(TotpEngine.verifyCode(rfcSecretBase32, "000000", epochNow, 1));
    }

    @Test
    public void testGenerateSecretAndUri() {
        String secret = TotpEngine.generateSecret(20);
        Assert.assertEquals(32, secret.length());
        String uri = TotpEngine.generateOtpAuthUri(secret, "TCL TV", "FamilyTime");
        Assert.assertTrue(uri.startsWith("otpauth://totp/FamilyTime:TCL%20TV?secret=" + secret));
        Assert.assertTrue(uri.contains("issuer=FamilyTime"));
        Assert.assertTrue(uri.contains("algorithm=SHA1"));
        Assert.assertTrue(uri.contains("digits=6"));
        Assert.assertTrue(uri.contains("period=30"));
    }
}
