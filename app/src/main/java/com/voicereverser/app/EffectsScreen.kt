package com.voicereverser.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EffectsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var alsoReverse by remember { mutableStateOf(false) }
    val clip = vm.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("مؤثرات الصوت", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        if (clip == null) {
            Text("ما اكو صوت حالياً. سجّل أو ارفع ملف من تبويب «الصوت» أول.", color = Color.Gray)
        } else {
            Text("الصوت الحالي: ${vm.currentSource} • ${fmt(clip.durationMs)}", color = Color.DarkGray)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = alsoReverse, onCheckedChange = { alsoReverse = it })
            Spacer(Modifier.width(10.dp))
            Text("اعكس الصوت بعد المؤثر")
        }

        VoiceEffect.values().toList().chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { e ->
                    val active = vm.playingKey == "fx:" + e.name
                    FilledTonalButton(
                        onClick = { vm.playEffect(e, alsoReverse) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(e.label, fontSize = 17.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (vm.lastFx != null) {
            Text("آخر مؤثر: ${vm.lastFxLabel}", color = Color.DarkGray)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { vm.saveFx() }, modifier = Modifier.weight(1f)) {
                    Text("حفظ")
                }
                FilledTonalButton(
                    onClick = { vm.prepareShareFx { ShareUtil.share(context, it) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("مشاركة")
                }
            }
        }
    }
}
