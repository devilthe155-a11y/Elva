package com.elva.music.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ElvaViolet = Color(0xFF9B7BFF)
val ElvaBg = Color(0xFF0E0B16)
val ElvaSurface = Color(0xFF1A1526)

@Composable
fun ElvaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = ElvaViolet, onPrimary = Color.Black,
        background = ElvaBg, surface = ElvaSurface,
        surfaceVariant = Color(0xFF261F38), onSurface = Color(0xFFF1EDFA),
        onSurfaceVariant = Color(0xFFB3ABC8),
    ),
    content = content,
)
