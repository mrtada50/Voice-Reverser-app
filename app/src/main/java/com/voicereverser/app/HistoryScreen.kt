package com.voicereverser.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun kindLabel(kind: String): String = when (kind) {
    "rec" -> "تسجيل"
    "rev" -> "معكوس"
    "fx" -> "مؤثر"
    else -> kind
}

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val fmtDate = remember { SimpleDateFormat("d/M  HH:mm", Locale.getDefault()) }

    if (vm.history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("السجل فاضي", color = Color.Gray, fontSize = 18.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.history, key = { it.file.name }) { item ->
            val playN = vm.playingKey == "h:" + item.file.name + ":n"
            val playR = vm.playingKey == "h:" + item.file.name + ":r"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(kindLabel(item.kind), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(
                                fmtDate.format(Date(item.timestamp)) + " • " + fmt(item.durationMs),
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { vm.playItem(item, false) }) {
                            Icon(if (playN) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                        }
                        IconButton(onClick = { vm.playItem(item, true) }) {
                            Icon(if (playR) Icons.Filled.Stop else Icons.Filled.Replay, null)
                        }
                        IconButton(onClick = { ShareUtil.share(context, item.file) }) {
                            Icon(Icons.Filled.Share, null)
                        }
                        IconButton(onClick = { vm.deleteItem(item) }) {
                            Icon(Icons.Filled.Delete, null)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { vm.useItem(item, false) }) { Text("استخدم كصوت حالي") }
                        TextButton(onClick = { vm.useItem(item, true) }) { Text("ثبّت كمرجع") }
                    }
                }
            }
        }
    }
}
