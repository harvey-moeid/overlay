package com.byga.smartoverlay.overlay

import android.animation.ValueAnimator
import com.byga.smartoverlay.agent.AgentSessions
import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
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
    override fun onCreate() {
        super.onCreate()
        serviceScope.launch { settings.data.collect {
            reducedMotion=it[booleanPreferencesKey("reduced_motion")] ?: false
            bubbleSizeDp=(it[intPreferencesKey("bubble_size_dp")] ?: 72).coerceIn(56,96)
            if(bubble!=null) { params.width=size; params.height=size; clamp(); update() }
        } }
    }
    private lateinit var wm: WindowManager
    private var bubble: ImageView? = null
    private var panel: LinearLayout? = null
    private var animator: ValueAnimator? = null
    private lateinit var params: WindowManager.LayoutParams
    private var bubbleSizeDp=72
    private val size get() = (bubbleSizeDp * resources.displayMetrics.density).toInt()
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP" || !Settings.canDrawOverlays(this)) { AgentSessions.stopAll(); stopSelf(); return START_NOT_STICKY }
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("bubble","Bubble aktif",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,OverlayService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"bubble").setContentTitle("BYGA • Bubble aktif").setContentText("Ketuk untuk panel • Stop untuk menutup").setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).addAction(Notification.Action.Builder(null,"Stop",stop).build()).setOngoing(true).build()
        startForeground(10,notification)
        if (bubble == null) show()
        return START_NOT_STICKY
    }
    private fun show() {
        wm=getSystemService(WindowManager::class.java)
        params=WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.LEFT; x=0; y=200 }
        val view=object : ImageView(this) {
            override fun performClick(): Boolean { super.performClick(); togglePanel(); return true }
            override fun performLongClick(): Boolean { AgentSessions.stopAll(); stopSelf(); return true }
        }.apply {
            setImageResource(R.drawable.byga_robot)
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
        try { wm.addView(view,params); bubble=view
            if(!reducedMotion) { view.scaleX=0.82f; view.scaleY=0.82f; view.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(OvershootInterpolator(0.8f)).start() }
        } catch (_: Exception) { stopSelf() }
    }
    private fun togglePanel() {
        panel?.let { runCatching { wm.removeView(it) }; panel=null; return }
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,18,18,18); setBackgroundColor(Color.rgb(8,13,25)) }
        val input=EditText(this).apply { hint="Prompt BYGA"; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); maxLines=4; contentDescription="Prompt bubble" }
        layout.addView(input)
        fun button(label: String, action: () -> Unit) { layout.addView(Button(this).apply { text=label; setOnClickListener { action() } }) }
        button("Salin") { Handoffs.copy(this,input.text.toString()) }
        button("Bagikan teks") { runCatching { Handoffs.share(this,input.text.toString()) } }
        button("Panel utama") { togglePanel(); startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
        button("Tutup panel") { togglePanel() }
        button("STOP bubble & agent") { AgentSessions.stopAll(); stopSelf() }
        val width=(300*resources.displayMetrics.density).toInt().coerceAtMost(resources.displayMetrics.widthPixels)
        val p=WindowManager.LayoutParams(width,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,0,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL; y=64; softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try { wm.addView(layout,p); panel=layout } catch (_: Exception) { panel=null }
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
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); panel?.let { togglePanel() }; if(bubble!=null) { clamp(); update() } }
    override fun onDestroy() { serviceScope.cancel(); panel?.let { runCatching { wm.removeView(it) } }; panel=null; animator?.cancel(); bubble?.let { it.animate().cancel(); runCatching { wm.removeView(it) } }; bubble=null; stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
}
