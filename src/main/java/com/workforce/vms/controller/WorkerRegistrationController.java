package com.workforce.vms.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Worker registration via form/param data binding.
 *
 * SPRING4SHELL (spring-beans / spring-webmvc 5.3.17, CVE-2022-22965).
 * POST /api/workers/register  (form params bound to WorkerForm)
 * Spring's parameter data binding on a POJO is the exact reachable surface for
 * Spring4Shell: attacker-controlled "class.module.classLoader.*" binding paths on
 * the vulnerable spring-beans version allow overwriting Tomcat's AccessLogValve to
 * drop a webshell. Datadog SCA confirms the vulnerable library is loaded + executed
 * as soon as this binding path runs.
 */
@RestController
public class WorkerRegistrationController {

    /** Command object populated by Spring data binding (the Spring4Shell surface). */
    public static class WorkerForm {
        private String name;
        private String title;
        private String vendorName;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getVendorName() { return vendorName; }
        public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    }

    @PostMapping("/api/workers/register")
    public Map<String, Object> register(@ModelAttribute WorkerForm form) {
        // DEMO-VULN: Spring4Shell (CVE-2022-22965) — reachable via POJO data binding
        // on the pinned-vulnerable spring-beans 5.3.17.
        Map<String, Object> result = new HashMap<>();
        result.put("registered", true);
        result.put("name", form.getName());
        result.put("title", form.getTitle());
        result.put("vendorName", form.getVendorName());
        return result;
    }
}
