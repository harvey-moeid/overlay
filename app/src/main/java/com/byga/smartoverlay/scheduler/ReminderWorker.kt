package com.byga.smartoverlay.scheduler

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.byga.smartoverlay.MainActivity
import com.byga.smartoverlay.data.*
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val id=inputData.getString("workflow") ?: return Result.failure()
        // UI execution always requires an explicit user handoff, even when unlocked.
        BygaDatabase.get(applicationContext).records().save(Record("schedule",id,"WAITING_FOR_USER"))
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("schedule","Jadwal",NotificationManager.IMPORTANCE_DEFAULT))
        if(ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT<33) {
            val open=PendingIntent.getActivity(applicationContext,3,Intent(applicationContext,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            NotificationManagerCompat.from(applicationContext).notify(21,NotificationCompat.Builder(applicationContext,"schedule").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("BYGA • Butuh tindakan pengguna").setContentText("Buka aplikasi, tinjau workflow dan akun sebelum melanjutkan.").setContentIntent(open).setAutoCancel(true).build())
        }
        return Result.success()
    }
    companion object {
        fun schedule(context: Context, minutes: Long, recurring: Boolean) {
            val data=workDataOf("workflow" to "local-draft")
            if(recurring) WorkManager.getInstance(context).enqueueUniquePeriodicWork("byga-reminder",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<ReminderWorker>(minutes.coerceAtLeast(15),TimeUnit.MINUTES).setInputData(data).build())
            else WorkManager.getInstance(context).enqueueUniqueWork("byga-once",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(minutes.coerceAtLeast(1),TimeUnit.MINUTES).setInputData(data).build())
        }
        fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork("byga-reminder"); WorkManager.getInstance(context).cancelUniqueWork("byga-once") }
    }
}
