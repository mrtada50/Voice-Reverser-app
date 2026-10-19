package com.voicereverser.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrendingScreen(vm: MainViewModel, onTry: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تحديات جاهزة", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "اختر عبارة، قلّها بشكل يطلع معكوس، وصوّر النتيجة مع أصحابك.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        items(Challenges.list) { ch ->
            val levelColor = when (ch.level) {
                "سهل" -> Color(0xFF2E9E5B)
                "متوسط" -> Color(0xFFE08A1E)
                else -> Color(0xFFD64545)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ch.phrase, fontSize = 18.sp)
                        Text(ch.level, fontSize = 13.sp, color = levelColor, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = {
                        vm.challenge = ch.phrase
                        onTry()
                    }) { Text("جرّب") }
                }
            }
        }
    }
}
