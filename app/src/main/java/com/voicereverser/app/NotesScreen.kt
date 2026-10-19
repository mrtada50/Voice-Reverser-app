package com.voicereverser.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var partial by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    val withMic = rememberWithMicPermission { vm.say("لازم تسمح بصلاحية المايك") }

    val dictation = remember {
        Dictation(context, object : Dictation.Listener {
            override fun onPartial(t: String) {
                partial = t
            }

            override fun onFinal(t: String) {
                if (t.isNotBlank()) {
                    val old = vm.noteDraft
                    vm.noteDraft = if (old.isBlank()) t else old.trimEnd() + " " + t
                }
                partial = ""
            }

            override fun onError(message: String) {
                listening = false
                partial = ""
                vm.say(message)
            }

            override fun onState(l: Boolean) {
                listening = l
            }
        })
    }
    DisposableEffect(Unit) {
        onDispose { dictation.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ملاحظات صوتية", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = vm.noteDraft,
            onValueChange = { vm.noteDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            label = { Text("ملاحظتك") }
        )

        if (listening && partial.isNotBlank()) {
            Text(partial, color = Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (listening) dictation.stop() else withMic { dictation.start() }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(if (listening) Icons.Filled.Stop else Icons.Filled.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text(if (listening) "إيقاف" else "إملاء صوتي")
            }
            OutlinedButton(
                onClick = {
                    if (vm.noteDraft.isNotBlank()) {
                        vm.addNote(vm.noteDraft)
                        vm.noteDraft = ""
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("حفظ")
            }
        }

        Text(
            "الإملاء يشتغل أوفلاين إذا نزّلت حزمة اللغة العربية من إعدادات خدمات جوجل للكلام.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        val fmtDate = remember { SimpleDateFormat("d/M HH:mm", Locale.getDefault()) }
        vm.notes.forEach { n ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(n.text, fontSize = 17.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmtDate.format(Date(n.ts)), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(n.text))
                            vm.say("تم النسخ")
                        }) { Icon(Icons.Filled.ContentCopy, null) }
                        IconButton(onClick = { vm.deleteNote(n) }) { Icon(Icons.Filled.Delete, null) }
                    }
                }
            }
        }
    }
}
