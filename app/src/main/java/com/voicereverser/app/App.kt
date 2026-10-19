package com.voicereverser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class NavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("الصوت", Icons.Filled.GraphicEq),
    NavItem("مؤثرات", Icons.Filled.RecordVoiceOver),
    NavItem("ملاحظات", Icons.Filled.Description),
    NavItem("السجل", Icons.Filled.History)
)

@Composable
fun App(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { Header(onSettings = { showSettings = true }) },
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> AudioScreen(vm)
                1 -> EffectsScreen(vm)
                2 -> NotesScreen(vm)
                else -> HistoryScreen(vm)
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("الإعدادات") },
            text = { Text("عاكس الصوت - النسخة 1.0\nكل المعالجة تتم على جهازك بدون إنترنت.") },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("إغلاق") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    showSettings = false
                }) { Text("مسح السجل") }
            }
        )
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF4C7DE7), Color(0xFF6FA0F2)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.GraphicEq, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "عاكس الصوت",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, "الإعدادات")
        }
    }
}
