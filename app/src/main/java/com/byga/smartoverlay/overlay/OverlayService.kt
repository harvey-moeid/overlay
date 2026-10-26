package com.byga.smartoverlay.overlay

import android.animation.ValueAnimator
import com.byga.smartoverlay.ui.StudioSessions
import com.byga.smartoverlay.agent.AgentSessions
import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.TextView
import android.content.res.ColorStateList
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.accessibility.ManualInputPermit
import com.byga.smartoverlay.accessibility.UiResult
import com.byga.smartoverlay.data.BygaDatabase
import com.byga.smartoverlay.data.Record
import com.byga.smartoverlay.R
import androidx.datastore.preferences.core.intPreferencesKey
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.Button
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.data.settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import com.byga.smartoverlay.MainActivity

class OverlayService : Service() {
    private val serviceScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var reducedMotion=false
    private val smartMapping by lazy { com.byga.smartoverlay.mapping.MappingOverlay(this,serviceScope,{closePanel()},{visible->bubble?.visibility=if(visible)View.VISIBLE else View.INVISIBLE}) }
    private var panelJob: Job? = null
    private var iconJob: Job? = null
    private val manualHandler=Handler(Looper.getMainLooper())
    override fun onCreate() {
        super.onCreate()
        StudioSessions.register("manual-input"){manualHandler.removeCallbacksAndMessages(null);smartMapping.stop()}
        serviceScope.launch { settings.data.collect {
            reducedMotion=it[booleanPreferencesKey("reduced_motion")] ?: false
            bubbleSizeDp=OverlayAppearance.visualDp(it[OverlayAppearance.sizeKey] ?: OverlayAppearance.DEFAULT_DP)
            if(bubble!=null) { params.width=size; params.height=size; resizeIcon(); clamp(); update() }
        } }
    }
    private lateinit var wm: WindowManager
    private var bubble: ImageView? = null
    private var panel: View? = null
    private var animator: ValueAnimator? = null
    private lateinit var params: WindowManager.LayoutParams
    private var bubbleSizeDp=OverlayAppearance.DEFAULT_DP
    private val size get() = (OverlayAppearance.touchDp(bubbleSizeDp) * resources.displayMetrics.density).toInt()
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP" || !Settings.canDrawOverlays(this)) { StudioSessions.stopAll(); AgentSessions.stopAll(); stopSelf(); return START_NOT_STICKY }
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("bubble","Bubble aktif",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,OverlayService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"bubble").setContentTitle("BYGA • Bubble aktif").setContentText("Ketuk untuk panel • Stop untuk menutup").setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).addAction(Notification.Action.Builder(null,"Stop",stop).build()).setOngoing(true).build()
        startForeground(10,notification)
        if (bubble == null) show()
        smartMapping // Connect pending workflow confirmations while the bubble is active.
        if(intent?.action=="MAP_BUTTONS")smartMapping.begin(intent.getBooleanExtra("point",false))
        return START_NOT_STICKY
    }
    private fun show() {
        wm=getSystemService(WindowManager::class.java)
        params=WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.LEFT; x=0; y=200 }
        val view=object : ImageView(this) {
            override fun performClick(): Boolean { super.performClick(); togglePanel(); return true }
            override fun performLongClick(): Boolean { StudioSessions.stopAll(); AgentSessions.stopAll(); stopSelf(); return true }
        }.apply {
            scaleType=ImageView.ScaleType.FIT_CENTER
            contentDescription="Robot BYGA: ketuk buka panel, tahan untuk stop, geser untuk pindah"
            isFocusable=true
            isClickable=true
            isLongClickable=true
        }
        var longPressed=false
        val detector=GestureDetector(this,object: GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: android.view.MotionEvent) = true
            override fun onSingleTapUp(e: android.view.MotionEvent) = true
            override fun onLongPress(e: android.view.MotionEvent) { longPressed=true; view.performLongClick() }
        })
        var startX=0; var startY=0; var touchX=0f; var touchY=0f; var dragged=false
        view.setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { animator?.cancel(); startX=params.x; startY=params.y; touchX=event.rawX; touchY=event.rawY; dragged=false; longPressed=false; detector.onTouchEvent(event) }
                MotionEvent.ACTION_MOVE -> {
                    detector.onTouchEvent(event)
                    if (kotlin.math.abs(event.rawX-touchX)+kotlin.math.abs(event.rawY-touchY)>12) dragged=true
                    if (dragged) { params.x=(startX+event.rawX-touchX).toInt(); params.y=(startY+event.rawY-touchY).toInt(); clamp(); update() }
                }
                MotionEvent.ACTION_UP -> { detector.onTouchEvent(event); if(!dragged && !longPressed) view.performClick(); if(!longPressed) dock() }
                MotionEvent.ACTION_CANCEL -> dock()
            }; true
        }
        try { wm.addView(view,params); bubble=view; resizeIcon(); OverlayRuntime.setRunning(true)
            if(!reducedMotion) { view.scaleX=0.82f; view.scaleY=0.82f; view.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(OvershootInterpolator(0.8f)).start() }
        } catch (_: Exception) { stopSelf() }
    }
    private fun closePanel() {
        panelJob?.cancel(); panelJob=null
        panel?.let { runCatching { wm.removeView(it) } }; panel=null
    }
    private fun togglePanel() {
        if(panel!=null || panelJob?.isActive==true) { closePanel(); return }
        panelJob=serviceScope.launch {
            try {
                val target=SafeAccessibilityService.current?.foregroundPackage() ?: ""
                val targetField=SafeAccessibilityService.current?.focusedFieldFingerprint(target) ?: ""
                ensureActive()
                showPanel(target,targetField)
            } catch(e:CancellationException){throw e} catch(_:Exception){Toast.makeText(this@OverlayService,"Panel belum siap. Coba lagi.",Toast.LENGTH_SHORT).show()}
        }
    }
    private fun resizeIcon() {
        val view=bubble ?: return
        val padding=((size-bubbleSizeDp*resources.displayMetrics.density)/2).toInt().coerceAtLeast(0)
        view.setPadding(padding,padding,padding,padding)
        iconJob?.cancel()
        val pixels=(bubbleSizeDp*resources.displayMetrics.density).toInt().coerceAtLeast(1)
        iconJob=serviceScope.launch {
            val bitmap=withContext(Dispatchers.IO) {
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeResource(resources,R.drawable.byga_robot,bounds)
                var sample=1
                while(bounds.outWidth/(sample*2)>=pixels)sample*=2
                BitmapFactory.decodeResource(resources,R.drawable.byga_robot,BitmapFactory.Options().apply{inSampleSize=sample})
            }
            view.setImageBitmap(bitmap)
        }
    }
    private fun showPanel(target:String,targetField:String) {
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,18,18,18); setBackgroundColor(Color.rgb(8,13,25)) }
        val heading=TextView(this).apply{text="BYGA  /  QUICK STUDIO";setTextColor(Color.rgb(114,232,226));textSize=16f;setPadding(8,8,8,16)}
        layout.addView(heading)
        val input=EditText(this).apply { hint="Tulis prompt atau draft…"; setTextColor(Color.WHITE); setHintTextColor(Color.rgb(156,170,198)); textSize=14f; maxLines=4; minLines=2;contentDescription="Prompt bubble";inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;setPadding(18,18,18,18);background=GradientDrawable().apply{setColor(Color.rgb(22,34,57));cornerRadius=24f} }
        layout.addView(input)
        fun button(label: String, action: () -> Unit) { layout.addView(Button(this).apply { text=label;isAllCaps=false;setTextColor(Color.WHITE);backgroundTintList=ColorStateList.valueOf(if(label.startsWith("STOP"))Color.rgb(109,39,61) else Color.rgb(31,62,119));setOnClickListener { action() } }) }
        val targetLabel=runCatching{packageManager.getApplicationLabel(packageManager.getApplicationInfo(target,0)).toString()}.getOrDefault(target.ifBlank{"Tidak diketahui"})
        val review=CheckBox(this).apply{text="Saya meninjau teks, akun, dan target $targetLabel. Isi kolom fokus saja, tanpa mengirim.";setTextColor(Color.LTGRAY);isEnabled=target in ManualInputPermit.packages && targetField.isNotBlank()}
        layout.addView(review)
        button("Isi kolom fokus • $targetLabel") {
            if(!review.isChecked || target !in ManualInputPermit.packages)Toast.makeText(this,"Pilih kolom teks di aplikasi yang didukung, lalu buka bubble dan review target",Toast.LENGTH_LONG).show()
            else {
                val permit=ManualInputPermit(target,input.text.toString(),true,SystemClock.elapsedRealtime(),targetField)
                closePanel()
                manualHandler.postDelayed({
                    serviceScope.launch {
                        val result=try { SafeAccessibilityService.current?.applyReviewedInput(permit) ?: UiResult.PERMISSION_DENIED }
                            catch(e:CancellationException){throw e} catch(_:Exception){UiResult.BLOCKED}
                        Toast.makeText(this@OverlayService,"BYGA input: $result • tidak menekan Kirim",Toast.LENGTH_LONG).show()
                        BygaDatabase.get(this@OverlayService).records().save(Record("manual_input",java.util.UUID.randomUUID().toString(),"$target • $result"))
                    }
                },450)
            }
        }
        button("Smart Mapping • deteksi tombol") { smartMapping.begin() }
        button("Smart Mapping • tunjuk langsung") { smartMapping.begin(true) }
        button("Library mapping") { closePanel();startActivity(Intent(this,MainActivity::class.java).putExtra("open_mapping",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        button("Salin") { Handoffs.copy(this,input.text.toString()) }
        button("Bagikan teks") { runCatching { Handoffs.share(this,input.text.toString()) } }
        button("Panel utama") { closePanel(); startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        button("Tutup panel") { closePanel() }
        button("STOP bubble & agent") { StudioSessions.stopAll(); AgentSessions.stopAll(); stopSelf() }
        val width=(300*resources.displayMetrics.density).toInt().coerceAtMost(resources.displayMetrics.widthPixels)
        val scroller=ScrollView(this).apply {isFillViewport=true;addView(layout);background=GradientDrawable().apply{setColor(Color.rgb(8,13,25));cornerRadius=32f;setStroke(2,Color.rgb(53,87,138))};clipToOutline=true}
        val height=minOf((520*resources.displayMetrics.density).toInt(),(resources.displayMetrics.heightPixels*0.5f).toInt())
        val p=WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,OverlayAppearance.PANEL_FLAGS,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL; y=64; softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        scroller.setOnTouchListener { _,event -> if(event.actionMasked==MotionEvent.ACTION_OUTSIDE) { closePanel();true } else false }
        try { wm.addView(scroller,p); panel=scroller } catch (_: Exception) { panel=null }
    }
    private fun clamp() {
        val d=resources.displayMetrics
        params.x=params.x.coerceIn(0,(d.widthPixels-size).coerceAtLeast(0))
        params.y=params.y.coerceIn(32,(d.heightPixels-size-100).coerceAtLeast(32))
    }
    private fun update() { bubble?.let { try { wm.updateViewLayout(it,params) } catch (_: Exception) { stopSelf() } } }
    private fun dock() {
        val target=if(params.x < resources.displayMetrics.widthPixels/2) 0 else resources.displayMetrics.widthPixels-size
        animator=ValueAnimator.ofInt(params.x,target).apply { duration=if(reducedMotion) 0 else 280; interpolator=OvershootInterpolator(0.6f); addUpdateListener { params.x=it.animatedValue as Int; clamp(); update() }; start() }
    }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig);smartMapping.stop();manualHandler.removeCallbacksAndMessages(null); closePanel(); if(bubble!=null) { clamp(); update() } }
    override fun onDestroy() { closePanel(); smartMapping.stop();StudioSessions.unregister("manual-input");manualHandler.removeCallbacksAndMessages(null);OverlayRuntime.setRunning(false); serviceScope.cancel(); panel?.let { runCatching { wm.removeView(it) } }; panel=null; animator?.cancel(); bubble?.let { it.animate().cancel(); runCatching { wm.removeView(it) } }; bubble=null; stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
}
