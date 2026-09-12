package io.alopter.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessPolicyTest {
    @Test fun allowedAppCanSendOnlyWithForegroundContext() {
        assertEquals(AccessDecision.ALLOW, AccessPolicy.decide(AppAccessRule.ALLOWED, true))
        assertEquals(AccessDecision.BLOCK, AccessPolicy.decide(AppAccessRule.ALLOWED, false))
    }

    @Test fun newAppsAskAndNeverAllowBlocks() {
        assertEquals(AccessDecision.ASK, AccessPolicy.decide(AppAccessRule.ASK_EVERY_TIME, true))
        assertEquals(AccessDecision.BLOCK, AccessPolicy.decide(AppAccessRule.NEVER_ALLOW, true))
    }
}
