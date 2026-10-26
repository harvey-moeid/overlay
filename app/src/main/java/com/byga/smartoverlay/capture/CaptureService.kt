package com.byga.smartoverlay.capture

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.WindowManager
import androidx.core.content.FileProvider
import com.byga.smartoverlay.MainActivity
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.ui.StudioSessions
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID
import org.json.JSONObject

class CaptureService: Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var projection: MediaProjection?=null;private var display: VirtualDisplay?=null;private var reader: ImageReader?=null
    private val claimed=java.util.concurrent.atomic.AtomicBoolean(false)
    private var requested="";private var rotation=0
    override fun onBind(intent: Intent?) = null
    override fun onCreate(){super.onCreate();StudioSessions.register("capture"){stopSelf()}}
    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?,flags:Int,startId:Int):Int {
        if(projection!=null){stopSelf();return START_NOT_STICKY}
        scope.launch(Dispatchers.IO) { File(cacheDir,"shared").listFiles()?.filter{it.name.startsWith("capture-") && it.name.endsWith(".png") && System.currentTimeMillis()-it.lastModified()>86400000}?.forEach{it.delete()} }
        requested=intent?.getStringExtra("package")?:""
        val consent=intent?.getParcelableExtra<Intent>("consent")
        if(consent==null || requested !in CapturePolicy.packages){stopSelf();return START_NOT_STICKY}
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("capture","Capture satu sesi",NotificationManager.IMPORTANCE_LOW))
        val stop=PendingIntent.getService(this,44,Intent(this,CaptureService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"capture").setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("BYGA • Capture satu sesi").setContentText("Satu gambar, disimpan privat; tidak dikirim ke AI").addAction(Notification.Action.Builder(null,"Stop",stop).build()).setOngoing(true).build()
        if(Build.VERSION.SDK_INT>=29) startForeground(44,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(44,notification)
        try {
            projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(Activity.RESULT_OK,consent)
            projection!!.registerCallback(object:MediaProjection.Callback(){override fun onStop(){stopSelf()}},Handler(Looper.getMainLooper()))
            rotation=getSystemService(WindowManager::class.java).defaultDisplay.rotation
            scope.launch {
                // Wait only for user-selected foreground app, never guessing or bypassing a protected screen.
                var ready=false
                repeat(20){if(!ready){ready=SafeAccessibilityService.current?.captureAllowed(requested)==true;if(!ready)delay(250)}}
                if(!ready){fail("Capture diblokir: app target/layar/izin tidak siap");return@launch}
                capture()
                delay(8000)
                if(!claimed.get())fail("Tidak ada frame yang dapat diambil; periksa consent atau layar terlindungi")
            }
        }catch(_:Exception){fail("Izin capture ditolak atau sesi tidak valid")}
        return START_NOT_STICKY
    }
    @Suppress("DEPRECATION") private fun capture(){
        val metrics=resources.displayMetrics
        val scale=minOf(1f,1280f/metrics.widthPixels,2048f/metrics.heightPixels)
        val w=(metrics.widthPixels*scale).toInt().coerceAtLeast(1);val h=(metrics.heightPixels*scale).toInt().coerceAtLeast(1)
        reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2)
        reader!!.setOnImageAvailableListener({r->
            val image=runCatching{r.acquireLatestImage()}.getOrNull()?:return@setOnImageAvailableListener
            if(!claimed.compareAndSet(false,true)){image.close();return@setOnImageAvailableListener}
            scope.launch {
                try{
                    if(SafeAccessibilityService.current?.captureAllowed(requested)!=true || getSystemService(WindowManager::class.java).defaultDisplay.rotation!=rotation){fail("Konteks berubah; capture dibatalkan");return@launch}
                    val file=withContext(Dispatchers.IO){
                        image.use{frame->
                            val plane=frame.planes[0];val paddedWidth=plane.rowStride/plane.pixelStride
                            val padded=Bitmap.createBitmap(paddedWidth,h,Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(plane.buffer)
                            val bitmap=Bitmap.createBitmap(padded,0,0,w,h)
                            val dir=File(cacheDir,"shared").apply{mkdirs()};val out=File(dir,"capture-${UUID.randomUUID()}.png")
                            out.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};if(bitmap!==padded)bitmap.recycle();padded.recycle();out
                        }
                    }
                    val uri=FileProvider.getUriForFile(this@CaptureService,"$packageName.files",file)
                    BygaDatabase.get(this@CaptureService).records().save(Record("capture",file.name,JSONObject().put("uri",uri.toString()).put("package",requested).put("status","LOCAL_REVIEW_REQUIRED").toString()))
                    val open=PendingIntent.getActivity(this@CaptureService,45,Intent(this@CaptureService,MainActivity::class.java).putExtra("open_capture",true),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    if(Build.VERSION.SDK_INT<33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED) getSystemService(NotificationManager::class.java).notify(45,Notification.Builder(this@CaptureService,"capture").setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("Capture siap ditinjau").setContentText("Buka BYGA → Screen Capture. Belum dikirim ke siapa pun.").setContentIntent(open).setAutoCancel(true).build())
                }catch(_:Exception){fail("Capture gagal disimpan; tidak ada klaim sukses")}
                finally{image.close();stopSelf()}
            }
        },Handler(Looper.getMainLooper()))
        display=projection!!.createVirtualDisplay("BYGA-on-demand",w,h,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null)
    }
    private fun fail(message:String){scope.launch{BygaDatabase.get(this@CaptureService).records().save(Record("capture_error","last",message));stopSelf()}}
    override fun onDestroy(){StudioSessions.unregister("capture");display?.release();reader?.close();projection?.stop();projection=null;scope.cancel();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()}
}
