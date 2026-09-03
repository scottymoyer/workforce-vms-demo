package com.workforce.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Contingent-Workforce VMS — Datadog Code Security demo application.
 *
 * Runs under dd-java-agent.jar with APM tracing + the full Code Security runtime
 * surface (IAST, runtime SCA, App & API Protection). See docker-compose.yml and
 * the README demo runbook.
 */
@SpringBootApplication
public class WorkforceVmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkforceVmsApplication.class, args);
    }
}
