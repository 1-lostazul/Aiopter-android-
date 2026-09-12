package io.alopter.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UriSafetyTest {
    @Test fun acceptsOnlyExactOrigin() {
        assertTrue(UriSafety.isAllowed("https://api.alopter.app/auth/callback", "https://api.alopter.app"))
        assertFalse(UriSafety.isAllowed("https://api.alopter.app.evil.test/auth", "https://api.alopter.app"))
        assertFalse(UriSafety.isAllowed("http://api.alopter.app/auth", "https://api.alopter.app"))
    }
}
