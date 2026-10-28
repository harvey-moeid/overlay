package com.byga.smartoverlay.mapping

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** User chosen native apps only. Browser grants never bypass origin verification. */
object MappingAccess {
    private const val PREFS = "mapping_app_grants"
    fun load(context: Context) {
        MappingPolicy.userPackages = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("packages", emptySet()).orEmpty().filter(::eligible).toSet()
    }
    fun eligible(pkg: String) = pkg.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+")) &&
        !Regex("(?i)browser|chrome|firefox|edge|opera|settings|systemui|permissioncontroller|packageinstaller|keyguard|bank|wallet|authenticator").containsMatchIn(pkg)
    suspend fun grant(context: Context, pkg: String) = withContext(Dispatchers.IO) {
        require(eligible(pkg)) { "Aplikasi sistem, browser lain, dan aplikasi sensitif tidak didukung" }
        val next = MappingPolicy.userPackages + pkg
        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("packages", next).commit())
        MappingPolicy.userPackages = next
    }
    suspend fun revoke(context: Context, pkg: String) = withContext(Dispatchers.IO) {
        val next = MappingPolicy.userPackages - pkg
        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("packages", next).commit())
        MappingPolicy.userPackages = next
    }
}

/** Metadata only; editable contents are never used as an identity. Best effort redaction. */
object MappingPrivacy {
    private val sensitive = Regex("(?i)password|kata sandi|one.time|otp|verification|verifikasi|2fa|session.?token|api.?key")
    fun sensitiveLabel(value: String) = sensitive.containsMatchIn(value)
    fun label(value: String): String = value.trim().takeIf {
        it.length in 1..100 && !sensitiveLabel(it) &&
            !Regex("[\\w.+-]+@[\\w.-]+|\\d{4,}|(?:sk-|Bearer )[\\w-]+", RegexOption.IGNORE_CASE).containsMatchIn(it)
    }.orEmpty()
}
