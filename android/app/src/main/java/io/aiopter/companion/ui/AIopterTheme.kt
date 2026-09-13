package io.aiopter.companion.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColorScheme(primary = Color(0xFF4CCBFF), secondary = Color(0xFF3A7BFF), background = Color(0xFF080B12), surface = Color(0xFF111722), onBackground = Color(0xFFF2F7FF), onSurface = Color(0xFFF2F7FF))
@Composable fun AIopterTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)
