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

enum class ProviderType { OPENAI, ANTHROPIC, GEMINI, OPENROUTER, GROQ, CUSTOM }
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
class HttpProvider(private val config: ProviderConfig, private val secret: String) : AiProviderAdapter {
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    override suspend fun text(prompt: String): AiResult = withContext(Dispatchers.IO) {
        if (!secureUrl(config.baseUrl) || config.model.isBlank() || secret.isBlank()) return@withContext AiResult.Failure("INVALID_CONFIG")
        val base = config.baseUrl.trimEnd('/')
        val body = JSONObject()
        val builder = Request.Builder()
        when(config.type) {
            ProviderType.ANTHROPIC -> {
                builder.url("$base/messages").header("x-api-key", secret).header("anthropic-version", "2023-06-01")
                body.put("model",config.model).put("max_tokens",512).put("messages",JSONArray().put(JSONObject().put("role","user").put("content",prompt)))
            }
            ProviderType.GEMINI -> {
                val model = java.net.URLEncoder.encode(config.model,"UTF-8")
                builder.url("$base/models/$model:generateContent").header("x-goog-api-key",secret)
                body.put("contents",JSONArray().put(JSONObject().put("parts",JSONArray().put(JSONObject().put("text",prompt)))))
            }
            else -> {
                builder.url("$base/chat/completions").header("Authorization","Bearer $secret")
                body.put("model",config.model).put("max_tokens",512).put("messages",JSONArray().put(JSONObject().put("role","user").put("content",prompt)))
            }
        }
        try {
            client.newCall(builder.post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext AiResult.Failure("HTTP_${response.code}")
                val source = response.body?.source() ?: return@withContext AiResult.Failure("EMPTY_RESPONSE")
                if (source.request(1_048_577)) return@withContext AiResult.Failure("RESPONSE_TOO_LARGE")
                val json = JSONObject(source.readUtf8())
                val text = when(config.type) {
                    ProviderType.ANTHROPIC -> json.getJSONArray("content").getJSONObject(0).getString("text")
                    ProviderType.GEMINI -> json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    else -> json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                }
                AiResult.Text(text)
            }
        } catch (_: Exception) { AiResult.Failure("NETWORK_OR_FORMAT_ERROR") }
    }
}
