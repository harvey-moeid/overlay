# Manifest components are also referenced by on-device UI tests and explicit Android intents.
-keepnames class com.byga.smartoverlay.MainActivity
-keepnames class com.byga.smartoverlay.overlay.OverlayService
-keepnames class com.byga.smartoverlay.accessibility.SafeAccessibilityService

# Public boundaries called from the separately compiled instrumentation APK.
# R8 cannot see these external callers while optimizing the application.
-keep class androidx.tracing.Trace { *; }
-keep class androidx.room.Room { *; }
-keep class androidx.room.Room$Companion { *; }
-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.RoomDatabase$Builder { *; }
-keep class com.byga.smartoverlay.data.BygaDatabase { *; }
-keep class com.byga.smartoverlay.data.Records { *; }
-keep class com.byga.smartoverlay.data.Record { *; }
-keep class com.byga.smartoverlay.data.SecretStore { *; }
-keep class com.byga.smartoverlay.agent.AgentDataKt { *; }
-keep class com.byga.smartoverlay.agent.AgentRecords { *; }
-keep class com.byga.smartoverlay.agent.MemoryEntry { *; }
-keep class com.byga.smartoverlay.agent.PlanCodec { *; }
-keep class com.byga.smartoverlay.ai.BudgetLedger { *; }

# Instrumentation is a separate APK and consumes the public Kotlin runtime ABI.
# Retain that shared ABI while still allowing R8 to optimize method bodies/call sites.
-keep,allowoptimization class kotlin.** { public *; protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public *; protected *; }
-keep,allowoptimization class androidx.concurrent.futures.CallbackToFutureAdapter** { public *; protected *; }
