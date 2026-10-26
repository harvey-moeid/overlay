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

# Kotlin facade entry points used directly by the on-device coroutine tests.
-keep class kotlin.ResultKt { *; }
-keep class kotlin.coroutines.intrinsics.IntrinsicsKt { *; }
-keep class kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsKt { *; }
-keep class kotlin.coroutines.jvm.internal.Boxing { *; }
-keep class kotlin.collections.IntIterator { *; }
-keep class kotlin.io.CloseableKt { *; }
-keep class kotlin.io.FilesKt { *; }
-keepclassmembers class kotlin.io.FilesKt** { public static *** writeText*(...); }
-keep class kotlinx.coroutines.BuildersKt { *; }
-keep class kotlinx.coroutines.AwaitKt { *; }
-keep class kotlinx.coroutines.flow.FlowKt { *; }
