package io.aiopter.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UriSafetyTest {
    @Test fun acceptsOnlyExactOrigin() {
        assertTrue(UriSafety.isAllowed("https://api.aiopter.ai/auth/callback", "https://api.aiopter.ai"))
        assertFalse(UriSafety.isAllowed("https://api.aiopter.ai.evil.test/auth", "https://api.aiopter.ai"))
        assertFalse(UriSafety.isAllowed("http://api.aiopter.ai/auth", "https://api.aiopter.ai"))
    }
}
