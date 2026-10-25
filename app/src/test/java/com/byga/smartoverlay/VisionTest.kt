package com.byga.smartoverlay
import com.byga.smartoverlay.ai.*
import org.json.JSONObject
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
class VisionTest{
    private fun body(type:ProviderType):JSONObject{val request=ProviderWire.vision(ProviderConfig("x",type,ProviderCatalog.get(type).baseUrl,"vision-model"),"fake-key","Describe","ZmFrZQ==","image/jpeg");val b=Buffer();request.body!!.writeTo(b);return JSONObject(b.readUtf8())}
    @Test fun openAiCompatibleVisionUsesImageContentNotPlainText(){val j=body(ProviderType.OPENAI);val content=j.getJSONArray("messages").getJSONObject(0).getJSONArray("content");assertEquals("image_url",content.getJSONObject(1).getString("type"));assertEquals("data:image/jpeg;base64,ZmFrZQ==",content.getJSONObject(1).getJSONObject("image_url").getString("url"))}
    @Test fun anthropicVisionUsesBase64Source(){val c=body(ProviderType.ANTHROPIC).getJSONArray("messages").getJSONObject(0).getJSONArray("content");assertEquals("base64",c.getJSONObject(0).getJSONObject("source").getString("type"));assertEquals("image/jpeg",c.getJSONObject(0).getJSONObject("source").getString("media_type"))}
    @Test fun geminiVisionUsesInlineData(){val c=body(ProviderType.GEMINI).getJSONArray("contents").getJSONObject(0).getJSONArray("parts");assertEquals("ZmFrZQ==",c.getJSONObject(0).getJSONObject("inline_data").getString("data"))}
    @Test fun cohereVisionNotFalselyAdvertised(){try{body(ProviderType.COHERE);fail()}catch(_:IllegalArgumentException){}}
    @Test fun nonImageMimeAndOversizedInputAreRejected(){for(mime in listOf("text/html","application/octet-stream")){try{ProviderWire.vision(ProviderConfig("x",ProviderType.OPENAI,"https://example.com","m"),"fake","x","ZmFrZQ==",mime);fail()}catch(_:IllegalArgumentException){}};try{ProviderWire.vision(ProviderConfig("x",ProviderType.OPENAI,"https://example.com","m"),"fake","x","a".repeat(3_000_001),"image/jpeg");fail()}catch(_:IllegalArgumentException){}}
}
