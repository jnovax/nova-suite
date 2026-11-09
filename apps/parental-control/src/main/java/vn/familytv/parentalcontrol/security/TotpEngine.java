package vn.familytv.parentalcontrol.security;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpEngine {
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final long PERIOD_SECONDS = 30L;
    private static final int DIGITS = 6;
    private static final int MODULO = 1000000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpEngine() {}

    public static String generateSecret() {
        return generateSecret(20);
    }

    public static String generateSecret(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base32.encode(bytes).replace("=", "");
    }

    public static String generateOtpAuthUri(String secretBase32, String accountName, String issuer) {
        try {
            String encodedAccount = URLEncoder.encode(accountName, "UTF-8").replace("+", "%20");
            String encodedIssuer = URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
            return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount +
                    "?secret=" + secretBase32 +
                    "&issuer=" + encodedIssuer +
                    "&algorithm=SHA1&digits=" + DIGITS +
                    "&period=" + PERIOD_SECONDS;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateCode(String secretBase32, long epochSeconds) {
        long timeStep = epochSeconds / PERIOD_SECONDS;
        return generateCodeForStep(secretBase32, timeStep);
    }

    public static boolean verifyCode(String secretBase32, String candidateCode, long epochSeconds) {
        return verifyCode(secretBase32, candidateCode, epochSeconds, 1);
    }

    public static boolean verifyCode(String secretBase32, String candidateCode, long epochSeconds, int windowSteps) {
        if (candidateCode == null) return false;
        String cleaned = candidateCode.trim();
        if (cleaned.length() != DIGITS) return false;
        for (int i = 0; i < cleaned.length(); i++) {
            if (!Character.isDigit(cleaned.charAt(i))) return false;
        }
        long currentStep = epochSeconds / PERIOD_SECONDS;
        for (long stepOffset = -windowSteps; stepOffset <= windowSteps; stepOffset++) {
            String validCode = generateCodeForStep(secretBase32, currentStep + stepOffset);
            if (validCode.equals(cleaned)) return true;
        }
        return false;
    }

    private static String generateCodeForStep(String secretBase32, long timeStep) {
        try {
            byte[] keyBytes = Base32.decode(secretBase32);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));

            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);

            int pin = truncatedHash % MODULO;
            String pinStr = Integer.toString(pin);
            while (pinStr.length() < DIGITS) {
                pinStr = "0" + pinStr;
            }
            return pinStr;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
