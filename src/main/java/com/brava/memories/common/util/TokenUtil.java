package com.brava.memories.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokenUtil() {}

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
