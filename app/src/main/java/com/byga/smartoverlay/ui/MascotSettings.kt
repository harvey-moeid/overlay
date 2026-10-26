package com.byga.smartoverlay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.byga.smartoverlay.R
import com.byga.smartoverlay.data.settings
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable fun MascotSettings() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val stored by remember { context.settings.data.map { it[intPreferencesKey("bubble_size_dp")] ?: 80 } }.collectAsState(initial=80)
    var size by remember { mutableFloatStateOf(80f) }
    LaunchedEffect(stored) { size=stored.coerceIn(56,128).toFloat() }
    Text("Robot bubble BYGA",style=MaterialTheme.typography.titleMedium)
    Image(painterResource(R.drawable.byga_robot),contentDescription="Preview maskot robot BYGA biru",modifier=Modifier.size(size.dp))
    Text("Ukuran ${size.roundToInt()} dp • latar transparan. Ketuk panel, geser/dock, tahan untuk Stop.")
    Slider(value=size,onValueChange={size=it},valueRange=56f..128f,steps=8,onValueChangeFinished={scope.launch { context.settings.edit { it[intPreferencesKey("bubble_size_dp")]=size.roundToInt() } }})
    Text("Ukuran diperbarui saat bubble aktif. Kurangi gerak menonaktifkan animasi dock; gerak masuk juga mengikuti pengaturan animator Android.")
}
