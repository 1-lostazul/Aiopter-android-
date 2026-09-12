package io.alopter.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameSafetyTest {
    @Test fun blocksAndroidBlackedOutFrames() = assertTrue(FrameSafety.isProbablyProtected(IntArray(400) { 0xff000000.toInt() }))
    @Test fun permitsVisibleFrames() = assertFalse(FrameSafety.isProbablyProtected(IntArray(400) { 0xff245f91.toInt() }))
    @Test fun doesNotBlockMostlyVisibleDarkInterfaces() {
        val samples = IntArray(400) { if (it < 360) 0xff000000.toInt() else 0xff8adfff.toInt() }
        assertFalse(FrameSafety.isProbablyProtected(samples))
    }
}
