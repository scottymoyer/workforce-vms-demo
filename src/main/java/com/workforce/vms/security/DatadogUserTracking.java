package com.workforce.vms.security;

import datadog.trace.api.interceptor.MutableSpan;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

/**
 * Attaches the authenticated user identity to the active Datadog trace so that APM
 * traces AND App &amp; API Protection security signals are attributed to a specific
 * account ("whose account is compromised") and can be blocked per-user.
 *
 * Pattern per Datadog docs (Add user info — Java): tag the LOCAL ROOT span with
 * usr.-prefixed keys. usr.id is mandatory. The dd-java-agent registers its tracer
 * with the OpenTracing GlobalTracer at runtime, so activeSpan() is the request span.
 *
 * Docs: https://docs.datadoghq.com/security/application_security/how-it-works/add-user-info/
 */
public final class DatadogUserTracking {

    private DatadogUserTracking() { }

    public static void setUser(String userId, String email, String role) {
        if (userId == null) {
            return;
        }
        Span span = GlobalTracer.get().activeSpan();
        if (span instanceof MutableSpan) {
            MutableSpan localRoot = ((MutableSpan) span).getLocalRootSpan();
            localRoot.setTag("usr.id", userId);      // mandatory
            if (email != null) {
                localRoot.setTag("usr.email", email);
            }
            if (role != null) {
                localRoot.setTag("usr.role", role);
            }
        }
    }
}
