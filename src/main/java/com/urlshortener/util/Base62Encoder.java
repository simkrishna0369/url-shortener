package com.urlshortener.util;

public final class Base62Encoder {

    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62Encoder() {
    }

    public static String encode(long value, int minLength) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
        if (minLength < 1) {
            throw new IllegalArgumentException("minLength must be at least 1");
        }
        if (value == 0) {
            return pad("0", minLength);
        }
        StringBuilder encoded = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            int digit = (int) (remaining % BASE);
            encoded.append(ALPHABET.charAt(digit));
            remaining /= BASE;
        }
        return pad(encoded.reverse().toString(), minLength);
    }

    private static String pad(String value, int minLength) {
        if (value.length() >= minLength) {
            return value;
        }
        return "0".repeat(minLength - value.length()) + value;
    }
}
