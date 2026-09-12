package io.alopter.policy

import java.net.URI

object UriSafety {
    fun isAllowed(candidate: String, base: String): Boolean = runCatching {
        val candidateUri = URI(candidate)
        val baseUri = URI(base)
        candidateUri.scheme == baseUri.scheme && candidateUri.host == baseUri.host && effectivePort(candidateUri) == effectivePort(baseUri)
    }.getOrDefault(false)

    private fun effectivePort(uri: URI) = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
}
