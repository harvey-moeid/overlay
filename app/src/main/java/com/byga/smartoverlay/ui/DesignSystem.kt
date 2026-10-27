package com.byga.smartoverlay.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byga.smartoverlay.R

val Ink=Color(0xFF080D19)
val Blue=Color(0xFF5787FF)
val Cyan=Color(0xFF72E8E2)
val Muted=Color(0xFF9CAAC6)
val Line=Color(0xFF23314C)
@Composable fun BygaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme=darkColorScheme(primary=Blue,onPrimary=Color.White,primaryContainer=Color(0xFF1F3E77),secondary=Cyan,background=Ink,surface=Color(0xFF101B2D),surfaceVariant=Color(0xFF162239),outline=Line,onBackground=Color(0xFFF1F5FF),onSurface=Color(0xFFF1F5FF),onSurfaceVariant=Muted),
        typography=Typography(headlineLarge=androidx.compose.ui.text.TextStyle(fontSize=34.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp),headlineSmall=androidx.compose.ui.text.TextStyle(fontSize=26.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-0.5).sp),titleLarge=androidx.compose.ui.text.TextStyle(fontSize=22.sp,fontWeight=FontWeight.SemiBold),titleMedium=androidx.compose.ui.text.TextStyle(fontSize=16.sp,fontWeight=FontWeight.SemiBold),bodyLarge=androidx.compose.ui.text.TextStyle(fontSize=16.sp,lineHeight=24.sp),bodyMedium=androidx.compose.ui.text.TextStyle(fontSize=14.sp,lineHeight=21.sp),labelLarge=androidx.compose.ui.text.TextStyle(fontSize=14.sp,fontWeight=FontWeight.SemiBold)),
        shapes=Shapes(extraSmall=RoundedCornerShape(10.dp),small=RoundedCornerShape(14.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(26.dp),extraLarge=RoundedCornerShape(32.dp)),content=content)
}
data class Destination(val id: String,val title: String,val caption: String,val icon: ImageVector)
val destinations=listOf(
    Destination("Home","Beranda","Ruang kendali Anda",Icons.Default.Home),
    Destination("Chat","AI Chat","Ide menjadi karya",Icons.AutoMirrored.Filled.Send),
    Destination("Workflows","Workflow Studio","Rancang, tinjau, jalankan",Icons.Default.List),
    Destination("Smart Mapping","Smart Mapping","Deteksi, tunjuk, tinjau tombol",Icons.Default.Build),
    Destination("Media & Social","Media & Social","Dari draft ke aplikasi tujuan",Icons.Default.Favorite),
    Destination("Agent Experimental","Agent Lab","Scope & kendali experimental",Icons.Default.Build),
    Destination("AI Providers","AI Providers","Model, key & koneksi",Icons.Default.Settings),
    Destination("Apps","Aplikasi","Launcher & profil tujuan",Icons.Default.Phone),
    Destination("Accounts","Akun","Identitas tujuan yang jelas",Icons.Default.AccountCircle),
    Destination("Scheduler","Jadwal","Pengingat yang menghormati Android",Icons.Default.DateRange),
    Destination("Studio","Prompt Library","Simpan ide terbaik",Icons.Default.Edit),
    Destination("Monitor","Live Monitor","Status & checkpoint",Icons.Default.Info),
    Destination("Capture","Screen Capture","Satu sesi, selalu dengan izin",Icons.Default.Phone),
    Destination("Mulai","Izin & Onboarding","Siapkan perangkat",Icons.Default.CheckCircle),
    Destination("Diagnostik","Diagnostics","Bukti uji, bukan asumsi",Icons.Default.Build),
    Destination("Settings","Pengaturan","Tampilan, privasi & data",Icons.Default.Settings)
)
@Composable fun Eyebrow(text: String) { Text(text.uppercase(),style=MaterialTheme.typography.labelSmall.copy(letterSpacing=2.sp),color=Cyan) }
@Composable fun StatusPill(text: String, good: Boolean=true) {
    Surface(color=(if(good) Cyan else Color(0xFFFFC77C)).copy(alpha=0.10f),shape=RoundedCornerShape(30.dp),border=BorderStroke(1.dp,(if(good) Cyan else Color(0xFFFFC77C)).copy(alpha=0.22f))) {
        Text(text,Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelSmall,color=if(good) Cyan else Color(0xFFFFC77C))
    }
}
@Composable fun SectionTitle(title: String, subtitle: String?=null) {
    Column(Modifier.padding(top=10.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,style=MaterialTheme.typography.titleLarge); subtitle?.let { Text(it,color=Muted,style=MaterialTheme.typography.bodyMedium) } }
}
@Composable fun PremiumCard(modifier: Modifier=Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF101B2D)),border=BorderStroke(1.dp,Line)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content) }
}
@Composable fun EmptyState(title: String, detail: String, action: String?=null, onAction: () -> Unit={}) {
    PremiumCard { Icon(Icons.Default.Star,null,tint=Cyan,modifier=Modifier.size(30.dp)); Text(title,style=MaterialTheme.typography.titleMedium); Text(detail,color=Muted); action?.let { TextButton(onClick=onAction) { Text(it); Icon(Icons.AutoMirrored.Filled.ArrowForward,null,modifier=Modifier.size(16.dp)) } } }
}
@Composable fun Dashboard(overlay: Boolean, accessibility: Boolean, providerCount: Int, workflowCount: Int, accountCount: Int, bubbleRunning: Boolean, onNavigate:(String)->Unit,onBubble:()->Unit,modifier:Modifier=Modifier) {
    val workspaceRows=remember { destinations.filter { it.id in setOf("Chat","Workflows","Media & Social","AI Providers","Capture","Agent Experimental","Smart Mapping") }.chunked(2) }
    LazyColumn(modifier.testTag("dashboard-list"),contentPadding=PaddingValues(top=4.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    item("hero") {
    Box(Modifier.fillMaxWidth().cachedTileLayer().background(Brush.linearGradient(listOf(Color(0xFF17325A),Color(0xFF111C34),Color(0xFF102B37))),RoundedCornerShape(30.dp)).border(1.dp,Color(0xFF35578A),RoundedCornerShape(30.dp)).padding(22.dp)) {
        Column(Modifier.fillMaxWidth(0.67f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Eyebrow("Your everyday copilot")
            Text("Ide besar.\nKendali penuh.",style=MaterialTheme.typography.headlineLarge)
            Text("Buat konten, rangkai workflow, dan bawa draft ke aplikasi favorit.",style=MaterialTheme.typography.bodyMedium,color=Color(0xFFBECFEA))
            Button(onClick={onNavigate("Chat")},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF246BFD))) { Text("Mulai berkarya"); Icon(Icons.AutoMirrored.Filled.ArrowForward,null,modifier=Modifier.padding(start=8.dp).size(18.dp)) }
        }
        RobotImage(124.dp,"Maskot BYGA",Modifier.align(Alignment.CenterEnd).offset(x=16.dp,y=(-2).dp).size(124.dp))
    }
    }
    item("counts") {
    Row(Modifier.fillMaxWidth().cachedTileLayer(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("$providerCount" to "Provider","$workflowCount" to "Workflow","$accountCount" to "Akun").forEach { (n,label) -> Card(Modifier.weight(1f),border=BorderStroke(1.dp,Line),colors=CardDefaults.cardColors(containerColor=Color(0xFF101B2D))) { Column(Modifier.padding(14.dp)) { Text(n,style=MaterialTheme.typography.headlineSmall,color=Cyan); Text(label,color=Muted,style=MaterialTheme.typography.labelMedium) } } }
    }
    }
    item("bubble") {
    PremiumCard(Modifier.cachedTileLayer()) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Column { Text("Robot bubble",style=MaterialTheme.typography.titleMedium); Text(if(overlay) "Siap menemani di atas aplikasi" else "Berikan izin overlay untuk memulai",color=Muted,style=MaterialTheme.typography.bodySmall) }; Switch(checked=bubbleRunning, onCheckedChange={if(!overlay)onNavigate("Mulai") else onBubble()}) }; Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { StatusPill(if(overlay) "Overlay diizinkan" else "Izin diperlukan",overlay); StatusPill(if(accessibility) "Accessibility aktif" else "Mode manual",accessibility) }; Button(onClick=if(overlay) onBubble else ({onNavigate("Mulai")})) { Text(if(!overlay) "Siapkan izin" else if(bubbleRunning) "Tutup bubble" else "Tampilkan bubble") } }
    }
    item("workspace-title") { SectionTitle("Ruang kerja", "Semua yang Anda perlukan, dalam satu tempat.") }
    items(workspaceRows,key={it.first().id},contentType={"destinations"}) { pair -> Row(Modifier.cachedTileLayer(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { pair.forEach { d -> Card(onClick={onNavigate(d.id)},modifier=Modifier.weight(1f),border=BorderStroke(1.dp,Line),colors=CardDefaults.cardColors(containerColor=Color(0xFF101B2D))) { Column(Modifier.padding(18.dp).heightIn(min=105.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Icon(d.icon,null,tint=Cyan,modifier=Modifier.size(26.dp)); Text(d.title,style=MaterialTheme.typography.titleMedium); Text(d.caption,color=Muted,style=MaterialTheme.typography.bodySmall) } } } } }
    item("footer") { Text("LOCAL FIRST  •  PRIVATE BY DESIGN",color=Muted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(vertical=12.dp)) }
    }
}

/** Cache each visible tile's rasterization while LazyColumn only changes its position.
 * Scope layers to lazy items: gradients, rounded clips and glyphs need not be redrawn
 * on every scroll frame, and offscreen items release their layers with composition.
 * State changes (counts, permissions, switches, ripples) still invalidate the layer.
 */
internal fun Modifier.cachedTileLayer() = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}
