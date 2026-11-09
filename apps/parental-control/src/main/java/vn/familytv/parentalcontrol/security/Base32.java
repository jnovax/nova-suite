package vn.familytv.parentalcontrol.security;

import java.util.ArrayList;

public final class Base32 {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int[] DECODE_TABLE = new int[128];

    static {
        for (int i = 0; i < DECODE_TABLE.length; i++) {
            DECODE_TABLE[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length(); i++) {
            char c = ALPHABET.charAt(i);
            DECODE_TABLE[c] = i;
            DECODE_TABLE[Character.toLowerCase(c)] = i;
        }
    }

    private Base32() {}

    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder output = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                output.append(ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            output.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        while (output.length() % 8 != 0) {
            output.append('=');
        }
        return output.toString();
    }

    public static byte[] decode(String input) {
        if (input == null) return new byte[0];
        String clean = input;
        while (clean.endsWith("=")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        clean = clean.toUpperCase();

        ArrayList<Byte> output = new ArrayList<>();
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int code = (int) c;
            if (code >= DECODE_TABLE.length || DECODE_TABLE[code] == -1) continue;
            buffer = (buffer << 5) | DECODE_TABLE[code];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                output.add((byte) ((buffer >> bitsLeft) & 0xFF));
            }
        }
        byte[] result = new byte[output.size()];
        for (int i = 0; i < output.size(); i++) {
            result[i] = output.get(i);
        }
        return result;
    }
}
