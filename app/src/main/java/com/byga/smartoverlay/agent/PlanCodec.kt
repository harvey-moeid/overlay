package com.byga.smartoverlay.agent

import org.json.JSONObject
import org.json.JSONArray
object PlanCodec {
    fun local(goal: String, pkg: String) = AgentPlan(goal,listOf(ProposedCall("set_text",pkg,"byga_safe_input",goal,goal),ProposedCall("verify_state",pkg,"byga_safe_input",goal,goal)))
    fun encode(plan: AgentPlan): String = JSONObject().put("goal",plan.goal).put("calls",JSONArray().apply {
        plan.calls.forEach { put(JSONObject().put("tool",it.tool).put("package",it.packageName).put("selector",it.selector).put("value",it.value).put("expected",it.expected)) }
    }).toString()
    fun decode(raw: String): AgentPlan {
        require(raw.length<=65536)
        val json=JSONObject(raw)
        require(json.keys().asSequence().toSet()==setOf("goal","calls"))
        val calls=json.getJSONArray("calls"); require(calls.length() in 1..30)
        return AgentPlan(json.getString("goal"),(0 until calls.length()).map { i ->
            val c=calls.getJSONObject(i)
            require(c.keys().asSequence().toSet()==setOf("tool","package","selector","value","expected"))
            ProposedCall(c.getString("tool"),c.getString("package"),c.getString("selector"),c.getString("value"),c.getString("expected"))
        }).also { require(it.valid()) }
    }
}
