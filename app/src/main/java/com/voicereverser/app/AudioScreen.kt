package com.voicereverser.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    val withMic = rememberWithMicPermission { vm.say("لازم تسمح بصلاحية المايك عشان التسجيل") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFile(uri)
    }
    val clip = vm.current

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("تسجيل", fontSize = 18.sp) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("رفع ملف", fontSize = 18.sp) })
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val phrase = vm.challenge
            if (phrase != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D6))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("التحدي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(phrase, fontSize = 20.sp)
                        }
                        IconButton(onClick = { vm.challenge = null }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                }
            }

            if (tab == 0) {
                BigCard(
                    title = if (vm.isRecording) "إيقاف التسجيل" else "ابدأ التسجيل",
                    subtitle = if (vm.isRecording) fmt(vm.recordMs) else null,
                    icon = if (vm.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    brush = RedBrush
                ) {
                    if (vm.isRecording) vm.stopRecording() else withMic { vm.startRecording() }
                }
            } else {
                BigCard(
                    title = if (vm.loading) "جارٍ التحميل..." else "اختر ملف صوتي",
                    subtitle = "mp3 / m4a / wav ...",
                    icon = Icons.Filled.UploadFile,
                    brush = OrangeBrush
                ) {
                    if (!vm.loading) picker.launch(arrayOf("audio/*", "video/*"))
                }
            }

            if (clip != null) {
                Text(
                    "الصوت الحالي: ${vm.currentSource} • ${fmt(clip.durationMs)}",
                    fontSize = 15.sp,
                    color = Color.DarkGray
                )
            }

            BigCard(
                title = if (vm.playingKey == "normal") "إيقاف" else "تشغيل",
                subtitle = "عادي",
                icon = if (vm.playingKey == "normal") Icons.Filled.Stop else Icons.Filled.PlayArrow,
                brush = GreenBrush,
                enabled = clip != null
            ) { vm.playNormal() }

            BigCard(
                title = if (vm.playingKey == "reversed") "إيقاف" else "عكس",
                subtitle = "تشغيل معكوس",
                icon = if (vm.playingKey == "reversed") Icons.Filled.Stop else Icons.Filled.Replay,
                brush = BlueBrush,
                enabled = clip != null
            ) { vm.playReversed() }

            Text(
                "طريقة التحدي: ارفع الأغنية وثبّتها كمرجع، اسمعها معكوسة، قلّدها وسجّل صوتك، ثم اضغط قارن.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            val ref = vm.reference
            if (ref != null) {
                Text("المرجع المثبّت: ${fmt(ref.durationMs)}", fontSize = 14.sp, color = Color.DarkGray)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { vm.setReference() }, modifier = Modifier.weight(1f)) {
                    Text("ثبّت كمرجع")
                }
                FilledTonalButton(onClick = { vm.compare() }, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (vm.playingKey == "compare") Icons.Filled.Stop else Icons.Filled.CompareArrows,
                        null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("قارن")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { vm.saveReversed() }, modifier = Modifier.weight(1f)) {
                    Text("حفظ المعكوس")
                }
                FilledTonalButton(
                    onClick = { vm.prepareShareReversed { ShareUtil.share(context, it) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("مشاركة المعكوس")
                }
            }
        }
    }
}
