package com.byga.smartoverlay.ai

import com.byga.smartoverlay.workflow.secureUrl
import java.net.URI

data class ProviderPreset(val label: String, val baseUrl: String, val models: List<String>)
object ProviderCatalog {
    // Suggestions, not a promise of account access. Live discovery and manual IDs remain available.
    val presets = mapOf(
        ProviderType.OPENAI to ProviderPreset("OpenAI", "https://api.openai.com/v1", listOf("gpt-4.1-mini", "gpt-4.1", "gpt-4o-mini")),
        ProviderType.ANTHROPIC to ProviderPreset("Anthropic", "https://api.anthropic.com/v1", listOf("claude-sonnet-4-5", "claude-haiku-4-5")),
        ProviderType.GEMINI to ProviderPreset("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", listOf("gemini-2.5-flash", "gemini-2.5-pro")),
        ProviderType.OPENROUTER to ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", listOf("openai/gpt-4.1-mini", "anthropic/claude-sonnet-4.5")),
        ProviderType.GROQ to ProviderPreset("Groq", "https://api.groq.com/openai/v1", listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant")),
        ProviderType.COHERE to ProviderPreset("Cohere", "https://api.cohere.com", listOf("command-a-plus-05-2026", "command-a-03-2025", "command-r7b-12-2024")),
        ProviderType.HUGGING_FACE to ProviderPreset("Hugging Face", "https://router.huggingface.co/v1", listOf("meta-llama/Llama-3.1-8B-Instruct", "Qwen/Qwen3-8B", "meta-llama/Llama-3.3-70B-Instruct")),
        ProviderType.MISTRAL to ProviderPreset("Mistral", "https://api.mistral.ai/v1", listOf("mistral-small-latest", "mistral-medium-latest", "mistral-large-latest")),
        ProviderType.NVIDIA_NIM to ProviderPreset("NVIDIA NIM", "https://integrate.api.nvidia.com/v1", listOf("nvidia/nemotron-3-super-120b-a12b", "nvidia/llama-3.1-nemotron-70b-instruct", "mistralai/mistral-large")),
        ProviderType.CUSTOM to ProviderPreset("Custom HTTPS", "", emptyList())
    )
    fun get(type: ProviderType) = presets.getValue(type)
    fun validBaseUrl(url: String) = secureUrl(url) && runCatching { val uri=URI(url); uri.rawQuery==null && uri.rawFragment==null }.getOrDefault(false)
}
