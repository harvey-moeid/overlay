package com.byga.smartoverlay.workflow

import org.json.JSONArray
import org.json.JSONObject
object WorkflowCodec {
    fun encode(plan: Plan): String = JSONObject().put("name",plan.name).put("version",1).put("steps",JSONArray().apply { plan.steps.forEach { put(JSONObject().put("action",it.action.name).put("value",it.value).put("package",it.packageName).put("selector",it.selector).put("argument",it.argument).put("timeoutMs",it.timeoutMs).put("retries",it.retries)) } }).toString()
    fun decode(raw: String): Plan {
        require(raw.length<=128000)
        val j=JSONObject(raw); require(j.keys().asSequence().all { it in setOf("name","version","steps") }); require(j.optInt("version",1)==1)
        val a=j.getJSONArray("steps"); require(a.length() in 1..50)
        return Plan(j.getString("name"),(0 until a.length()).map { index ->
            val s=a.getJSONObject(index); require(s.keys().asSequence().all { it in setOf("action","value","package","selector","argument","timeoutMs","retries") })
            Step(Action.valueOf(s.getString("action")),s.optString("value"),s.optString("package"),s.optString("selector"),s.optString("argument"),s.optLong("timeoutMs",600000),s.optInt("retries"))
        }).also { require(it.validate().isEmpty()) }
    }
    val templates=listOf(
        Plan("ChatGPT → prompt → salin teks / unduh gambar",listOf(
            Step(Action.OPEN_CHATGPT),
            Step(Action.CHATGPT_SEND_PROMPT,"Bantu buat tiga ide konten yang relevan untuk audiens Indonesia."),
            Step(Action.CHATGPT_COLLECT_RESULT))),
        Plan("Flow → unggah gambar → generate → unduh video",listOf(
            Step(Action.OPEN_FLOW),Step(Action.FLOW_CREATE_PROJECT),Step(Action.FLOW_UPLOAD_IMAGE),
            Step(Action.FLOW_SET_PROMPT,"Video sinematik cahaya pagi, kamera bergerak perlahan. Pertahankan karakter pada gambar."),
            Step(Action.FLOW_GENERATE),Step(Action.FLOW_DOWNLOAD_VIDEO))),
        Plan("ChatGPT gambar → Flow → video",listOf(
            Step(Action.OPEN_CHATGPT),Step(Action.CHATGPT_SEND_PROMPT,"Buat gambar seekor robot biru lucu di taman pada pagi hari, format landscape."),
            Step(Action.CHATGPT_DOWNLOAD_IMAGE),Step(Action.OPEN_FLOW),Step(Action.FLOW_CREATE_PROJECT),
            Step(Action.FLOW_UPLOAD_IMAGE,"{{output}}"),
            Step(Action.FLOW_SET_PROMPT,"Animasikan robot melambaikan tangan, kamera bergerak perlahan, cahaya pagi lembut."),
            Step(Action.FLOW_GENERATE),Step(Action.FLOW_DOWNLOAD_VIDEO))),
        Plan("Buka aplikasi terinstal",listOf(Step(Action.LAUNCH_APP))),
        Plan("Caption AI → tinjau → salin",listOf(Step(Action.GENERATE_AI_TEXT,"Buat 3 caption Indonesia singkat untuk video produktivitas."),Step(Action.REQUEST_USER_APPROVAL,"Tinjau teks hasil AI sebelum disalin."),Step(Action.COPY_TEXT,"{{output}}")))
    ).let{templates->templates+templates[2].copy(name="CHATGPT IMAGE → GOOGLE FLOW VIDEO → SAVE TO GALLERY")}
}
