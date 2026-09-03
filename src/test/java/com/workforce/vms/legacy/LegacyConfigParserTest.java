package com.workforce.vms.legacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the billing-reconciliation config loader.
 *
 * This test PASSES on SnakeYAML 1.30. It is the tripwire for the upgrade blast-radius
 * scenario: bumping snakeyaml to 2.0 breaks LegacyConfigParser's compilation
 * (single-arg Constructor(Class) removed), so this test module fails to build — the
 * CI quality gate catches the regression pre-merge. See MITIGATION.md.
 */
class LegacyConfigParserTest {

    @Test
    void parsesReconciliationConfig() {
        String yaml = "schedule: \"0 2 * * *\"\n"
                + "batchSize: 500\n"
                + "autoApprove: false\n";

        LegacyConfigParser.ReconciliationConfig config = new LegacyConfigParser().parse(yaml);

        assertEquals("0 2 * * *", config.schedule);
        assertEquals(500, config.batchSize);
        assertTrue(!config.autoApprove);
    }
}
