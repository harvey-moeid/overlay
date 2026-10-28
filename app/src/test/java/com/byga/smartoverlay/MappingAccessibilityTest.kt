package com.byga.smartoverlay

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.*
import com.byga.smartoverlay.mapping.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class MappingFakeService:AccessibilityService() {
    var screen:AccessibilityNodeInfo?=null
    var readThread:Thread?=null
    override fun getRootInActiveWindow():AccessibilityNodeInfo? { readThread=Thread.currentThread();return screen }
    override fun getWindows():MutableList<AccessibilityWindowInfo> = mutableListOf()
    override fun onAccessibilityEvent(event:AccessibilityEvent?)=Unit
    override fun onInterrupt()=Unit
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MappingAccessibilityTest {
    private lateinit var service:MappingFakeService
    private lateinit var adapter:MappingAccessibility
    private var clicks=0
    @Before fun setup(){service=Robolectric.buildService(MappingFakeService::class.java).create().get();adapter=MappingAccessibility(service);MappingBrowserContext.visible=false;MappingBrowserContext.url=""}
    @After fun cleanup(){MappingBrowserContext.visible=false;MappingBrowserContext.url="";service.onDestroy()}
    private fun node(pkg:String=MappingPolicy.CHATGPT,label:String="",id:String="",clickable:Boolean=false):AccessibilityNodeInfo=AccessibilityNodeInfo.obtain().apply {
        packageName=pkg;className=if(clickable)"android.widget.Button" else "android.widget.FrameLayout";text=label;viewIdResourceName=id
        isVisibleToUser=true;isEnabled=true;isClickable=clickable;setBoundsInScreen(Rect(20,100,200,180))
        if(clickable)addAction(AccessibilityNodeInfo.ACTION_CLICK)
        shadowOf(this).setRefreshReturnValue(true)
        shadowOf(this).setOnPerformActionListener { _,_->clicks++;true }
    }
    private fun attach(root:AccessibilityNodeInfo,child:AccessibilityNodeInfo){shadowOf(root).addChild(child)}
    private fun scene():Pair<AccessibilityNodeInfo,AccessibilityNodeInfo>{val root=node();val button=node(label="Kirim",id="app:id/send",clickable=true);attach(root,button);service.screen=root;return root to button}
    private suspend fun saved():ButtonMapping { val s=adapter.snapshot();return ButtonMapping(name="Kirim",scope=s.scope,identity=s.candidates.single().identity) }
    @Test fun remoteTreeIsReadOffTheCallingThread()=runTest {
        scene();val caller=Thread.currentThread();adapter.snapshot()
        assertNotNull(service.readThread);assertNotSame(caller,service.readThread)
    }
    @Test fun observingDoesNotClick()=runTest {scene();val s=adapter.snapshot();assertEquals(1,s.candidates.size);assertEquals(0,clicks)}
    @Test fun matchedNativeActionIsDispatchedExactlyOnce()=runTest {scene();val m=saved();val permit=MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime());assertTrue(adapter.click(m,permit).startsWith("CLICK_ACCEPTED"));assertEquals(1,clicks);try{adapter.click(m,permit);fail()}catch(_:IllegalStateException){};assertEquals(1,clicks)}
    @Test fun nativeActionFollowsMovedNodeInsteadOfOldPoint()=runTest {val(_,button)=scene();val m=saved();button.setBoundsInScreen(Rect(600,900,850,1000));adapter.click(m,MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime()));assertEquals(1,clicks)}
    @Test fun nativeAmbiguityCausesNoAction()=runTest {val(root,_)=scene();val m=saved();attach(root,node(label="Kirim",id="app:id/send",clickable=true));try{adapter.click(m,MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime()));fail()}catch(_:IllegalStateException){};assertEquals(0,clicks)}
    @Test fun passwordScreenCannotBeMapped()=runTest {val(root,_)=scene();attach(root,node().apply{isPassword=true;isEditable=true});try{adapter.snapshot();fail()}catch(_:IllegalStateException){};assertEquals(0,clicks)}
    @Test fun lockedDeviceCannotBeMapped()=runTest {scene();shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true);try{adapter.snapshot();fail()}catch(_:IllegalStateException){};assertEquals(0,clicks)}
    @Test fun anotherAppCannotReceiveMappedClick()=runTest {scene();val m=saved();service.screen=node(pkg="com.bank.app");try{adapter.click(m,MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime()));fail()}catch(_:IllegalStateException){};assertEquals(0,clicks)}
    @Test fun chromeDomainAndPageContentAreScoped()=runTest {
        val root=node(MappingPolicy.CHROME);val bar=node(MappingPolicy.CHROME,"labs.google/fx/tools/flow/project/123","com.android.chrome:id/url_bar")
        attach(root,bar);attach(root,node(MappingPolicy.CHROME,"Toolbar",clickable=true))
        val web=node(MappingPolicy.CHROME).apply{className="android.webkit.WebView"};attach(root,web);attach(web,node(MappingPolicy.CHROME,"Generate","web:generate",true));service.screen=root
        val snapshot=adapter.snapshot();assertEquals("flow-project",snapshot.scope.screen);assertEquals(1,snapshot.candidates.size);assertEquals("Generate",snapshot.candidates.single().identity.label)
        bar.text="evil.com/fx/tools/flow";try{adapter.snapshot();fail()}catch(_:IllegalStateException){}
    }
    @Test fun editingChromeAddressIsNotEvidenceOfLoadedSite()=runTest {
        val root=node(MappingPolicy.CHROME);attach(root,node(MappingPolicy.CHROME,"chatgpt.com","com.android.chrome:id/url_bar").apply{isFocused=true});service.screen=root
        try{adapter.snapshot();fail()}catch(_:IllegalStateException){};assertEquals(0,clicks)
    }
    @Test fun bygaMappingsAreRestrictedToPracticeArea()=runTest {
        val root=node(MappingPolicy.BYGA);attach(root,node(MappingPolicy.BYGA,"Stop","toolbar:stop",true));val practice=node(MappingPolicy.BYGA).apply{contentDescription="byga_mapping_test_screen"};attach(root,practice);attach(practice,node(MappingPolicy.BYGA,"Latihan","practice",true));service.screen=root
        val s=adapter.snapshot();assertEquals("mapping-test",s.scope.screen);assertEquals(listOf("Latihan"),s.candidates.map{it.identity.label})
    }
    @Test fun editableValuesAreNeverPartOfDiscoveryIdentity()=runTest {
        val root=node();val field=node(id="app:id/prompt").apply{isEditable=true;className="android.widget.EditText";text="PRIVATE DRAFT 987654";hintText="Prompt";addAction(AccessibilityNodeInfo.ACTION_SET_TEXT)}
        attach(root,field);service.screen=root
        val item=adapter.snapshot().candidates.single()
        assertEquals(ElementKind.INPUT,item.kind);assertFalse(item.identity.toString().contains("PRIVATE"));assertTrue(item.identity.label.isBlank());assertEquals(0,clicks)
    }
    @Test fun mappedInputVerifiesMatchingObservedValueWithoutSend()=runTest {
        val root=node();val field=node(id="app:id/prompt").apply{isEditable=true;className="android.widget.EditText";text="hello";addAction(AccessibilityNodeInfo.ACTION_SET_TEXT)}
        shadowOf(field).setOnPerformActionListener{action,args->assertEquals(AccessibilityNodeInfo.ACTION_SET_TEXT,action);field.text=args?.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE);clicks++;true}
        attach(root,field);service.screen=root
        val s=adapter.snapshot();val m=ButtonMapping(name="Prompt",scope=s.scope,identity=s.candidates.single().identity,elementKind=ElementKind.INPUT)
        assertEquals("INPUT_VERIFIED",adapter.perform(m,MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime(),"INPUT",mappingHash("hello")),"INPUT","hello"));assertEquals(1,clicks)
    }
    @Test fun acceptedSetTextWithoutValueChangeIsUncertain()=runTest {
        val root=node();val field=node(id="app:id/prompt").apply{isEditable=true;className="android.widget.EditText";addAction(AccessibilityNodeInfo.ACTION_SET_TEXT)}
        attach(root,field);service.screen=root
        val s=adapter.snapshot();val m=ButtonMapping(name="Prompt",scope=s.scope,identity=s.candidates.single().identity,elementKind=ElementKind.INPUT)
        try{adapter.perform(m,MappingClickPermit(m.fingerprint(),SystemClock.elapsedRealtime(),"INPUT",mappingHash("hello")),"INPUT","hello");fail()}catch(_:IllegalStateException){}
        assertEquals(1,clicks)
    }
    @Test fun checkboxTabAndScrollableContainersAreDiscovered()=runTest {
        val root=node();attach(root,node(label="Aktif",id="switch",clickable=true).apply{isCheckable=true})
        attach(root,node(id="list").apply{isScrollable=true;addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)})
        attach(root,node(label="Home",id="tab",clickable=true).apply{className="android.widget.TabWidget"})
        service.screen=root;assertEquals(setOf(ElementKind.TOGGLE,ElementKind.SCROLL,ElementKind.TAB),adapter.snapshot().candidates.map{it.kind}.toSet());assertEquals(0,clicks)
    }
    @Test fun foreignPackageChildCannotBecomeCandidate()=runTest {
        val root=node();attach(root,node(pkg="com.other.app",label="Generate",clickable=true));service.screen=root
        assertTrue(adapter.snapshot().candidates.isEmpty())
    }
    @Test fun otpFieldIdentifiedOnlyByResourceIdBlocksWholeSnapshot()=runTest {
        val(root,_)=scene();attach(root,node(id="app:id/otp_code").apply{isEditable=true})
        try{adapter.snapshot();fail()}catch(_:IllegalStateException){};assertEquals(0,clicks)
    }

}
