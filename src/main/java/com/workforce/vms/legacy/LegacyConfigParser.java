package com.workforce.vms.legacy;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Internal config loader used by the (trusted) billing-reconciliation job.
 *
 * ── UPGRADE BLAST RADIUS (see MITIGATION.md) ─────────────────────────────────
 * This is the "second module in the monolith" that makes bumping SnakeYAML to the
 * CVE-2022-1471-fixed 2.0 line NOT a drop-in change:
 *
 *   • This file compiles against SnakeYAML 1.30 (the pinned, vulnerable version).
 *   • The single-arg  new Constructor(Class)  used below was REMOVED in SnakeYAML
 *     2.0 — the surviving overload requires a LoaderOptions argument.
 *   • Therefore upgrading snakeyaml 1.30 → 2.0 to fix the CVE breaks THIS module's
 *     compilation, even though the vulnerable sink is in VendorController.
 *
 * The failing build is caught pre-merge by the CI quality gate + the unit test
 * (LegacyConfigParserTest). A coding agent can analyze this diff's blast radius and
 * propose the corrected  new Constructor(AppConfig.class, new LoaderOptions())  call
 * before the fix ships. See MITIGATION.md for the before/after.
 *
 * NOTE: this parser only ever reads a trusted, in-repo config string — it is NOT the
 * attacker-reachable YAML sink. The reachable sink is VendorController#importYaml.
 */
public class LegacyConfigParser {

    /** Typed view of the internal reconciliation config. */
    public static class ReconciliationConfig {
        public String schedule;
        public int batchSize;
        public boolean autoApprove;
    }

    /**
     * Parses the internal reconciliation config into a typed object.
     *
     * The  new Constructor(ReconciliationConfig.class)  call is the API that changed
     * in SnakeYAML 2.0. Do not "fix" it to satisfy 2.0 without reading MITIGATION.md —
     * that IS the demo.
     */
    public ReconciliationConfig parse(String yaml) {
        // Single-arg Constructor(Class) — valid in 1.30, REMOVED in 2.0.
        Yaml parser = new Yaml(new Constructor(ReconciliationConfig.class));
        return parser.load(yaml);
    }
}
