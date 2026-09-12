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

    @Test fun oneTimeApprovalMustMatchTheCurrentAppAndRemainFresh() {
        assertEquals(true, AccessPolicy.acceptsOneTimeApproval("com.example.one", "com.example.one", 1_000, 30_999))
        assertEquals(false, AccessPolicy.acceptsOneTimeApproval("com.example.one", "com.example.two", 1_000, 2_000))
        assertEquals(false, AccessPolicy.acceptsOneTimeApproval("com.example.one", "com.example.one", 1_000, 31_001))
        assertEquals(false, AccessPolicy.acceptsOneTimeApproval(null, "com.example.one", 1_000, 2_000))
    }
}
