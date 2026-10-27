package com.byga.smartoverlay.ui

import android.app.ActivityManager
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.byga.smartoverlay.BuildConfig
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.performance.UiPerformance

@Composable fun PerformanceControls() {
    val context=LocalContext.current
    var summary by remember { mutableStateOf("") }
    PremiumCard {
        Text("Performa & versi",style=MaterialTheme.typography.titleMedium)
        Text("${BuildConfig.VERSION_NAME} • ${BuildConfig.BUILD_TYPE}")
        Text("Frame diukur lokal ketika jendela BYGA terlihat. Tidak merekam isi layar atau API key.",color=Muted)
        TextButton(onClick={
            val s=UiPerformance.frames.snapshot()
            val memory=context.getSystemService(ActivityManager::class.java)
            summary="BYGA ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})\n${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}\nHeap ${memory.memoryClass} MB / low RAM ${memory.isLowRamDevice}\nFrame ${s.frames}; >32ms ${s.over32ms}; >700ms ${s.frozen}; p95 ${s.p95ms}ms; maksimum ${s.maxMs}ms"
        }) { Text("Lihat hasil pengukuran") }
        if(summary.isNotBlank()){Text(summary);TextButton(onClick={Handoffs.copy(context,summary)}){Text("Salin diagnostik performa")}}
        TextButton(onClick={UiPerformance.frames.reset();summary="Pengukuran direset. Gunakan aplikasi lalu lihat hasil kembali."}){Text("Mulai pengukuran baru")}
    }
}
