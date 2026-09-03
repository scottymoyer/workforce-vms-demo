package com.workforce.vms.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Password hashing utility.
 *
 * DEMO-VULN: Weak cryptographic hash (CWE-327 / CWE-916). Passwords are hashed with
 * unsalted MD5, which is fast and broken for password storage. Datadog Static Analysis
 * (SAST) flags the use of MessageDigest.getInstance("MD5") for a security-sensitive
 * value. Reachable at runtime through AuthController#login.
 */
public final class PasswordHasher {

    private PasswordHasher() { }

    public static String md5(String input) {
        try {
            // DEMO-VULN: MD5 used for password hashing (weak, unsalted).
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return String.format("%032x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
