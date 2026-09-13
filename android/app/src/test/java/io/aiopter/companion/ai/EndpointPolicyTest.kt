package io.aiopter.companion.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EndpointPolicyTest {
    @Test
    fun releaseAcceptsOnlyTheOfficialHttpsOrigin() {
        assertEquals("https://api.aiopter.ai", EndpointPolicy.requireAllowed("https://api.aiopter.ai/", false))
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("http://api.aiopter.ai", false) }
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("https://api.aiopter.ai.evil.test", false) }
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("https://api.aiopter.ai/v1", false) }
    }

    @Test
    fun debugCleartextIsLimitedToLocalDevelopmentHosts() {
        assertEquals("http://10.0.2.2:3001", EndpointPolicy.requireAllowed("http://10.0.2.2:3001", true))
        assertEquals("http://localhost:3001", EndpointPolicy.requireAllowed("http://localhost:3001", true))
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("http://api.aiopter.ai", true) }
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("http://192.168.1.10:3001", true) }
    }

    @Test
    fun credentialsAndUnexpectedUrlComponentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("https://user:pass@api.aiopter.ai", false) }
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.requireAllowed("https://api.aiopter.ai?token=value", false) }
    }
}
