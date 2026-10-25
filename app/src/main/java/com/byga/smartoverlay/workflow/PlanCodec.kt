package com.byga.smartoverlay.workflow

import org.json.JSONArray
import org.json.JSONObject
object WorkflowCodec {
    fun encode(plan: Plan): String = JSONObject().put("name",plan.name).put("version",1).put("steps",JSONArray().apply { plan.steps.forEach { put(JSONObject().put("action",it.action.name).put("value",it.value).put("package",it.packageName).put("selector",it.selector)) } }).toString()
    fun decode(raw: String): Plan {
        require(raw.length<=128000)
        val j=JSONObject(raw); require(j.keys().asSequence().all { it in setOf("name","version","steps") }); require(j.optInt("version",1)==1)
        val a=j.getJSONArray("steps"); require(a.length() in 1..50)
        return Plan(j.getString("name"),(0 until a.length()).map { index ->
            val s=a.getJSONObject(index); require(s.keys().asSequence().all { it in setOf("action","value","package","selector") })
            Step(Action.valueOf(s.getString("action")),s.optString("value"),s.optString("package"),s.optString("selector"))
        }).also { require(it.validate().isEmpty()) }
    }
    val templates=listOf(
        Plan("Caption AI → tinjau → salin",listOf(Step(Action.GENERATE_AI_TEXT,"Buat 3 caption Indonesia singkat untuk video produktivitas."),Step(Action.REQUEST_USER_APPROVAL,"Tinjau teks hasil AI sebelum disalin."),Step(Action.COPY_TEXT,"{{output}}"))),
        Plan("Prompt Flow → salin → buka browser",listOf(Step(Action.COPY_TEXT,"Video sinematik cahaya pagi, kamera bergerak perlahan."),Step(Action.REQUEST_USER_APPROVAL,"Teks siap. Buka Flow secara manual dari Apps."))),
        Plan("Siapkan prompt ChatGPT",listOf(Step(Action.COPY_TEXT,"Bantu buat tiga ide konten yang relevan untuk audiens Indonesia."),Step(Action.LAUNCH_APP,packageName="com.openai.chatgpt")))
    )
}
