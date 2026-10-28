package com.byga.smartoverlay.mapping

import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class MappingScope(val packageName:String,val site:String="",val screen:String="")
data class TargetIdentity(val resourceId:String="",val label:String="",val description:String="",val role:String="",val parentId:String="",val structure:String="") {
    fun stable()=resourceId.isNotBlank() || label.isNotBlank() || description.isNotBlank()
}
data class TargetBounds(val left:Int,val top:Int,val right:Int,val bottom:Int) {
    fun contains(x:Float,y:Float)=x>=left && x<right && y>=top && y<bottom
    val area get()=(right-left).toLong()*(bottom-top)
}
enum class ElementKind { BUTTON, INPUT, TOGGLE, TAB, SCROLL }
data class MappingCandidate(val key:String,val identity:TargetIdentity,val bounds:TargetBounds,
    val kind:ElementKind=ElementKind.BUTTON,val enabled:Boolean=true,val clickable:Boolean=true,
    val checked:Boolean=false,val selected:Boolean=false,val windowId:Int=-1)
data class MappingSnapshot(val scope:MappingScope,val candidates:List<MappingCandidate>,val width:Int,val height:Int,val rotation:Int,val layoutHash:String,val observedAt:Long,val windowId:Int=-1)
enum class MappingStatus { FOUND, AMBIGUOUS, NOT_FOUND, STALE, BLOCKED, NEEDS_REVIEW }
data class MappingResolution(val status:MappingStatus,val candidate:MappingCandidate?=null,val candidates:List<MappingCandidate> = emptyList(),val confidence:Float=0f,val reason:String="")
enum class MappingKind { SEMANTIC, POINT }
data class ButtonMapping(
    val id:String=UUID.randomUUID().toString(),val name:String,val scope:MappingScope,
    val kind:MappingKind=MappingKind.SEMANTIC,val identity:TargetIdentity=TargetIdentity(),
    val x:Float=0f,val y:Float=0f,val width:Int=0,val height:Int=0,val rotation:Int=0,
    val layoutHash:String="",val askEveryTime:Boolean=false,val enabled:Boolean=true,val elementKind:ElementKind=ElementKind.BUTTON
) {
    fun validate():Boolean=id.length in 1..120 && name.isNotBlank() && name.length<=80 && MappingPolicy.allowedScope(scope) &&
        if(kind==MappingKind.SEMANTIC)identity.stable() else x>=0f && x<1f && y>=0f && y<1f && width in 1..10000 && height in 1..20000 && rotation in 0..3 && layoutHash.isNotBlank() && askEveryTime
    fun encode():String=JSONObject().put("version",1).put("id",id).put("name",name)
        .put("package",scope.packageName).put("site",scope.site).put("screen",scope.screen)
        .put("kind",kind.name).put("resource",identity.resourceId).put("label",identity.label)
        .put("description",identity.description).put("role",identity.role).put("parent",identity.parentId)
        .put("x",x.toDouble()).put("y",y.toDouble()).put("width",width).put("height",height).put("rotation",rotation)
        .put("layout",layoutHash).put("ask",askEveryTime).put("enabled",enabled).apply {
            // Preserve fingerprints of v1 mappings already referenced by saved workflows.
            if(identity.structure.isNotBlank())put("structure",identity.structure)
            if(elementKind!=ElementKind.BUTTON)put("elementKind",elementKind.name)
        }.toString()
    fun fingerprint()=mappingHash(encode())
    companion object {
        fun decode(raw:String):ButtonMapping {
            require(raw.length<=8192)
            val j=JSONObject(raw);require(j.getInt("version")==1)
            return ButtonMapping(j.getString("id"),j.getString("name"),MappingScope(j.getString("package"),j.optString("site"),j.optString("screen")),
                MappingKind.valueOf(j.getString("kind")),TargetIdentity(j.optString("resource"),j.optString("label"),j.optString("description"),j.optString("role"),j.optString("parent"),j.optString("structure")),
                j.optDouble("x",0.0).toFloat(),j.optDouble("y",0.0).toFloat(),j.optInt("width"),j.optInt("height"),j.optInt("rotation"),j.optString("layout"),j.optBoolean("ask"),j.optBoolean("enabled",true),ElementKind.valueOf(j.optString("elementKind","BUTTON")))
                .also{require(it.validate())}
        }
    }
}
fun mappingHash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}

