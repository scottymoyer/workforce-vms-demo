package com.workforce.vms.config;

/**
 * Centralized "secrets" for the demo.
 *
 * DEMO-VULN: Hardcoded secret (CWE-798). A real API key and the JWT signing key
 * are committed in source instead of being injected from a secrets manager. Datadog
 * Static Analysis (SAST) flags the hardcoded credential; the same value is reachable
 * at runtime through /login (JWT signing) and the vendor-billing integration stub.
 */
public final class DemoSecrets {

    private DemoSecrets() { }

    // DEMO-VULN: Hardcoded API key committed to source control.
    public static final String BILLING_API_KEY = "sk_live_9f8c2a1b7e4d6f30a1c5b9e2d4f6a8c0";

    // DEMO-VULN: Hardcoded JWT signing secret (should come from a vault / KMS).
    public static final String JWT_SIGNING_SECRET =
            "workforce-vms-demo-super-secret-signing-key-do-not-use-in-prod-0123456789";
}
