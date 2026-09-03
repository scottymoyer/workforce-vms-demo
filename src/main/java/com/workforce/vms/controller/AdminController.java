package com.workforce.vms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Operational/admin endpoints (network diagnostics + config restore). */
@RestController
public class AdminController {

    /**
     * COMMAND INJECTION.
     * GET /api/admin/ping?host=8.8.8.8
     * A "network reachability" check that shells out with the user-supplied host,
     * so "8.8.8.8; cat /etc/passwd" runs arbitrary commands.
     */
    @GetMapping("/api/admin/ping")
    public Map<String, Object> ping(@RequestParam("host") String host) throws Exception {
        Map<String, Object> result = new HashMap<>();
        // DEMO-VULN: OS Command Injection (CWE-78). Untrusted input in a shell command.
        String[] cmd = {"/bin/sh", "-c", "ping -c 1 " + host};
        Process proc = Runtime.getRuntime().exec(cmd);
        String output = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        proc.waitFor();
        result.put("host", host);
        result.put("output", output);
        return result;
    }

    /**
     * INSECURE DESERIALIZATION.
     * POST /api/admin/restore-config  (body: base64-encoded Java serialized object)
     * "Restore a saved configuration blob." Deserializing untrusted data with
     * ObjectInputStream + commons-collections 3.2.1 on the classpath enables RCE
     * via the InvokerTransformer gadget chain.
     */
    @PostMapping("/api/admin/restore-config")
    public Map<String, Object> restoreConfig(@RequestBody String base64Blob) throws Exception {
        Map<String, Object> result = new HashMap<>();
        byte[] raw = Base64.getDecoder().decode(base64Blob.trim());
        // DEMO-VULN: Insecure Deserialization (CWE-502). readObject() on untrusted bytes.
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(raw))) {
            Object obj = ois.readObject();
            result.put("restored", String.valueOf(obj));
            result.put("type", obj == null ? "null" : obj.getClass().getName());
        }
        return result;
    }
}
