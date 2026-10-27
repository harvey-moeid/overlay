package com.byga.smartoverlay.scheduler

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.work.*
import com.byga.smartoverlay.MainActivity
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.workflow.WorkflowCodec
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class ScheduleConfig(val id:String,val workflowId:String,val name:String,val runAt:Long,val recurringMinutes:Long,val priority:Int,val dependency:String="",val enabled:Boolean=true) {
    fun encode()=JSONObject().put("workflow",workflowId).put("name",name).put("runAt",runAt).put("recurringMinutes",recurringMinutes).put("priority",priority).put("dependency",dependency).put("enabled",enabled).toString()
    companion object{fun decode(row:Record):ScheduleConfig{val j=JSONObject(row.body);return ScheduleConfig(row.id,j.getString("workflow"),j.getString("name"),j.getLong("runAt"),j.optLong("recurringMinutes"),j.optInt("priority",1),j.optString("dependency"),j.optBoolean("enabled",true))}}
}
object SchedulePolicy {
    fun valid(c:ScheduleConfig, now:Long)=c.workflowId.isNotBlank()&&c.name.isNotBlank()&&c.priority in 0..2&&c.dependency!=c.id&&(c.recurringMinutes==0L&&c.runAt>now||c.recurringMinutes in 15..525600)
    fun dependencyReady(dependency:String, state:String?)=dependency.isBlank()||state=="USER_COMPLETED"
}
class ScheduleWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result{
        val id=inputData.getString("schedule")?:return Result.failure();val dao=BygaDatabase.get(applicationContext).records()
        val config=dao.get("schedule_config",id)?.let{runCatching{ScheduleConfig.decode(it)}.getOrNull()}?:return Result.failure()
        if(!config.enabled)return Result.success()
        if(dao.get("workflow",config.workflowId)==null){dao.save(Record("schedule",id,"BLOCKED_WORKFLOW_MISSING"));notifyUser(id,"Workflow tidak lagi tersedia. Tinjau jadwal.");return Result.failure()}
        val dependency=if(config.dependency.isBlank())null else dao.get("schedule",config.dependency)?.body
        if(!SchedulePolicy.dependencyReady(config.dependency,dependency)){
            dao.save(Record("schedule",id,"WAITING_FOR_DEPENDENCY"))
            return if(runAttemptCount<5)Result.retry()else{notifyUser(id,"Dependensi belum dikonfirmasi selesai. Tinjau antrean.");Result.success()}
        }
        dao.save(Record("schedule",id,"WAITING_FOR_USER"))
        notifyUser(id,"${config.name} siap ditinjau. Tidak membuka UI atau menjalankan aksi diam-diam.")
        return Result.success()
    }
    private fun notifyUser(id:String,detail:String){
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("schedule","Pengingat workflow",NotificationManager.IMPORTANCE_DEFAULT))
        val open=PendingIntent.getActivity(applicationContext,id.hashCode(),Intent(applicationContext,MainActivity::class.java).putExtra("open_schedule",true),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id.hashCode(),Notification.Builder(applicationContext,"schedule").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("BYGA • Butuh tindakan pengguna").setContentText(detail).setContentIntent(open).setAutoCancel(true).build())
    }
    companion object{
        fun enqueue(context:Context,c:ScheduleConfig){
            val data=workDataOf("schedule" to c.id)
            if(c.recurringMinutes>0)WorkManager.getInstance(context).enqueueUniquePeriodicWork("byga-${c.id}",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<ScheduleWorker>(c.recurringMinutes,TimeUnit.MINUTES).setInputData(data).setBackoffCriteria(BackoffPolicy.LINEAR,15,TimeUnit.MINUTES).build())
            else WorkManager.getInstance(context).enqueueUniqueWork("byga-${c.id}",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<ScheduleWorker>().setInitialDelay((c.runAt-System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS).setInputData(data).setBackoffCriteria(BackoffPolicy.LINEAR,15,TimeUnit.MINUTES).build())
        }
        fun cancel(context:Context,id:String)=WorkManager.getInstance(context).cancelUniqueWork("byga-$id")
    }
}
@Composable fun ScheduleStudio(onWorkflows:()->Unit){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{BygaDatabase.get(context).records()}
    val rows by remember{dao.observe("schedule_config")}.collectAsStateWithLifecycle(initialValue=emptyList());val states by remember{dao.observe("schedule")}.collectAsStateWithLifecycle(initialValue=emptyList());val workflows by remember{dao.observe("workflow")}.collectAsStateWithLifecycle(initialValue=emptyList())
    val configs=rows.mapNotNull{runCatching{ScheduleConfig.decode(it)}.getOrNull()}.sortedByDescending{it.priority}
    var name by remember{mutableStateOf("")};var workflow by remember{mutableStateOf("")};var recurring by remember{mutableStateOf(false)};var interval by remember{mutableStateOf("15")};var priority by remember{mutableIntStateOf(1)};var dependency by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
    var date by remember{mutableStateOf(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).plusMinutes(15))}
    val formatter=remember{DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm 'WIB'")}
    PremiumCard{Eyebrow("Make room for what matters");Text("Rencana hari ini,\nsiap saat Anda siap.",style=MaterialTheme.typography.headlineSmall);Text("WorkManager mengirim pengingat; waktu dapat ditunda Android. Tidak menjalankan UI, unlock, atau posting otomatis.",color=Muted)}
    if(workflows.isEmpty())EmptyState("Simpan workflow pertama", "Jadwal mengacu pada workflow yang tersimpan, bukan alur tersembunyi.","Buka Workflow Studio",onWorkflows)
    Row(Modifier.horizontalScroll(rememberScrollState())){workflows.forEach{row->val p=runCatching{WorkflowCodec.decode(row.body)}.getOrNull();if(p!=null)FilterChip(workflow==row.id,{workflow=row.id;if(name.isBlank())name=p.name},label={Text(p.name)})}}
    OutlinedTextField(name,{name=it},label={Text("Nama jadwal")},modifier=Modifier.fillMaxWidth())
    Row{Checkbox(recurring,{recurring=it});Text("Berulang")}
    if(recurring)OutlinedTextField(interval,{interval=it},label={Text("Interval menit (minimal 15)")},modifier=Modifier.fillMaxWidth())else OutlinedButton(onClick={DatePickerDialog(context,{_,y,m,d->val picked=date.withYear(y).withMonth(m+1).withDayOfMonth(d);TimePickerDialog(context,{_,hour,minute->date=picked.withHour(hour).withMinute(minute).withSecond(0)},date.hour,date.minute,true).show()},date.year,date.monthValue-1,date.dayOfMonth).show()}){Text(formatter.format(date))}
    Text("Prioritas antrean di aplikasi (bukan janji urutan eksekusi OS)",color=Muted)
    Row{listOf(0 to "Rendah",1 to "Normal",2 to "Tinggi").forEach{(p,label)->FilterChip(priority==p,{priority=p},label={Text(label)})}}
    Text("Dependensi: pengguna harus menandai jadwal lain selesai sebelum pengingat ini siap.",color=Muted,style=MaterialTheme.typography.bodySmall)
    Row(Modifier.horizontalScroll(rememberScrollState())){FilterChip(dependency.isBlank(),{dependency=""},label={Text("Tanpa dependensi")});PagedItems(configs){c->FilterChip(dependency==c.id,{dependency=c.id},label={Text(c.name)})}}
    Button(onClick={val c=ScheduleConfig(UUID.randomUUID().toString(),workflow,name,date.toInstant().toEpochMilli(),if(recurring)interval.toLongOrNull()?:-1 else 0,priority,dependency);if(!SchedulePolicy.valid(c,System.currentTimeMillis()))note="Pilih workflow, nama dan waktu/interval yang valid"else scope.launch{dao.save(Record("schedule_config",c.id,c.encode()));dao.save(Record("schedule",c.id,"ENQUEUED"));ScheduleWorker.enqueue(context,c);note="Pengingat disimpan; izinkan notifikasi agar terlihat"}},modifier=Modifier.fillMaxWidth()){Text("Simpan & jadwalkan pengingat")}
    if(note.isNotBlank())Text(note,color=Cyan)
    SectionTitle("Antrean (${configs.size})")
    PagedItems(configs){c->val state=states.find{it.id==c.id}?.body?:"NOT_ENQUEUED";PremiumCard{StatusPill(state,state !in setOf("WAITING_FOR_DEPENDENCY","CANCELLED"));Text(c.name,style=MaterialTheme.typography.titleMedium);Text(if(c.recurringMinutes>0)"Setiap ${c.recurringMinutes} menit" else formatter.format(Instant.ofEpochMilli(c.runAt).atZone(ZoneId.of("Asia/Jakarta"))),color=Muted);Text("Prioritas ${c.priority} • ${if(c.dependency.isBlank())"Tanpa dependensi" else "Menunggu konfirmasi jadwal sebelumnya"}",color=Muted,style=MaterialTheme.typography.bodySmall);Row{TextButton(onClick={scope.launch{val enabled=!c.enabled;dao.save(Record("schedule_config",c.id,c.copy(enabled=enabled).encode()));if(enabled){ScheduleWorker.enqueue(context,c.copy(enabled=true));dao.save(Record("schedule",c.id,"ENQUEUED"))}else{ScheduleWorker.cancel(context,c.id);dao.save(Record("schedule",c.id,"PAUSED"))}}}){Text(if(c.enabled)"Pause" else "Aktifkan")};TextButton(onClick={scope.launch{dao.save(Record("schedule",c.id,"USER_COMPLETED"))}}){Text("Saya sudah selesai")};TextButton(onClick={scope.launch{ScheduleWorker.cancel(context,c.id);dao.delete("schedule_config",c.id);dao.delete("schedule",c.id)}}){Text("Hapus")}}}}
    TextButton(onClick={ReminderWorker.cancel(context);note="Pengingat legacy v0.1–v0.3 dibatalkan"}){Text("Batalkan pengingat versi lama")}
}
