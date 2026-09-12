package io.alopter.companion.access

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.alopter.policy.AppAccessRule
import kotlinx.coroutines.flow.first

private val Context.accessPolicyDataStore by preferencesDataStore(name = "app_access_rules")

class AccessPolicyStore(private val context: Context) {
    suspend fun ruleFor(packageName: String): AppAccessRule {
        if (packageName == context.packageName) return AppAccessRule.ALLOWED
        val stored = context.accessPolicyDataStore.data.first()[key(packageName)]
        return stored?.let { runCatching { AppAccessRule.valueOf(it) }.getOrNull() } ?: AppAccessRule.ASK_EVERY_TIME
    }

    suspend fun setRule(packageName: String, rule: AppAccessRule) {
        require(packageName.matches(PACKAGE_PATTERN))
        context.accessPolicyDataStore.edit { it[key(packageName)] = rule.name }
    }

    private fun key(packageName: String) = stringPreferencesKey("rule.$packageName")

    private companion object {
        val PACKAGE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_.]{1,254}$")
    }
}

class ForegroundAppResolver(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }

    fun currentPackage(): String? {
        if (!hasUsageAccess()) return null
        val usage = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val events = usage.queryEvents(end - LOOKBACK_MS, end)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = Long.MIN_VALUE
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= latestTimestamp) {
                latestTimestamp = event.timeStamp
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }

    fun labelFor(packageName: String): String = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private companion object { const val LOOKBACK_MS = 30_000L }
}
