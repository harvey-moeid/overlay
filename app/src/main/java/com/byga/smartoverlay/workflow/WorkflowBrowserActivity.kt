package com.byga.smartoverlay.workflow

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.byga.smartoverlay.MainActivity
import com.byga.smartoverlay.media.*
import com.byga.smartoverlay.mapping.MappingBrowserContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/** Visible browser workspace. No JS interface, raw scripts, hidden login or background actions. */
class WorkflowBrowserActivity:ComponentActivity() {
    private lateinit var web:WebView
    private lateinit var message:TextView
    private val frameMonitor by lazy { com.byga.smartoverlay.performance.WindowFrameMonitor(window) }
    private lateinit var address:TextView
    private lateinit var pause:Button
    private var resumed=false
    private var upload:Uri?=null
    private var chooserUsed=false
    private var failedLoad=false
    private var loading=false
    private val adapter by lazy { assets.open("workflow-adapter.js").bufferedReader().use{it.readText()} }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(8,13,25));setPadding(12,12,12,0) }
        address=TextView(this).apply { setTextColor(Color.CYAN);textSize=12f }
        message=TextView(this).apply { setTextColor(Color.WHITE);textSize=14f;setPadding(0,8,0,8) }
        layout.addView(address);layout.addView(message)
        val controls=LinearLayout(this)
        fun control(label:String,action:()->Unit)=Button(this).apply { text=label;isAllCaps=false;setOnClickListener{action()};controls.addView(this,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)) }
        control("Alur") { startActivity(Intent(this,MainActivity::class.java).putExtra("open_workflow",true).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        pause=control("Jeda") { WorkflowBrowserSession.changePause(!WorkflowBrowserSession.paused);pause.text=if(WorkflowBrowserSession.paused)"Lanjut" else "Jeda" }
        control("Stop") { com.byga.smartoverlay.ui.StudioSessions.stopAll();WorkflowBrowserSession.end();message.text="Workflow dihentikan. Hasil situs dapat diperiksa manual." }
        control("Mapping") {
            if(android.provider.Settings.canDrawOverlays(this))androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,com.byga.smartoverlay.overlay.OverlayService::class.java).setAction("MAP_BUTTONS"))
            else message.text="Aktifkan izin bubble dari Pengaturan BYGA dahulu."
        }
        layout.addView(controls)
        web=WebView(this)
        web.settings.apply {
            javaScriptEnabled=true;domStorageEnabled=true;allowFileAccess=false;allowContentAccess=false
            mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            // Flow's editor uses a desktop layout; keep the actual WebView user agent identity.
            useWideViewPort=true;loadWithOverviewMode=true
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false)
        web.webViewClient=object:WebViewClient() {
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                if(!request.isForMainFrame)return false
                return if(WebWorkflowPolicy.navigable(request.url.toString()))false else { message.text="Navigasi di luar ChatGPT, Flow, atau login resmi diblokir.";true }
            }
            override fun onPageStarted(view:WebView,url:String?,favicon:android.graphics.Bitmap?) { MappingBrowserContext.url=url.orEmpty();loading=true;failedLoad=false;address.text=url?.let{Uri.parse(it).let{u->"${u.scheme}://${u.host}${u.path}"}} }
            override fun onPageFinished(view:WebView,url:String?) { loading=false }
            override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,response:WebResourceResponse) { if(request.isForMainFrame){failedLoad=true;message.text="Situs mengembalikan HTTP ${response.statusCode}. Periksa akses akun atau mulai ulang alur."} }
            override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError) { if(request.isForMainFrame){failedLoad=true;message.text="Halaman gagal dimuat. Periksa koneksi, lalu buka ulang workflow."} }
            override fun onReceivedSslError(view:WebView,handler:SslErrorHandler,error:android.net.http.SslError) { failedLoad=true;handler.cancel();message.text="Sertifikat situs tidak valid; koneksi dihentikan." }
        }
        web.webChromeClient=object:WebChromeClient() {
            override fun onShowFileChooser(view:WebView,callback:ValueCallback<Array<Uri>>,params:FileChooserParams):Boolean {
                val current=WorkflowBrowserSession.command.value
                val uri=upload
                val allowed=current?.action==Action.FLOW_UPLOAD_IMAGE && uri!=null && !chooserUsed && canAct(current)
                if(allowed) { chooserUsed=true; callback.onReceiveValue(arrayOf(uri!!)) }
                else { callback.onReceiveValue(null);message.text="Pilih gambar pada langkah Unggah gambar di editor Alur." }
                return true
            }
        }
        layout.addView(web,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));setContentView(layout)
        openMappingPage(intent)
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if(WorkflowBrowserSession.command.value!=null) { WorkflowBrowserSession.end() };finish() }
        })
        lifecycleScope.launch {
            WorkflowBrowserSession.command.collectLatest { command ->
                if(command==null) { upload=null; return@collectLatest }
                try { command.result.complete(execute(command)) }
                catch(e:CancellationException) { if(isFinishing)command.result.cancel();throw e }
                catch(e:Exception) { message.text=e.message ?: "Langkah gagal; periksa situs sebelum mengulang.";command.result.completeExceptionally(e) }
                finally { upload=null;chooserUsed=false }
            }
        }
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);setIntent(intent);openMappingPage(intent) }
    private fun openMappingPage(intent:Intent) {
        val url=when(intent.getStringExtra("mapping_site")){"chatgpt"->"https://chatgpt.com/";"flow"->"https://labs.google/fx/tools/flow";else->null}
        if(url!=null && !WorkflowBrowserSession.isActive()){web.loadUrl(url);message.text="Buka layar tujuan, lalu pilih Mapping. Login dilakukan manual."}
    }
    override fun onResume() { super.onResume();frameMonitor.start();resumed=true;MappingBrowserContext.visible=true;MappingBrowserContext.url=web.url.orEmpty();WorkflowBrowserSession.visible=true;if(::web.isInitialized)web.onResume();if(::pause.isInitialized)pause.text=if(WorkflowBrowserSession.paused)"Lanjut" else "Jeda" }
    override fun onPause() { frameMonitor.stop();resumed=false;MappingBrowserContext.visible=false;WorkflowBrowserSession.visible=false;if(::web.isInitialized)web.onPause();super.onPause() }
    override fun onDestroy() {
        frameMonitor.close()
        if(!isChangingConfigurations)WorkflowBrowserSession.command.value?.result?.completeExceptionally(IllegalStateException("Browser ditutup; periksa efek langkah di situs sebelum memulai ulang."))
        web.destroy();super.onDestroy()
    }
    private fun canAct(command:WebCommand)=resumed && !WorkflowBrowserSession.paused &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
        WorkflowBrowserSession.isActive() && WorkflowBrowserSession.command.value===command &&
        !command.result.isCompleted && WebWorkflowPolicy.permits(command.action,web.url ?: "") && !failedLoad && !loading

    private fun progress(note:String,state:RunState=RunState.RUNNING) {
        if(message.text.toString()!=note)message.text=note
        WorkflowBrowserSession.report(state,note)
    }
    private suspend fun awaitVisible(command:WebCommand,domain:Boolean=true) {
        awaitBrowserReady(state={
            checkCommandActive(command)
            when {
                domain && failedLoad -> BrowserReadiness.FAILED
                WorkflowBrowserSession.paused -> BrowserReadiness.PAUSED
                !resumed -> BrowserReadiness.BACKGROUND
                getSystemService(KeyguardManager::class.java).isKeyguardLocked -> BrowserReadiness.LOCKED
                domain && loading -> BrowserReadiness.LOADING
                !domain || canAct(command) -> BrowserReadiness.READY
                else -> BrowserReadiness.LOGIN
            }
        },onWaiting={progress(it,RunState.WAITING_FOR_USER)})
        progress(command.action.displayName())
    }
    private fun checkCommandActive(command:WebCommand) {
        check(WorkflowBrowserSession.isActive() && !command.result.isCompleted) { "Sesi workflow sudah berakhir" }
    }
    private suspend fun js(script:String):String {
        return suspendCancellableCoroutine { continuation ->
            web.evaluateJavascript(script) { result -> if(continuation.isActive) continuation.resume(runCatching{JSONTokener(result).nextValue()?.toString() ?: ""}.getOrDefault("")) }
        }
    }
    private suspend fun invoke(command:WebCommand,phase:String):JSONObject {
        awaitVisible(command)
        val key="bygaResult"+UUID.randomUUID().toString().replace("-","")
        val script=adapter.replace("BYGA_ACTION",JSONObject.quote(command.action.name))
            .replace("BYGA_PHASE",JSONObject.quote(phase)).replace("BYGA_RESULT",JSONObject.quote(key))
            .replace("BYGA_VALUE",JSONObject.quote(command.value))
        js(script)
        return try {
            withTimeout(20000) {
                while(true) {
                    awaitVisible(command)
                    val result=js("window[${JSONObject.quote(key)}] || ''")
                    if(result.isNotBlank()) return@withTimeout JSONObject(result)
                    delay(100)
                }
                @Suppress("UNREACHABLE_CODE") JSONObject()
            }
        } finally { if(canAct(command))js("delete window[${JSONObject.quote(key)}]") }
    }
    private suspend fun execute(command:WebCommand):String {
        message.text=command.action.displayName()
        if(command.action in setOf(Action.OPEN_CHATGPT,Action.OPEN_FLOW)) {
            awaitVisible(command,false)
            loading=true
            web.loadUrl(WebWorkflowPolicy.home(command.action))
            delay(800)
        }
        if(command.action==Action.FLOW_UPLOAD_IMAGE) {
            val uri=Uri.parse(command.value)
            check(uri.scheme=="content") { "Pilih ulang gambar di editor workflow." }
            withContext(Dispatchers.IO) {
                val mime=contentResolver.getType(uri) ?: ""
                check(mime in setOf("image/jpeg","image/png","image/webp")) { "Gambar harus JPEG, PNG, atau WebP." }
                contentResolver.openAssetFileDescriptor(uri,"r")?.use { check(it.length in -1L..20_000_000L){"Gambar maksimal 20MB"} } ?: error("Gambar tidak dapat dibaca. Pilih ulang dari perangkat.")
                contentResolver.openInputStream(uri)?.use { input ->
                    val header=ByteArray(32);val count=input.read(header)
                    check(count>0 && MediaPolicy.matches(mime,header.copyOf(count))){"Isi file bukan gambar yang didukung"}
                    var size=count.toLong();val buffer=ByteArray(8192)
                    while(true) { currentCoroutineContext().ensureActive();val read=input.read(buffer);if(read<0)break;size+=read;check(size<=20_000_000){"Gambar maksimal 20MB"} }
                } ?: error("Gambar tidak dapat dibaca")
            }
            upload=uri;chooserUsed=false
        }
        var phase="begin"
        while(true) {
            val result=invoke(command,phase)
            when(result.optString("status")) {
                "wait" -> { progress("${command.action.displayName()}\n${result.optString("note")}");delay(900) }
                "preparing" -> { phase="prepare";message.text="Membuka kontrol unggah gambar…";delay(700) }
                "dispatched" -> { phase="observe";progress("Aksi dikirim satu kali. Menunggu verifikasi…");delay(700) }
                "error" -> error(result.optString("note","Adapter situs tidak cocok"))
                "done" -> {
                    val output=result.optString("result")
                    val imageResult=command.action==Action.CHATGPT_COLLECT_RESULT && result.optString("kind")=="image"
                    if(command.action==Action.CHATGPT_COPY_RESULT || (command.action==Action.CHATGPT_COLLECT_RESULT && !imageResult)) {
                        check(output.isNotBlank() && output.length<=32000){"Jawaban kosong atau lebih dari 32000 karakter"}
                        awaitVisible(command);Handoffs.copy(this,output);WorkflowBrowserSession.chatResponse=output
                    }
                    if(command.action==Action.CHATGPT_DOWNLOAD_IMAGE || command.action==Action.FLOW_DOWNLOAD_VIDEO || imageResult) {
                        progress("Menyimpan media (maks. 50MB)…")
                        val saved=download(command,output)
                        message.text="Tersimpan di galeri BYGA: $saved";return saved
                    }
                    message.text="${command.action.displayName()} — terverifikasi"
                    return output
                }
                else -> error("Respons adapter tidak valid")
            }
        }
    }

    private suspend fun download(command:WebCommand,url:String):String {
        check(secureUrl(url) || url.startsWith("blob:")) { "Media tidak memiliki sumber HTTPS/blob yang didukung." }
        awaitVisible(command)
        val token="bygaMedia"+UUID.randomUUID().toString().replace("-","")
        val q=JSONObject.quote(token)
        val file=withContext(Dispatchers.IO){File.createTempFile("workflow-media-",".part",cacheDir)}
        try {
            // Fetch in the page's session, including authenticated/blob media. No cookies leave WebView.
            js("""window[$q]={controller:new AbortController()};fetch(${JSONObject.quote(url)},{credentials:'same-origin',signal:window[$q].controller.signal}).then(async r=>{
              if(!r.ok || Number(r.headers.get('content-length'))>52428800)throw Error('HTTP/size');
              const reader=r.body.getReader(),parts=[];let size=0;
              while(true){const part=await reader.read();if(part.done)break;size+=part.value.length;if(size>52428800){await reader.cancel();throw Error('size')};parts.push(part.value)}
              if(!size)throw Error('empty');return new Blob(parts,{type:r.headers.get('content-type')||''});
            }).then(b=>{if(window[$q])Object.assign(window[$q],{blob:b,size:b.size,mime:b.type})}).catch(()=>{if(window[$q])window[$q].error='Media gagal diambil (CORS/login/ukuran). Gunakan tombol download resmi situs; batas 50MB.'})""")
            var size:Long
            var mime:String
            withTimeout(90000) {
                while(true) {
                    awaitVisible(command)
                    val state=JSONObject(js("JSON.stringify({size:window[$q]?.size,mime:window[$q]?.mime,error:window[$q]?.error})"))
                    check(!state.has("error")){state.optString("error")}
                    if(state.has("size"))break
                    delay(300)
                }
            }
            val meta=JSONObject(js("JSON.stringify({size:window[$q].size,mime:window[$q].mime})"))
            size=meta.getLong("size");mime=meta.getString("mime").substringBefore(';')
            check(if(command.action!=Action.FLOW_DOWNLOAD_VIDEO)mime.startsWith("image/") else mime.startsWith("video/")){"Tipe media tidak sesuai langkah"}
            withContext(Dispatchers.IO){file.outputStream()}.use { stream ->
                var offset=0L
                while(offset<size) {
                    awaitVisible(command)
                    val end=minOf(offset+65536,size)
                    js("window[$q].chunk=null;window[$q].blob.slice($offset,$end).arrayBuffer().then(b=>{window[$q].chunk=btoa(String.fromCharCode(...new Uint8Array(b)))}).catch(()=>{window[$q].chunk='ERROR'})")
                    val chunk=withTimeout(10000) {
                        var data=""
                        while(data.isBlank()) { awaitVisible(command);data=js("window[$q]?.chunk || ''");if(data.isBlank())delay(30) }
                        data
                    }
                    val bytes=Base64.decode(chunk,Base64.DEFAULT)
                    check(bytes.size.toLong()==end-offset){"Media tidak lengkap"}
                    withContext(Dispatchers.IO){stream.write(bytes)}
                    offset=end
                    message.text="Mengunduh ${offset*100/size}% • maks. 50MB"
                }
            }
            awaitVisible(command)
            return when(val saved=DirectDownload.saveLocal(this,file,mime)) {
                is DownloadResult.Saved -> saved.uri.toString()
                is DownloadResult.Blocked -> error(saved.code)
            }
        } finally {
            withContext(NonCancellable+Dispatchers.IO){file.delete()}
            if(WebWorkflowPolicy.permits(command.action,web.url ?: ""))withContext(NonCancellable) {
                withTimeoutOrNull(1000){js("window[$q]?.controller?.abort();delete window[$q]")}
            }
        }
    }
}
