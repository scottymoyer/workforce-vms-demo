package com.workforce.vms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Vendor operations: logo fetch (SSRF) and bulk import (SnakeYAML deserialization). */
@RestController
public class VendorController {

    /**
     * SERVER-SIDE REQUEST FORGERY.
     * GET /api/vendors/logo?url=https://example.com/logo.png
     * "Fetch a vendor's logo from a URL." The server makes an outbound request to a
     * fully user-controlled URL, enabling access to internal services / metadata.
     */
    @GetMapping("/api/vendors/logo")
    public Map<String, Object> fetchLogo(@RequestParam("url") String url) throws Exception {
        Map<String, Object> result = new HashMap<>();
        // DEMO-VULN: SSRF (CWE-918). No allowlist / scheme / host validation.
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        result.put("requestedUrl", url);
        result.put("status", code);
        try (InputStream in = conn.getInputStream()) {
            String body = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().limit(50).collect(Collectors.joining("\n"));
            result.put("preview", body.length() > 2000 ? body.substring(0, 2000) : body);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * INSECURE YAML DESERIALIZATION (SnakeYAML, CVE-2022-1471).
     * POST /api/vendors/import-yaml  (body: raw YAML)
     * Bulk-imports vendor records from a YAML document. SnakeYAML 1.30's default
     * constructor allows instantiation of arbitrary types via tags (e.g.
     * "!!javax.script.ScriptEngineManager"), leading to RCE.
     */
    @PostMapping("/api/vendors/import-yaml")
    public Map<String, Object> importYaml(@RequestBody String yamlBody) {
        Map<String, Object> result = new HashMap<>();
        // DEMO-VULN: SnakeYAML unsafe load (CVE-2022-1471). Default Yaml() constructor
        // permits arbitrary type instantiation from attacker-controlled tags.
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(yamlBody);
        result.put("imported", loaded);
        result.put("type", loaded == null ? "null" : loaded.getClass().getName());
        return result;
    }
}
