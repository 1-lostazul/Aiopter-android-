package io.aiopter.policy

enum class AppAccessRule(val displayName: String) {
    ALLOWED("Allowed"),
    ASK_EVERY_TIME("Ask Every Time"),
    NEVER_ALLOW("Never Allow"),
}

enum class AccessDecision { ALLOW, ASK, BLOCK }

object AccessPolicy {
    fun decide(rule: AppAccessRule, hasForegroundContext: Boolean): AccessDecision = when {
        !hasForegroundContext -> AccessDecision.BLOCK
        rule == AppAccessRule.ALLOWED -> AccessDecision.ALLOW
        rule == AppAccessRule.ASK_EVERY_TIME -> AccessDecision.ASK
        else -> AccessDecision.BLOCK
    }

    fun acceptsOneTimeApproval(
        expectedPackage: String?,
        currentPackage: String?,
        requestedAtMillis: Long,
        approvedAtMillis: Long,
        maxAgeMillis: Long = 30_000,
    ): Boolean = !expectedPackage.isNullOrBlank() &&
        expectedPackage == currentPackage &&
        approvedAtMillis >= requestedAtMillis &&
        approvedAtMillis - requestedAtMillis <= maxAgeMillis
}
