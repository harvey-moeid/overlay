package com.byga.smartoverlay.data

import com.byga.smartoverlay.workflow.WorkflowCodec
import com.byga.smartoverlay.profiles.AccountProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ConfigTransfer {
    val kinds=setOf("workflow","account","prompt","app")
    fun export(rows:List<Record>):String {
        val allowed=rows.filter{it.kind in kinds};require(allowed.size<=100)
        return JSONObject().put("format","byga-local-config").put("version",1).put("records",JSONArray().apply{allowed.forEach{row->put(JSONObject().put("kind",row.kind).put("body",row.body))}}).toString()
    }
    fun parse(raw:String):List<Record>{
        require(raw.length<=256000);val j=JSONObject(raw);require(j.optString("format")=="byga-local-config"&&j.optInt("version")==1)
        require(j.keys().asSequence().all{it in setOf("format","version","records")});val array=j.getJSONArray("records");require(array.length()<=100)
        return (0 until array.length()).map{index->
            val item=array.getJSONObject(index);require(item.keys().asSequence().toSet()==setOf("kind","body"));val kind=item.getString("kind");require(kind in kinds);val body=item.getString("body");require(body.length<=128000)
            val id=UUID.randomUUID().toString()
            val sanitized=when(kind){
                "workflow"->WorkflowCodec.encode(WorkflowCodec.decode(body))
                "account"->AccountProfile.decode(Record(kind,id,body)).copy(id=id,status="UNKNOWN",confirmedAt=0).encode()
                "prompt"->{val p=runCatching{JSONObject(body)}.getOrElse{JSONObject().put("title","Prompt impor").put("text",body)};JSONObject().put("title",p.optString("title").take(120)).put("text",p.optString("text").take(32000)).put("tags",p.optString("tags").take(500)).put("favorite",false).toString()}
                else->{require(body.matches(Regex("[A-Za-z][A-Za-z0-9_]*([.][A-Za-z0-9_]+)+")));body}
            }
            Record(kind,id,sanitized)
        }
    }
}
