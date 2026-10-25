package com.byga.smartoverlay.profiles

import org.json.JSONObject
import com.byga.smartoverlay.data.Record

data class TargetApp(val name: String,val packageName: String)
val targetApps=listOf(TargetApp("ChatGPT","com.openai.chatgpt"),TargetApp("Claude","com.anthropic.claude"),TargetApp("TikTok","com.zhiliaoapp.musically"),TargetApp("Instagram","com.instagram.android"))
data class AccountProfile(val id: String,val packageName: String,val label: String,val alias: String,val status: String="UNKNOWN",val confirmedAt: Long=0) {
    fun encode()=JSONObject().put("package",packageName).put("label",label).put("alias",alias).put("status",status).put("confirmedAt",confirmedAt).toString()
    companion object {
        fun decode(row: Record): AccountProfile = runCatching { val j=JSONObject(row.body); AccountProfile(row.id,j.getString("package"),j.getString("label"),j.optString("alias"),j.optString("status","UNKNOWN"),j.optLong("confirmedAt",0)) }.getOrElse { AccountProfile(row.id,"",row.body.substringBefore(" • UNKNOWN"),"",status="UNKNOWN") }
    }
}
object AccountGuard {
    fun canHandoff(account: AccountProfile?, pkg: String, reviewed: Boolean, now: Long): Boolean = account!=null && pkg.isNotBlank() && account.packageName==pkg && account.alias.isNotBlank() && reviewed && account.status=="USER_CONFIRMED" && now-account.confirmedAt in 0..300000
}
