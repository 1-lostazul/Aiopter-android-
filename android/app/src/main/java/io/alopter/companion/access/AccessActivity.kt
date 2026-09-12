package io.alopter.companion.access

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.alopter.companion.ui.AlopterTheme
import io.alopter.policy.AppAccessRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(val label: String, val packageName: String, val rule: AppAccessRule)

@OptIn(ExperimentalMaterial3Api::class)
class AccessActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private val resolver by lazy { ForegroundAppResolver(this) }
    private val store by lazy { AccessPolicyStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlopterTheme {
                val usageAccess = remember(refresh) { resolver.hasUsageAccess() }
                var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) { apps = loadApps() }
                Scaffold(topBar = { TopAppBar(title = { Text("Apps & Access") }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                        Text("Choose what Alopter may send while screen sharing is on. New apps always ask first.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.large) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (usageAccess) "App awareness is on" else "App awareness is required", fontWeight = FontWeight.Bold)
                                    Text(if (usageAccess) "Alopter can enforce the rule for the app beneath the overlay." else "Without this access, screen frames stay blocked.", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text(if (usageAccess) "Review" else "Enable") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(apps, key = { it.packageName }) { app ->
                                AppRuleCard(app) { rule ->
                                    scope.launch {
                                        store.setRule(app.packageName, rule)
                                        apps = apps.map { if (it.packageName == app.packageName) it.copy(rule = rule) else it }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
    }

    private suspend fun loadApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
        } else @Suppress("DEPRECATION") packageManager.queryIntentActivities(launcher, 0)
        resolved.mapNotNull { info ->
            val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
            if (packageName == this@AccessActivity.packageName) return@mapNotNull null
            InstalledApp(info.loadLabel(packageManager).toString(), packageName, store.ruleFor(packageName))
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}

@Composable
private fun AppRuleCard(app: InstalledApp, onRule: (AppAccessRule) -> Unit) {
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(app.label, fontWeight = FontWeight.SemiBold)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppAccessRule.entries.forEach { rule ->
                    FilterChip(selected = app.rule == rule, onClick = { onRule(rule) }, label = { Text(rule.displayName, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
