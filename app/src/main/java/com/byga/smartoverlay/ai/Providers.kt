package com.byga.smartoverlay.ai

import com.byga.smartoverlay.workflow.secureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ProviderType { OPENAI, ANTHROPIC, GEMINI, OPENROUTER, GROQ, COHERE, HUGGING_FACE, MISTRAL, NVIDIA_NIM, CUSTOM }
data class ProviderConfig(val id: String, val type: ProviderType, val baseUrl: String, val model: String)
sealed class AiResult { data class Text(val value: String): AiResult(); data class Failure(val code: String): AiResult() }
interface AiProviderAdapter { suspend fun text(prompt: String): AiResult }
class FakeProvider: AiProviderAdapter { override suspend fun text(prompt: String) = AiResult.Text("Konsep lokal: $prompt") }
class Fallback(private val adapters: List<AiProviderAdapter>) : AiProviderAdapter {
    override suspend fun text(prompt: String): AiResult {
        for (adapter in adapters.take(3)) {
            val result = try { adapter.text(prompt) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { AiResult.Failure("NETWORK") }
            if (result is AiResult.Text) return result
        }
        return AiResult.Failure("ALL_PROVIDERS_FAILED")
    }
}
internal object ProviderWire {
    private fun auth(builder: Request.Builder, type: ProviderType, secret: String): Request.Builder = when(type) {
        ProviderType.ANTHROPIC -> builder.header("x-api-key",secret).header("anthropic-version","2023-06-01")
        ProviderType.GEMINI -> builder.header("x-goog-api-key",secret)
        else -> builder.header("Authorization","Bearer $secret")
    }
    fun chat(config: ProviderConfig, secret: String, prompt: String): Request {
        val base=config.baseUrl.trimEnd('/')
        val body=JSONObject()
        val path=when(config.type) {
            ProviderType.ANTHROPIC -> "/messages"
            ProviderType.COHERE -> "/v2/chat"
            ProviderType.GEMINI -> "/models/${java.net.URLEncoder.encode(config.model,"UTF-8")}:generateContent"
            else -> "/chat/completions"
        }
        if(config.type==ProviderType.GEMINI) {
            body.put("contents",JSONArray().put(JSONObject().put("parts",JSONArray().put(JSONObject().put("text",prompt)))))
        } else {
            body.put("model",config.model).put("max_tokens",512).put("stream",false)
                .put("messages",JSONArray().put(JSONObject().put("role","user").put("content",prompt)))
        }
        return auth(Request.Builder().url(base+path),config.type,secret).post(body.toString().toRequestBody("application/json".toMediaType())).build()
    }
    fun models(type: ProviderType, baseUrl: String, secret: String): Request {
        val path=when(type) {
            ProviderType.COHERE -> "/v1/models?endpoint=chat&page_size=1000"
            ProviderType.GEMINI -> "/models?pageSize=1000"
            else -> "/models"
        }
        return auth(Request.Builder().url(baseUrl.trimEnd('/')+path),type,secret).get().build()
    }
    private fun blocks(array: JSONArray): String = (0 until array.length()).mapNotNull { index ->
        array.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() }
    }.joinToString("\n")
    fun text(type: ProviderType, json: JSONObject): String = when(type) {
        ProviderType.ANTHROPIC -> blocks(json.getJSONArray("content"))
        ProviderType.COHERE -> blocks(json.getJSONObject("message").getJSONArray("content"))
        ProviderType.GEMINI -> blocks(json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts"))
        else -> json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    }
    fun modelIds(type: ProviderType, json: JSONObject): List<String> {
        val array=if(type in setOf(ProviderType.COHERE,ProviderType.GEMINI)) json.getJSONArray("models") else json.getJSONArray("data")
        return (0 until array.length()).mapNotNull { index ->
            val item=array.getJSONObject(index)
            if(type==ProviderType.GEMINI) {
                val methods=item.optJSONArray("supportedGenerationMethods") ?: return@mapNotNull null
                if((0 until methods.length()).none { methods.optString(it)=="generateContent" }) return@mapNotNull null
            }
            item.optString(if(type in setOf(ProviderType.COHERE,ProviderType.GEMINI)) "name" else "id")
                .removePrefix(if(type==ProviderType.GEMINI) "models/" else "").takeIf { it.isNotBlank() && it.length<=256 }
        }.distinct().sorted().take(1000)
    }
}
sealed class ModelListResult {
    data class Models(val ids: List<String>, val partial: Boolean=false): ModelListResult()
    data class Failure(val code: String): ModelListResult()
}
class HttpProvider(private val config: ProviderConfig, private val secret: String, private val client: OkHttpClient = defaultClient()) : AiProviderAdapter {
    companion object {
        internal fun defaultClient() = OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    }
    private fun read(response: Response): JSONObject? {
        val source=response.body?.source() ?: return null
        if(source.request(1_048_577)) return null
        return JSONObject(source.readUtf8())
    }
    override suspend fun text(prompt: String): AiResult = withContext(Dispatchers.IO) {
        if(!ProviderCatalog.validBaseUrl(config.baseUrl) || config.model.isBlank() || secret.isBlank()) return@withContext AiResult.Failure("INVALID_CONFIG")
        try {
            client.newCall(ProviderWire.chat(config,secret,prompt)).execute().use { response ->
                if(!response.isSuccessful) return@withContext AiResult.Failure("HTTP_${response.code}")
                val json=read(response) ?: return@withContext AiResult.Failure("EMPTY_OR_OVERSIZED_RESPONSE")
                val text=ProviderWire.text(config.type,json)
                if(text.isBlank()) AiResult.Failure("EMPTY_TEXT") else AiResult.Text(text)
            }
        } catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(_: Exception) { AiResult.Failure("NETWORK_OR_FORMAT_ERROR") }
    }
    suspend fun listModels(): ModelListResult = withContext(Dispatchers.IO) {
        if(!ProviderCatalog.validBaseUrl(config.baseUrl) || secret.isBlank()) return@withContext ModelListResult.Failure("HTTPS_ENDPOINT_AND_KEY_REQUIRED")
        try {
            client.newCall(ProviderWire.models(config.type,config.baseUrl,secret)).execute().use { response ->
                if(!response.isSuccessful) return@withContext ModelListResult.Failure("HTTP_${response.code}")
                val json=read(response) ?: return@withContext ModelListResult.Failure("EMPTY_OR_OVERSIZED_RESPONSE")
                ModelListResult.Models(ProviderWire.modelIds(config.type,json),json.optString("next_page_token").isNotBlank() || json.optString("nextPageToken").isNotBlank() || json.optBoolean("has_more",false))
            }
        } catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(_: Exception) { ModelListResult.Failure("NETWORK_OR_MODEL_FORMAT_ERROR") }
    }
}
