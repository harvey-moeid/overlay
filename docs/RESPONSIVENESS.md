# Responsiveness fixes — 0.6.1

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
