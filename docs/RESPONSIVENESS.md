# Responsiveness fixes — 0.6.2

The quick panel and mapping reviews now use non-modal touch windows, so touches outside a panel are no longer swallowed. An outside tap closes the panel; dismissing a workflow click confirmation cancels its pending permit. Point-teaching mode still intentionally captures one selected touch and expires after 30 seconds.

The robot is 28dp by default (24–56dp in Settings), with a minimum 48dp touch window. The new size preference applies the compact default on upgrades; the robot design is unchanged. Bitmap decoding is sampled to the displayed size and runs off Main. Reset to 28dp is available in Settings.

Accessibility calls run on a serialized IO dispatcher, including mapping, reviewed field input, capture checks and the Agent Lab. Scans check cancellation and a two-second budget between remote calls; one Android Binder call itself cannot be interrupted by this budget. Mapping retries run at most once per second after each completed scan. Existing account/domain/password checks and single-use click permits remain in effect.

Provider secrets use suspend APIs with IO dispatch and a shared mutex for Keystore creation/encryption/decryption and durable preference writes/deletes. No API key enters logs or the repository.

Browser readiness now has a separate 60-second deadline. Failed page loads stop immediately. The workflow Monitor shows waiting instructions for login, foreground, loading and unlock; long video generation can still use the existing ten-minute outer action limit. External effects are not retried automatically. Stop/cancellation preserves uncertain checkpoints for manual inspection.

Persisted libraries and workflow steps compose eight entries per page (previous/next controls); mapping detection shows twelve buttons per page. Prompt parsing/filtering runs on Default. Dashboard counters query SQL counts rather than whole workflow/account payloads; chat history and execution/diagnostic feeds query bounded recent results. Library data remains local and no saved entry is deleted by pagination.

## Verification

Regression coverage includes outside-touch flags with keyboard focus preserved, compact visual/touch sizing, a 1,000-entry library with pagination, off-caller-thread Accessibility traversal, scan cancellation/deadlines, failed-page/login timeout/cancellation, and the existing mapping authorization and workflow uncertain-checkpoint suites. The build also compiles instrumentation tests and runs Android lint. JVM/UI fixtures are not proof of Samsung One UI or live ChatGPT/Flow compatibility.

## Device smoke test

1. Install the APK, grant overlay access, and enable the bubble. Check its compact size; drag, dock, tap and long-press Stop.
2. Open the quick panel above another app. Tap outside: the panel should close and the underlying app should remain usable. Keyboard input inside the panel must still work.
3. Scan a complex page; immediately Stop. No result panel or click should appear after Stop. Repeat with a locked HP, changed target and sensitive field: the operation must stop safely.
4. Test a failed browser page and unfinished login. Confirm actionable status in Monitor and a bounded wait. Stop during a wait. Do not retry a paid generation without inspecting the result.
5. Open a library with more than eight entries; navigate both ways and delete the last item on a page. Confirm no missing saved entries or empty inaccessible page.
6. Save/test/delete a provider on a QA device and verify chat/capture/workflow behavior with a test account.

## Follow-up after device stutter report

- The installable artifact is now the non-debuggable optimized variant with R8/resource shrinking, rather than a development APK. It uses test/debug signing for private installation; official distribution needs an owner-managed signing key.
- Crossing raw-screen touch slop cancels GestureDetector long-press detection, because local coordinates move with the bubble window. A long drag cannot accidentally trigger emergency Stop; a stationary hold still stops it.
- Bubble motion is coalesced with `postOnAnimation`: even 500 input updates before a frame produce one window update using the latest position. No idle frame loop runs. Drag uses Android touch slop, avoids repeated display-metric queries, and docks without overshoot.
- Opening the quick panel performs no Accessibility or package-manager lookup. The reviewed-input action resolves its target only after the user explicitly chooses it. Its entire job, including the delayed action, is cancelled by global Stop.
- Robot images in the app and overlay share a bounded 2MB cache and are sampled/decoded off Main. Settings changes unrelated to size do not reload the icon.
- Dashboard cards now use a LazyColumn with stable row keys, so offscreen tools are not composed eagerly. Workflow app labels and selected-file metadata load on IO instead of invoking PackageManager or ContentResolver during composition.
- Chat uses a bounded LazyColumn; typing invalidates the composer rather than measuring all replies. JSON serialization/deserialization runs on Default. History SQL joins only short titles and loads eight rows per page; selecting a thread fetches its complete body. Old threads without titles remain accessible.
- UI database subscriptions are lifecycle-aware. Explicit browser Pause does not consume the 60-second readiness budget.
- Settings includes version/build identity and optional copyable local frame metrics. Metrics contain device/build information and aggregate timings, never screen text, credentials or message contents. The frame callback only updates an in-memory histogram on a background handler.
- Emulator instrumentation measures warmed scrolling, checks for frozen frames, exercises repeated bubble drags, opens its panel without Accessibility enabled, and verifies that outside touches reach Chat. API35 and API36 runs, screenshots and measurements are retained as Actions artifacts. Emulator frame timings are not measurements of the user's Samsung hardware.
