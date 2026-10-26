package com.byga.smartoverlay.mapping

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.os.SystemClock
import android.view.*
import android.widget.*
import com.byga.smartoverlay.MainActivity
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.data.BygaDatabase
import com.byga.smartoverlay.data.Record
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** One teaching surface; pointing consumes a touch without forwarding it to the target app. */
class MappingOverlay(private val context:Context,private val scope:CoroutineScope,
    private val hidePanel:()->Unit,private val bubbleVisible:(Boolean)->Unit) {
    private val wm=context.getSystemService(WindowManager::class.java)
    private var panel:View?=null
    private var pointer:View?=null
    private var timeout:Job?=null
    private var work:Job?=null
    private var runtimeRequest:MappingConfirmation?=null
    init {
        scope.launch { MappingRuntime.confirmation.collectLatest { request ->
            if(request==null) { if(runtimeRequest!=null)close();runtimeRequest=null }
            else { runtimeRequest=request;showRuntime(request) }
        } }
    }
    fun close() {
        panel?.let{runCatching{wm.removeView(it)}};panel=null
        pointer?.let{runCatching{wm.removeView(it)}};pointer=null
        timeout?.cancel();timeout=null;bubbleVisible(true)
    }
    fun stop() { work?.cancel();MappingRuntime.cancel();close() }
    fun begin(point:Boolean=false) {
        if(com.byga.smartoverlay.workflow.WorkflowRunner.hasActiveRun() && !MappingRuntime.paused){toast("Jeda atau Stop workflow sebelum memetakan tombol");return}
        if(MappingRuntime.confirmation.value!=null){toast("Jawab konfirmasi workflow dahulu");return}
        work?.cancel();hidePanel();close()
        work=scope.launch {
            delay(450)
            runCatching{checkNotNull(SafeAccessibilityService.current){"Aktifkan Accessibility dahulu"}.mapping.snapshot()}
                .onSuccess{if(point)point(it) else list(it)}.onFailure{toast(it.message ?: "Layar tidak dapat dibaca")}
        }
    }
    private fun toast(text:String){Toast.makeText(context,text,Toast.LENGTH_LONG).show()}
    private fun text(value:String)=TextView(context).apply{text=value;setTextColor(Color.WHITE);textSize=14f;setPadding(8,8,8,8)}
    private fun button(box:LinearLayout,label:String,action:()->Unit) { box.addView(Button(context).apply{text=label;isAllCaps=false;setTextColor(Color.WHITE);backgroundTintList=ColorStateList.valueOf(Color.rgb(31,62,119));setOnClickListener{action()}}) }
    private fun show(title:String,content:(LinearLayout)->Unit) {
        close();hidePanel()
        val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(18,18,18,18);setBackgroundColor(Color.rgb(8,13,25));addView(text(title).apply{textSize=18f;setTextColor(Color.CYAN)})}
        content(box)
        val scroll=ScrollView(context).apply{addView(box)}
        val width=minOf((340*context.resources.displayMetrics.density).toInt(),context.resources.displayMetrics.widthPixels)
        val params=WindowManager.LayoutParams(width,(context.resources.displayMetrics.heightPixels*0.62f).toInt(),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,0,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;y=(64*context.resources.displayMetrics.density).toInt();softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE}
        runCatching{wm.addView(scroll,params);panel=scroll}.onFailure{toast("Izin overlay tidak tersedia");close()}
    }
    private fun list(snapshot:MappingSnapshot) {
        show("Smart Mapping • ${snapshot.candidates.size} tombol") { box ->
            box.addView(text("${snapshot.scope.packageName}\n${snapshot.scope.site.ifBlank{snapshot.scope.screen}}\nPilih tombol untuk disorot dan ditinjau. Belum ada tombol aplikasi yang ditekan."))
            button(box,"Tunjuk tombol langsung"){close();point(snapshot)}
            snapshot.candidates.take(100).forEach { candidate ->
                val name=candidate.identity.description.ifBlank{candidate.identity.label}.ifBlank{candidate.identity.resourceId.substringAfterLast('/')}
                button(box,name.take(90)){review(snapshot,candidate,null)}
            }
            if(snapshot.candidates.isEmpty())box.addView(text("Tidak ada tombol beridentitas terbaca. Gunakan mode tunjuk; posisi manual akan selalu meminta konfirmasi."))
            button(box,"Pindai ulang"){begin()}
            button(box,"Batal"){close()}
        }
    }
    private fun point(snapshot:MappingSnapshot) {
        close();hidePanel();bubbleVisible(false)
        val frame=android.widget.FrameLayout(context)
        val drawing=object:View(context) {
            private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas:Canvas) {
                canvas.drawColor(Color.argb(38,0,0,0));paint.color=Color.CYAN;paint.style=Paint.Style.STROKE;paint.strokeWidth=3f
                snapshot.candidates.forEach{val b=it.bounds;canvas.drawRoundRect(b.left.toFloat(),b.top.toFloat(),b.right.toFloat(),b.bottom.toFloat(),12f,12f,paint)}
            }
            @Suppress("ClickableViewAccessibility")
            override fun onTouchEvent(event:MotionEvent):Boolean {
                if(event.actionMasked==MotionEvent.ACTION_UP) {
                    val x=event.rawX;val y=event.rawY
                    val fresh=runCatching{SafeAccessibilityService.current?.mapping?.snapshot()}.getOrNull()
                    close()
                    if(fresh==null || fresh.scope!=snapshot.scope || fresh.layoutHash!=snapshot.layoutHash)toast("Layar berubah. Buka mapping kembali.")
                    else review(fresh,MappingPolicy.candidateAt(fresh,x,y),x to y)
                    performClick()
                }
                return true
            }
            override fun performClick():Boolean { super.performClick();return true }
        }
        frame.addView(drawing,android.widget.FrameLayout.LayoutParams(-1,-1))
        val cancel=Button(context).apply{text="Tunjuk target • Batal";isAllCaps=false;setOnClickListener{close()}}
        frame.addView(cancel,android.widget.FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=48})
        val params=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT)
        runCatching{wm.addView(frame,params);pointer=frame}.onFailure{close();toast("Mode tunjuk tidak tersedia")}
        timeout=scope.launch{delay(30000);close();toast("Mode tunjuk berakhir; tidak ada klik dilakukan")}
    }
    private fun review(snapshot:MappingSnapshot,candidate:MappingCandidate?,point:Pair<Float,Float>?) {
        if(candidate==null && point==null){toast("Target tidak ditemukan");return}
        val semantic=candidate!=null && snapshot.candidates.count{it.identity==candidate.identity}==1
        val selectedPoint=point ?: candidate?.bounds?.let{(it.left+it.right)/2f to (it.top+it.bottom)/2f}
        val defaultName=candidate?.identity?.let{it.description.ifBlank{it.label}.ifBlank{it.resourceId.substringAfterLast('/')}} ?: "Posisi manual"
        show("Tombol dipilih • tanya dahulu") { box ->
            var chosen=false
            val name=EditText(context).apply{setText(defaultName.take(80));setTextColor(Color.WHITE);hint="Nama aksi";maxLines=2}
            box.addView(name)
            box.addView(text("${snapshot.scope.packageName}\n${snapshot.scope.site.ifBlank{snapshot.scope.screen}}\n${candidate?.identity?.resourceId.orEmpty()}\n${if(semantic)"Target dikenali berdasarkan identitas; posisi boleh bergeser." else "Identitas target tidak terbaca atau kembar. Posisi manual harus dikonfirmasi setiap kali dan hanya berlaku pada ukuran/layout yang sama."}"))
            val ask=CheckBox(context).apply{text="Tanya sebelum setiap klik workflow";setTextColor(Color.LTGRAY);isChecked=!semantic;isEnabled=semantic}
            box.addView(ask)
            fun save(test:Boolean) {
                if(chosen)return
                if(name.text.isBlank()){toast("Isi nama mapping");return}
                val mapping=ButtonMapping(name=name.text.toString().take(80),scope=snapshot.scope,
                    kind=if(semantic)MappingKind.SEMANTIC else MappingKind.POINT,
                    identity=candidate?.identity ?: TargetIdentity(),x=(selectedPoint?.first ?: 0f)/snapshot.width,y=(selectedPoint?.second ?: 0f)/snapshot.height,
                    width=snapshot.width,height=snapshot.height,rotation=snapshot.rotation,layoutHash=snapshot.layoutHash,askEveryTime=ask.isChecked)
                if(!mapping.validate()){toast("Mapping tidak valid");return}
                chosen=true
                val permit=if(test)MappingClickPermit(mapping.fingerprint(),SystemClock.elapsedRealtime()) else null
                close()
                work=scope.launch {
                    BygaDatabase.get(context).records().save(Record("button_mapping",mapping.id,mapping.encode()))
                    if(test) {
                        delay(450)
                        try {
                            requireCurrentMapping(context,mapping)
                            val result=checkNotNull(SafeAccessibilityService.current).mapping.click(mapping,checkNotNull(permit))
                            BygaDatabase.get(context).records().save(Record("mapping_test",mapping.id,result));toast(result)
                        } catch(e:CancellationException){throw e} catch(e:Exception){toast(e.message ?: "Klik tidak dapat dipastikan")}
                    } else toast("Mapping disimpan tanpa menekan tombol aplikasi")
                }
            }
            button(box,"Simpan saja"){save(false)}
            button(box,"Simpan & uji tombol sekarang"){save(true)}
            button(box,"Batal"){close()}
        }
        highlight(snapshot,candidate?.bounds ?: selectedPoint?.let{TargetBounds(it.first.toInt()-28,it.second.toInt()-28,it.first.toInt()+28,it.second.toInt()+28)})
    }
    private fun highlight(snapshot:MappingSnapshot,bounds:TargetBounds?) {
        if(bounds==null)return
        val view=object:View(context) {
            override fun onDraw(canvas:Canvas) {
                val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.CYAN;style=Paint.Style.STROKE;strokeWidth=6f}
                canvas.drawRoundRect(bounds.left.toFloat(),bounds.top.toFloat(),bounds.right.toFloat(),bounds.bottom.toFloat(),12f,12f,paint)
            }
        }
        val params=WindowManager.LayoutParams(snapshot.width,snapshot.height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.LEFT;alpha=0.65f}
        runCatching{wm.addView(view,params);pointer=view}
    }
    private fun showRuntime(request:MappingConfirmation) {
        show("Workflow meminta klik") { box ->
            val m=request.mapping
            box.addView(text("${m.name}\n${m.scope.packageName}\n${m.scope.site.ifBlank{m.scope.screen}}\n${if(m.kind==MappingKind.POINT)"Posisi manual ${(m.x*100).toInt()}% × ${(m.y*100).toInt()}%. Periksa target pada layar sebelum melanjutkan." else "Klik satu kali pada tombol beridentitas yang sudah dipetakan."}\nGenerate dapat memakai kredit akun situs."))
            button(box,"Ya, klik satu kali"){if(MappingRuntime.confirmation.value===request)request.answer.complete(MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime()));close()}
            button(box,"Batal"){request.answer.complete(null);close()}
        }
        if(request.mapping.kind==MappingKind.POINT) {
            val s=runCatching{SafeAccessibilityService.current?.mapping?.snapshot()}.getOrNull()
            if(s!=null){val x=(request.mapping.x*s.width).toInt();val y=(request.mapping.y*s.height).toInt();highlight(s,TargetBounds(x-28,y-28,x+28,y+28))}
        }
    }
}
