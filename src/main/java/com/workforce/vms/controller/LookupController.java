package com.workforce.vms.controller;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** String-interpolation lookup (Text4Shell) + a reflected-XSS greeting. */
@RestController
public class LookupController {

    /**
     * TEXT4SHELL (commons-text 1.9, CVE-2022-42889).
     * GET /api/lookup?query=Hello ${date:yyyy}
     * StringSubstitutor.createInterpolator() enables the "script", "dns" and "url"
     * lookups. On 1.9, "${script:javascript:...}" leads to RCE.
     */
    @GetMapping(value = "/api/lookup", produces = MediaType.TEXT_PLAIN_VALUE)
    public String lookup(@RequestParam("query") String query) {
        // DEMO-VULN: Text4Shell (CVE-2022-42889). Interpolating attacker-controlled input.
        StringSubstitutor interpolator = StringSubstitutor.createInterpolator();
        return interpolator.replace(query);
    }

    /**
     * REFLECTED XSS.
     * GET /api/greeting?name=<script>alert(1)</script>
     * The parameter is echoed into an HTML response without escaping.
     */
    @GetMapping(value = "/api/greeting", produces = MediaType.TEXT_HTML_VALUE)
    public String greeting(@RequestParam("name") String name) {
        // DEMO-VULN: Reflected XSS (CWE-79). Unescaped user input rendered as HTML.
        return "<html><body><h1>Welcome, " + name + "!</h1>"
                + "<p>Your workforce dashboard is loading…</p></body></html>";
    }
}
