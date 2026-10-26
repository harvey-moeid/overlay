package com.byga.smartoverlay.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.byga.smartoverlay.overlay.OverlayAppearance
import com.byga.smartoverlay.data.settings
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable fun MascotSettings() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val stored by remember { context.settings.data.map { it[OverlayAppearance.sizeKey] ?: OverlayAppearance.DEFAULT_DP } }.collectAsStateWithLifecycle(initialValue=OverlayAppearance.DEFAULT_DP)
    var size by remember { mutableFloatStateOf(OverlayAppearance.DEFAULT_DP.toFloat()) }
    LaunchedEffect(stored) { size=OverlayAppearance.visualDp(stored).toFloat() }
    Text("Robot bubble BYGA",style=MaterialTheme.typography.titleMedium)
    RobotImage(size.dp,"Preview maskot robot BYGA biru",Modifier.size(size.dp))
    Text("Ukuran ${size.roundToInt()} dp • robot kecil, latar transparan. Area ketuk minimal 48 dp. Ketuk panel, geser/dock, tahan untuk Stop.")
    Slider(value=size,onValueChange={size=it},valueRange=24f..56f,steps=7,onValueChangeFinished={scope.launch { context.settings.edit { it[OverlayAppearance.sizeKey]=size.roundToInt() } }})
    TextButton(onClick={ scope.launch { context.settings.edit { it[OverlayAppearance.sizeKey]=OverlayAppearance.DEFAULT_DP } } }) { Text("Kembalikan kecil • 28 dp") }
    Text("Ukuran diperbarui saat bubble aktif. Kurangi gerak menonaktifkan animasi dock; gerak masuk juga mengikuti pengaturan animator Android.")
}
