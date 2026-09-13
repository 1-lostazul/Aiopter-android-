package io.aiopter.policy

object FrameSafety {
    fun isProbablyProtected(samples: IntArray): Boolean {
        if (samples.size < 100) return false
        val dark = samples.count { pixel ->
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff
            red + green + blue < 24
        }
        return dark.toDouble() / samples.size >= 0.985
    }
}
