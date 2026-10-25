package com.byga.smartoverlay

import com.byga.smartoverlay.ai.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProviderTest {
    private fun config(type: ProviderType, model: String="model/id") = ProviderConfig("fake",type,ProviderCatalog.get(type).baseUrl,model)
    private fun json(request: Request): JSONObject { val buffer=Buffer(); request.body!!.writeTo(buffer); return JSONObject(buffer.readUtf8()) }
    @Test fun everyProviderHasCatalogEntry() { assertEquals(ProviderType.entries.toSet(),ProviderCatalog.presets.keys) }
    @Test fun builtInEndpointsAreHttpsAndPresetsExist() { ProviderType.entries.filter {it!=ProviderType.CUSTOM}.forEach { assertTrue(ProviderCatalog.validBaseUrl(ProviderCatalog.get(it).baseUrl)); assertTrue(ProviderCatalog.get(it).models.isNotEmpty()) } }
    @Test fun secretsMustNotBeEmbeddedInBaseUrl() { listOf("http://example.com","https://user:key@example.com","https://example.com?api_key=x","https://example.com#key").forEach { assertFalse(ProviderCatalog.validBaseUrl(it)) } }
    @Test fun cohereUsesNativeV2ChatAndBearer() {
        val request=ProviderWire.chat(config(ProviderType.COHERE),"fake-secret","halo")
        assertEquals("/v2/chat",request.url.encodedPath); assertEquals("Bearer fake-secret",request.header("Authorization")); assertNull(request.url.query)
        assertEquals("halo",json(request).getJSONArray("messages").getJSONObject(0).getString("content"))
    }
    @Test fun cohereModelsUseV1ChatFilter() { val r=ProviderWire.models(ProviderType.COHERE,ProviderCatalog.get(ProviderType.COHERE).baseUrl,"fake"); assertEquals("/v1/models",r.url.encodedPath); assertEquals("chat",r.url.queryParameter("endpoint")) }
    @Test fun openAiCompatibleNewProvidersPreserveModelId() {
        listOf(ProviderType.HUGGING_FACE,ProviderType.MISTRAL,ProviderType.NVIDIA_NIM).forEach { type ->
            val r=ProviderWire.chat(config(type,"org/model:provider"),"fake","halo")
            assertEquals("/v1/chat/completions",r.url.encodedPath); assertEquals("org/model:provider",json(r).getString("model")); assertEquals("Bearer fake",r.header("Authorization"))
        }
    }
    @Test fun anthropicAuthAndPathArePreserved() { val r=ProviderWire.chat(config(ProviderType.ANTHROPIC),"fake","x"); assertEquals("/v1/messages",r.url.encodedPath); assertEquals("fake",r.header("x-api-key")); assertNull(r.header("Authorization")) }
    @Test fun geminiKeyNeverInQueryString() { val r=ProviderWire.chat(config(ProviderType.GEMINI,"gemini-2.5-flash"),"fake","x"); assertEquals("fake",r.header("x-goog-api-key")); assertNull(r.url.query); assertTrue(r.url.encodedPath.endsWith(":generateContent")) }
    @Test fun cohereResponseConcatenatesTextBlocks() { assertEquals("Satu\nDua",ProviderWire.text(ProviderType.COHERE,JSONObject("""{"message":{"content":[{"type":"text","text":"Satu"},{"type":"text","text":"Dua"}]}}"""))) }
    @Test fun cohereModelNamesAreDistinctSorted() { assertEquals(listOf("a","z"),ProviderWire.modelIds(ProviderType.COHERE,JSONObject("""{"models":[{"name":"z"},{"name":"a"},{"name":"a"},{"name":""}]}"""))) }
    @Test fun compatibleModelIdsKeepNamespaces() { assertEquals(listOf("org/model:route"),ProviderWire.modelIds(ProviderType.HUGGING_FACE,JSONObject("""{"data":[{"id":"org/model:route"}]}"""))) }
    @Test fun geminiFiltersNonChatModelsAndRemovesPrefix() { assertEquals(listOf("good"),ProviderWire.modelIds(ProviderType.GEMINI,JSONObject("""{"models":[{"name":"models/good","supportedGenerationMethods":["generateContent"]},{"name":"models/embed","supportedGenerationMethods":["embedContent"]}]}"""))) }
    private fun client(payload: String, code: Int=200) = OkHttpClient.Builder().addInterceptor { chain -> Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("mock").body(payload.toResponseBody("application/json".toMediaType())).build() }.build()
    @Test fun realAdapterParsesCohereMock() = runTest { val r=HttpProvider(config(ProviderType.COHERE),"fake",client("""{"message":{"content":[{"type":"text","text":"OK"}]}}""")).text("test"); assertEquals(AiResult.Text("OK"),r) }
    @Test fun newCompatibleAdaptersParseMocks() = runTest { for(t in listOf(ProviderType.HUGGING_FACE,ProviderType.MISTRAL,ProviderType.NVIDIA_NIM)) assertEquals(AiResult.Text("OK"),HttpProvider(config(t),"fake",client("""{"choices":[{"message":{"content":"OK"}}]}""")).text("test")) }
    @Test fun unauthorizedResponseIsRedacted() = runTest { assertEquals(AiResult.Failure("HTTP_401"),HttpProvider(config(ProviderType.COHERE),"fake",client("secret server detail",401)).text("x")) }
    @Test fun emptyTextNeverClaimsSuccess() = runTest { assertEquals(AiResult.Failure("EMPTY_TEXT"),HttpProvider(config(ProviderType.COHERE),"fake",client("""{"message":{"content":[]}}""")).text("x")) }
    @Test fun emptyModelListIsAnEmptyActualResult() = runTest { val r=HttpProvider(config(ProviderType.NVIDIA_NIM),"fake",client("""{"data":[]}""")).listModels(); assertEquals(ModelListResult.Models(emptyList()),r) }
    @Test fun partialModelListIsLabelled() = runTest { val r=HttpProvider(config(ProviderType.COHERE),"fake",client("""{"models":[{"name":"chat"}],"next_page_token":"next"}""")).listModels(); assertEquals(ModelListResult.Models(listOf("chat"),true),r) }
    @Test fun malformedModelResponseFailsHonestly() = runTest { assertEquals(ModelListResult.Failure("NETWORK_OR_MODEL_FORMAT_ERROR"),HttpProvider(config(ProviderType.MISTRAL),"fake",client("{}" )).listModels()) }
    @Test fun missingKeyDoesNotReachTransport() = runTest { val c=OkHttpClient.Builder().addInterceptor { error("Must never send") }.build(); assertEquals(AiResult.Failure("INVALID_CONFIG"),HttpProvider(config(ProviderType.MISTRAL),"",c).text("x")) }
    @Test fun transportsDoNotFollowRedirectsOrBlindRetry() { val c=HttpProvider.defaultClient(); assertFalse(c.followRedirects); assertFalse(c.followSslRedirects); assertFalse(c.retryOnConnectionFailure) }
    @Test fun hugeResponsesAreRejected() = runTest { assertEquals(AiResult.Failure("EMPTY_OR_OVERSIZED_RESPONSE"),HttpProvider(config(ProviderType.COHERE),"fake",client("x".repeat(1_048_577))).text("x")) }
}