object MappingPolicy {
    const val BYGA="com.byga.smartoverlay"
    const val CHROME="com.android.chrome"
    const val CHATGPT="com.openai.chatgpt"
    @Volatile var userPackages:Set<String> = emptySet()
    val packages get()=setOf(BYGA,CHROME,CHATGPT)+userPackages
    fun site(address:String):Pair<String,String>?=runCatching {
        val raw=address.trim()
        require(raw.isNotBlank() && !raw.any{it.isWhitespace()})
        val uri=URI(if(raw.contains("://"))raw else "https://$raw").normalize()
        require(uri.scheme=="https" && uri.userInfo==null && (uri.port==-1 || uri.port==443))
        when(uri.host) {
            "chatgpt.com" -> {
                require(!Regex("(?i)/(auth|login|signup|settings)(/|$)").containsMatchIn(uri.path ?: ""))
                "https://chatgpt.com" to "chat"
            }
            "labs.google" -> {
                val path=uri.path ?: ""
                require(path=="/fx/tools/flow" || path.startsWith("/fx/tools/flow/"))
                "https://labs.google/fx/tools/flow" to if(path.contains("/project/"))"flow-project" else "flow-home"
            }
            else -> error("Situs belum didukung")
        }
    }.getOrNull()
    fun allowedScope(scope:MappingScope)=scope.packageName in packages && scope.screen.isNotBlank() &&
        if(scope.packageName==CHATGPT || scope.packageName in userPackages)scope.site.isBlank() else if(scope.site.isBlank())scope.packageName==BYGA && scope.screen=="mapping-test" else site(scope.site)?.first==scope.site && if(scope.site=="https://chatgpt.com")scope.screen=="chat" else scope.screen in setOf("flow-project","flow-home")

    /** Coordinates select a candidate during teaching; semantic replay never uses those coordinates. */
    fun candidateAt(snapshot:MappingSnapshot,x:Float,y:Float):MappingCandidate?=
        snapshot.candidates.filter{it.bounds.contains(x,y)}.minByOrNull{it.bounds.area}
    fun resolve(mapping:ButtonMapping,snapshot:MappingSnapshot,now:Long):MappingCandidate? =
        inspect(mapping,snapshot,now).takeIf{it.status==MappingStatus.FOUND}?.candidate
    fun inspect(mapping:ButtonMapping,snapshot:MappingSnapshot,now:Long):MappingResolution {
        if(!mapping.enabled || !mapping.validate() || snapshot.scope!=mapping.scope)return MappingResolution(MappingStatus.BLOCKED,reason="Package, situs, layar, atau izin mapping berbeda")
        if(now-snapshot.observedAt !in 0..3000)return MappingResolution(MappingStatus.STALE,reason="Snapshot kedaluwarsa")
        if(mapping.kind==MappingKind.POINT)return MappingResolution(if(pointAllowed(mapping,snapshot,now))MappingStatus.NEEDS_REVIEW else MappingStatus.STALE,reason="Posisi manual perlu konfirmasi setiap eksekusi")
        val target=mapping.identity
        val matches=snapshot.candidates.filter { c ->
            val id=c.identity
            (target.resourceId.isBlank() || id.resourceId==target.resourceId) &&
            (target.label.isBlank() || id.label==target.label) &&
            (target.description.isBlank() || id.description==target.description) &&
            (target.role.isBlank() || id.role==target.role) &&
            (target.parentId.isBlank() || id.parentId==target.parentId) &&
            (target.structure.isBlank() || id.structure==target.structure) &&
            (c.kind==mapping.elementKind || (mapping.elementKind==ElementKind.BUTTON && c.clickable && c.kind!=ElementKind.INPUT))
        }
        if(matches.size>1)return MappingResolution(MappingStatus.AMBIGUOUS,candidates=matches,reason="Lebih dari satu target cocok; pilih ulang")
        matches.singleOrNull()?.let { c ->
            if(!c.enabled || (snapshot.windowId!=-1 && c.windowId!=snapshot.windowId))return MappingResolution(MappingStatus.BLOCKED,reason="Target nonaktif atau window berubah")
            return MappingResolution(MappingStatus.FOUND,c,listOf(c),1f,"Identitas dan scope cocok tepat")
        }
        val similar=snapshot.candidates.filter{c->c.kind==mapping.elementKind && c.identity.role==target.role &&
            ((target.label.isNotBlank() && c.identity.label==target.label) || (target.description.isNotBlank() && c.identity.description==target.description))}
        return MappingResolution(if(similar.isNotEmpty())MappingStatus.NEEDS_REVIEW else MappingStatus.NOT_FOUND,candidates=similar,confidence=if(similar.isNotEmpty())0.5f else 0f,reason=if(similar.isNotEmpty())"Atribut berubah. Tinjau dan petakan ulang; tidak diklik otomatis" else "Target tidak ditemukan")
    }
    fun pointAllowed(mapping:ButtonMapping,snapshot:MappingSnapshot,now:Long)=mapping.validate() && mapping.enabled &&
        mapping.kind==MappingKind.POINT && mapping.askEveryTime && snapshot.scope==mapping.scope &&
        now-snapshot.observedAt in 0..3000 && mapping.width==snapshot.width && mapping.height==snapshot.height &&
        mapping.rotation==snapshot.rotation && mapping.layoutHash==snapshot.layoutHash
}

class MappingClickPermit(val hash:String,private val issuedAt:Long,private val operation:String="CLICK",private val valueHash:String=mappingHash("")) {
    private val consumed=AtomicBoolean(false)
    fun claim(mapping:ButtonMapping,now:Long,requestedOperation:String="CLICK",value:String="")=operation==requestedOperation && valueHash==mappingHash(value) && hash==mapping.fingerprint() && now-issuedAt in 0..15000 && consumed.compareAndSet(false,true)
}

/** Updated by the native browser Activity, never by JavaScript from a website. */
object MappingBrowserContext {
    @Volatile var url=""
    @Volatile var visible=false
}
