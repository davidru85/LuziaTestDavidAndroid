# File: MEMORY.md

## Purpose
A durable record of architectural and design decisions made during development. Each phase appends its own section. Read this before working on a phase to understand the "why" behind non-obvious choices; update it whenever a fork is settled or a prior decision is revised.

This file complements — but does not replace — `CONTEXT.md`, `TECHNICAL_SPEC.md`, `DESIGN_SYSTEM.md`, and `ROADMAP.md`. Those describe *what* the system is; this file records *why it ended up that way*.

---

## Phase 5.5 — Decisions Locked (2026-04-17)

### Fork 1 — Role Selector / Personas: **(a) Full Implementation**

Three personas, declared in `res/values/strings.xml` as parallel string arrays (`role_names` + `role_prompts`):

| Index | Name | Prompt |
| :--- | :--- | :--- |
| 0 | **Student** | *You are a patient, educational tutor. Explain concepts step by step and encourage learning.* |
| 1 | **Scientist** | *You are a rigorous scientist. Provide evidence-based, analytical, and precise answers.* |
| 2 | **Artist** | *You are a creative artist. Think imaginatively, brainstorm ideas, and inspire creativity.* |

**Default persona on app launch:** Student (index 0).

**Per-message persona capture — interpretation (B), confirmed:**
*   The `system` role is **removed** from the domain model. No automatic `system` entry is ever injected at index 0.
*   **User messages** carry the persona prompt active at send time as their serialized `role` field.
*   **Assistant messages** continue to serialize as `"role": "assistant"`.
*   Changing the persona mid-conversation affects only subsequent user messages; historical user messages retain the persona active when they were sent.

Interpretation (A) — "every message including assistants carries the persona" — was rejected as architecturally unconventional and inconsistent with OpenAI-style `role` semantics.

Impacted specs updated: `DESIGN_SYSTEM.md §2`, `TECHNICAL_SPEC.md §API Contracts #2`.

### Fork 2 — Tier-2 Retry: **(a) Full Implementation**

FAILED assistant bubbles gain a retry affordance wired end-to-end.

**Scope:**
*   `ChatRepository.deleteMessage(id: String)` — new DAO + impl.
*   `ChatViewModel.onRetryLastFailure()` — deletes the failed assistant row, then re-invokes `StreamAssistantReplyUseCase` with the cleaned history.
*   `AssistantMessageBubble` gains an `onRetry: (() -> Unit)?` parameter; the button renders only when `streamState == FAILED`.

**Rationale:** A failed AI reply is currently a dead-end in the UX. Adding retry now is cheap (~30 lines + tests) and the VM is fresh in mind.

### Fork 3 — Permission Handling: **(a) Merge Phase 6.2 into 5.5**

`RECORD_AUDIO` permission flow lives inside `ChatScreen` (sub-task 5.5.J) via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission)` plus a rationale dialog. Phase 6.2 is marked absorbed.

**Rationale:** Permission handling is a screen concern; splitting it into a separate roadmap item was roadmap artifact, not an engineering boundary.

**Note:** Sub-task 5.5.K (wire `ChatScreen` into `MainActivity`) is literally what Phase 6.1 describes, so **6.1 is also absorbed** into 5.5. Phase 6 has no remaining work.

### Confirm-or-diverge defaults (locked)

*   **Auto-scroll:** LazyColumn auto-scrolls only when the user is already near the bottom. Implemented with `derivedStateOf` on `LazyListState.firstVisibleItemIndex`. Matches `DESIGN_SYSTEM.md §Auto-Scroll Logic` literally.
*   **DeleteSweep confirmation:** `AlertDialog` before clearing the conversation (DESIGN_SYSTEM literal).
*   **Tier-1 Snackbar:** `ChatScreen` subscribes to `vm.events: SharedFlow<ChatEvent>` via `LaunchedEffect` + `SnackbarHostState`.
*   **Tier-3 AlertDialog:** **Deferred to Phase 7.** All errors surface as Tier-1 Snackbars for now. Tier classification inside `ChatEvent` is Phase 7 scope. → **Shipped in Phase 7.0** (2026-04-18) via `ChatEvent.Tier1`/`Tier3` + `AppError.toChatEvent()`.

---

## Phase 7.1 — Decisions Locked (2026-04-18)

### Fork 4 — Revised `/chat` wire contract (supersedes Fork 1 for serialisation)

Manual E2E testing (Phase 7.1.2) against the backend returned `422 VALIDATION_ERROR` on every `/chat` request. The Fork 1 design — stuffing the full persona prompt into the wire `role` field — is incompatible with the backend's standard LLM role validation (the backend enforces `role ∈ {"user", "assistant", "system"}`).

**Resolution (confirmed 2026-04-18):** the wire format for `POST /chat` now carries **three fields per message**:

| Field | Type | Notes |
| :--- | :--- | :--- |
| `role` | string enum | `"user"` / `"assistant"` / `"system"`. Standard LLM convention. |
| `role_prompt` | string | **Required on `"user"` messages; omitted on `"assistant"` and `"system"`.** Carries the persona prompt active when the user sent the message. |
| `content` | string | Message text. Unchanged. |

**Per-message persona capture semantics are preserved** — the persona active at send-time sticks to the user message for the lifetime of the conversation — but it now lives in its own dedicated field (`role_prompt`) instead of overloading `role`.

**What survives from Fork 1 (still in force):**
*   No standalone `system` entry is injected by the client at index 0 of the history. The `"system"` enum value is defined for forward compatibility only; the current client flow never emits it.
*   Assistant messages serialize as `role = "assistant"` without a `role_prompt`.
*   Changing the persona mid-conversation affects only subsequent user messages; historical user messages retain the `role_prompt` active when they were sent.

**What changes from Fork 1:**
*   Wire-level `role` is no longer the persona prompt. It is the standard LLM enum.
*   New `role_prompt` field carries the persona prompt on user messages.

**Domain and persistence layers are unchanged.** `ChatMessage.personaPrompt: String?` (domain) and `ChatMessageEntity.personaPrompt: String?` (Room) already exist (added in 5.5.B); no Room schema bump is required.

**Impacted specs updated:**
*   `TECHNICAL_SPEC.md §API Contracts #2` — rewritten to the three-field shape.
*   `DESIGN_SYSTEM.md §2` — untouched; it only describes UX (per-message persona capture), not the wire shape.

**Execution note — backend coordination:** Phase 7.1.3 TDD work is **blocked on the backend developer confirming deployment of the matching three-field contract**. Implementation (DTO + `ChatMapper`) begins only after the backend is live with the new schema.

**Deferred (Phase 7.1.4):** empty-`content` assistant rows observed lingering in Room after failed streams — a separate, client-only defect unrelated to the wire-format change.

### Fork 5 — `/transcribe` contract revision for empty / too-short audio (2026-04-18, API_SPEC v1.2.0)

Manual Phase 7.2 edge-case testing against the backend revealed — and the backend team then fixed — a contract defect on `/transcribe`:

**Before (v1.1.0):** empty or too-short audio surfaced as `502 UPSTREAM_ERROR` ("Transcription service failed. Please try again."). Misleading because `502` implies a Whisper outage, encouraging the client to retry — but the actual cause is user-actionable (bad input).

**After (v1.2.0, deployed 2026-04-18):** empty or too-short audio now returns:

```
HTTP 400 Bad Request
{ "error": { "code": "BAD_REQUEST", "message": "Audio file is empty or too short to transcribe." } }
```

One byte-level verbatim string covers both sub-cases (zero-byte local guard + Whisper 4xx). `502 UPSTREAM_ERROR` is now reserved for genuine Whisper-side 5xx / network / timeout.

**Client-side implications:**

1.  `AppError.fromCode("BAD_REQUEST", "Audio file is empty or too short…")` **currently discards the backend message** — the `BadRequest` variant is a `data object` with a fixed generic message ("The request was invalid."). The user sees the generic string, not the actionable "empty or too short" guidance. Fix to preserve backend-supplied messages verbatim lands in **Phase 7.2** (scoped below).
2.  Do **not** auto-retry `/transcribe` 4xx responses. The app never did, but worth pinning explicitly — a future Phase 7.2 retry-on-error helper must skip 400/413/415.
3.  The `/transcribe` response on Whisper-hallucinated `"you"` for short audio (non-error, `200 OK`) is indistinguishable from a valid transcription on the client side. Surface as-is; no special handling required.

**Backend-side open questions (deferred, not in Phase 7.2 scope):**

*   Whether the Android client should add a pre-upload duration guard (< 0.3s reject locally before upload) is a design call, not a contract issue. Trade-off: saves a round-trip but duplicates backend validation. Current position: skip — backend is authoritative, Golden Rule #2 favours capture-and-transmit simplicity.
*   Whether to request a structured `error.details.reason` from the backend for analytics (distinguish zero-byte from too-short). Not needed for Phase 7.2 UX.

**Impacted specs:** `TECHNICAL_SPEC.md §API Contracts #1` — rewritten with the new error table.

---

#### Fork 4 addendum — backend decisions locked in (2026-04-18)

Backend team responded with their API_SPEC.md v1.1.0. Points that bind the Android client:

1.  **`role_prompt` on non-user turns is accept-and-ignore**, not strict reject. Android still omits it (minimal payload), but accidental inclusion is safe — no 422.
2.  **Empty `content` rules are role-dependent:**
    *   `"user"` / `"system"` empty → **422**.
    *   `"assistant"` empty → **accepted on the wire; silently dropped before forwarding to OpenAI.** This means the transient empty-assistant-placeholder pattern the app produces during an in-flight SSE stream is wire-safe. 7.1.4 cleanup remains a pure client-hygiene task, **not a blocker** for 7.1.3.
3.  **Missing / empty `role_prompt` on any user turn → strict 422.** Enforced on the *latest* user turn **and every historical user turn** in the payload. Implication: the Android `ChatMapper` must guarantee a non-empty `role_prompt` for every user message serialized, including older rows in history. Current domain model populates `ChatMessage.personaPrompt` at send time, so new rows are safe; if any pre-existing row could have a null `personaPrompt`, it must be filtered or filled client-side before POST.
4.  **Android never emits `"system"`.** The backend is the sole producer of `system` turns and injects exactly one, prepended to the OpenAI payload from the latest user turn's `role_prompt`. The `"system"` enum value can be **omitted from the Android DTO / domain enum entirely** if it simplifies the model. The spec retains it only for forward-compat.
5.  **Backend persona injection logic** (informational, affects client mental model):
    *   Only the **latest** `"user"` turn's `role_prompt` steers the next reply.
    *   Historical `role_prompt`s are audit metadata — required to be valid, but do not re-enter OpenAI.
    *   Server-side memory: none; interleaved `system` turns: none. One system turn per request, derived from the latest user turn only.
6.  **Specific 422 `message` strings are byte-level pinned** (see `TECHNICAL_SPEC.md §API Contracts #2 — Validation errors`). The client's `ErrorMapper` / `AppError` stack must recognise `code: "VALIDATION_ERROR"` as a Tier-1 event (Snackbar). Currently `AppError.fromCode(...)` only knows `BAD_REQUEST` and `FILE_TOO_LARGE`; `VALIDATION_ERROR` falls through to `Unknown` → Tier-3 `AlertDialog`, which is the **wrong tier**. Adding `VALIDATION_ERROR` as a first-class `AppError` variant (Tier-1) is part of Phase 7.1.3 scope.
7.  **No backwards compatibility.** Atomic cutover; the backend will not accept two-field payloads after deployment. Client and backend ship together — no mixed-shape transient state.
8.  **Single backend environment.** Android `staging` and `production` build flavors point to the same backend URL. Flavor orthogonality is a client concern only.

---

## Phase 5.5 — Sub-task Plan

Each sub-task follows the TDD cycle (🔴 RED → 🟢 GREEN → 🔵 REFACTOR) and is executed one at a time with user acknowledgement at each phase transition.

| # | Sub-task | Scope |
| :--- | :--- | :--- |
| **5.5.A** | Domain refactor | Drop `MessageRole.SYSTEM`; add `personaPrompt: String?` to `ChatMessage`; cascade through mapper / use-case / purity tests. |
| **5.5.B** | Data refactor | `ChatMessageEntity` + mapper; destructive Room DB bump (pre-launch, no users yet); `ChatRequestDto` + `ChatMapper` emit new per-message `role`. |
| **5.5.C** | Persona catalog | `Persona` enum in domain; `strings.xml` arrays; `PersonaCatalog` interface in domain + Android-strings-backed impl in data. |
| **5.5.D** | `ChatRepository.deleteMessage(id)` | Domain + DAO + impl + tests. Prerequisite for retry. |
| **5.5.E** | VM extensions | `selectedPersona: StateFlow<Persona>`, `onPersonaSelected()`, `onRetryLastFailure()`; `onSendTap` attaches active `personaPrompt` when building user msg. |
| **5.5.F** | `RoleSelectorChips` component | Compose leaf: `FilterChip` strip, SingleSelect. |
| **5.5.G** | `AssistantMessageBubble` retry button | Add `onRetry: (() -> Unit)?`; render only in FAILED. |
| **5.5.H** | `ChatInputBar` composite | `OutlinedTextField` + `MorphingActionButton` + `StreamingIndicator` placement. |
| **5.5.I** | `ChatTopAppBar` | Title + `Icons.Outlined.DeleteSweep` + confirm `AlertDialog`; disabled when history is empty. |
| **5.5.J** | `ChatScreen` | `Scaffold` integrating everything; LazyColumn auto-scroll; `SnackbarHostState` subscribed to `vm.events`; `RECORD_AUDIO` launcher + rationale (absorbs 6.2). |
| **5.5.K** | Wire into `MainActivity` | Replace `Greeting` with `ChatScreen`; device smoke test on LG G8 (absorbs 6.1). |

---

## Cross-cutting invariants (still enforced)

1. **Golden Rules** (`CONTEXT.md`): Zero Local ASR/TTS · Zero Local Audio Processing · Domain Purity · No Data Leakage · Strict TDD · Text-Only UI.
2. `DomainPurityTest` and `PresentationPurityTest` must stay green through every 5.5 sub-task.
3. **Room migration strategy:** destructive (pre-launch, no users). Version bump is acceptable in 5.5.B.
4. **Accessibility content descriptions:** English hardcoded strings for now (e.g., "Record voice message", "Sending message"). Migration to `stringResource()` is a Phase 7 polish item. → **Shipped in 7.3.1.A** (2026-04-18); default English pinned + Spanish/Portuguese translations scheduled for 7.3.3.F.

---

## Phase 7.3.3 — UX Decisions Locked (2026-04-18)

User-surfaced UX suggestions + design decisions for Phase 7.3.3 (UI/UX Improvements).

### Sub-task scope

*Ordering:* A → B → C → D → E → F (quick wins first, i18n last so it translates the final stabilised copy).

**7.3.3.A — Record → Stop icon swap while recording.** Today `Icons.Filled.Mic` is shown in both idle and recording states; only `contentDescription` toggles. Change to `Icons.Filled.Stop` (or equivalent) when `isRecording`. Purely visual — existing `MorphingActionButtonTest` contentDescription checks cover the semantic surface.

**7.3.3.B — FAILED assistant bubble copy, split by position:**
*   *Latest* FAILED assistant (retryable via `onRetryLastFailure`): **"I've gone blank. Mind retrying?"** + retry button.
*   *Older* FAILED assistants (non-retryable — only the last failure reaches `onRetryLastFailure`): **"Sorry, empty message"** + no retry button.
*   Disambiguation already wired via the existing `onRetry: (() -> Unit)?` parameter (non-null on the latest, null on older). No domain-model change needed.
*   Copy goes into `strings.xml` → translated in 7.3.3.F.

**7.3.3.C — Tier-3 AlertDialog — M3 Expressive redesign + Option-B copy strategy.**
*   Current shape: backend message displayed verbatim in the AlertDialog body, title hardcoded (`"Service error"` / `"Unexpected error"`).
*   Target shape: hand-written friendly copy keyed by `AppError` variant (`Internal`, `ServiceUnavailable`, `Unknown` all go through Tier-3). Backend message preserved in a collapsible "Details" section for debugging.
*   Re-plumbing: `ChatEvent.Tier3.title: String` → `Tier3.kind: Tier3Kind` enum. Composable resolves icon + `stringResource` title + friendly body + backend-details. Unblocks 7.3.1's deferred title-i18n.
*   M3 Expressive visuals: icon slot, accent colour per severity, cleaner typographic hierarchy.

**7.3.3.D — Mic permission rationale + Clear conversation confirm dialogs — M3 Expressive polish.** Icon slots, minor copy touch-ups, likely share the reusable `LuziaAlertDialog` wrapper introduced in 7.3.3.C.

**7.3.3.E — Role selector — icons per persona + motion on selection.**
*   Icons per persona (to be chosen during implementation — candidates: Student → graduation cap / book, Scientist → science / atom, Artist → palette).
*   Motion: animated scale or ripple on the chip being selected.
*   M3 `FilterChip` strip retained — single-select + `Role.RadioButton` semantics from 5.5.F unchanged. Visual layer only.

**7.3.3.F — i18n: neutral Spanish (`values-es`) + neutral Portuguese (`values-pt`).**
*   Scope: every string in `res/values/strings.xml` including `role_names`, `role_prompts`, the 20+ entries added in 7.3.1.A, and the 7.3.3.B / 7.3.3.C copy.
*   Tests: one pinning file per locale (`A11yStringMigrationEsTest`, `A11yStringMigrationPtTest`) using Robolectric `@Config(qualifiers = "es")` / `@Config(qualifiers = "pt")`. Same pattern as the existing English `A11yStringMigrationTest`. ~40 new assertions.

### Risks flagged

**`role_prompts` translation risk:** `role_prompts` strings are sent to the backend as the per-user-message `role_prompt` that steers the LLM (per Fork 4). Translating them changes the language of the system prompt delivered to the upstream model. In practice most chat LLMs reply in the user's message language regardless of system-prompt language, but quality can shift subtly. **Mitigation:** if reply quality regresses for Spanish / Portuguese users after 7.3.3.F, revert the specific `role_prompts` subset back to English without touching the rest of the translation set.

---

## Phase 10 — Golden Rules Revision (2026-04-19)

### Fork 6 — Output-side TTS carve-out on Rules #1 and #6

The original Golden Rules prohibited **all** local ASR/TTS and **all** audio controls in the message list. During Phase 10 polish planning the user requested a "read aloud" affordance on assistant replies (surfaced on the most recent received message). Rather than reject the feature or bolt on a workaround, Rules #1 and #6 were narrowed to reflect the actual architectural concern (no local audio processing on the **input** path) and permit a specific, bounded output-side affordance.

**Architectural invariant preserved (unchanged):**
*   No local STT / ASR — user audio continues to be captured raw and sent to the backend for Whisper transcription.
*   No third-party TTS engines (e.g., bundled ML models, cloud TTS SDKs from other vendors).
*   No automatic or background playback — the user must initiate each read-aloud via an explicit tap.

**Newly permitted:**
*   Android system `TextToSpeech` (`android.speech.tts.TextToSpeech`) invoked on assistant replies on user demand, via a single icon-button affordance.
*   Placement of the affordance is a UX decision (ROADMAP 10.6.D currently scopes it to the last received assistant message only).

**Why the narrowing — not a full relaxation:** the original rules were load-bearing against two classes of scope creep: (a) duplicating backend Whisper responsibilities on-device (input path), and (b) turning the text-only chat into a media-app with waveforms, scrubbers, and playback controls for recorded audio (output path). Narrowing Rule #1 to the input path keeps (a) intact; narrowing Rule #6 to "no playback of *recorded* audio" keeps (b) intact while allowing synthetic speech rendered from already-rendered text.

Impacted specs: `CONTEXT.md §Golden Rules #1, #6`.
Implementation: tracked as ROADMAP 10.6.D.

---

## Phase 10.6 — Chat UX Layout Decisions (2026-04-19)

Three coupled refinements that landed across 10.6.A / 10.6.B / 10.6.C, together reshaping how the chat screen composes its chrome, message list, and input bar.

### 10.6.A — Retry affordance relocated below the bubble

*   `AssistantMessageBubble` no longer hosts the retry button. Its `onRetry: (() -> Unit)?` parameter was replaced by `isRetryable: Boolean`, used only to select the 7.3.3.B friendly-copy variant. The bubble itself stays decoupled from the retry callback.
*   New `RetryAssistantReplyButton` composable — a `TextButton` with `Refresh` icon + `"Retry reply"` label — is rendered beneath the latest FAILED assistant row by `ChatScreenContent`, inside a `Column(verticalArrangement = spacedBy(4.dp))` that stacks bubble + button.
*   **Why:** the in-bubble IconButton got visually cramped under longer-text locales (confirmed in Spanish, where `"Reintentar respuesta"` vied with the body copy for horizontal room).

### 10.6.B — Sticky top chrome + bottom-anchored message list

`ChatScreenContent` restructured into three zones: topBar (chrome stack) / LazyColumn (messages) / bottomBar (input). The final shape came after two failed device-verification attempts — the learnings are load-bearing for future Compose layout work.

**Final shape:**
*   `ChatTopAppBar` + `RoleSelectorChips` stacked inside `Scaffold.topBar`. Both stay pinned when the IME opens — previously the role selector sat in the content area and could get squeezed out.
*   LazyColumn uses natural (chronological) item order + `verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)` for short-list bottom-anchoring + `LaunchedEffect(state.messages.size) { listState.scrollToItem(lastIndex) }` to keep the newest message visible as the list grows.
*   `AndroidManifest.xml` gained `android:windowSoftInputMode="adjustResize"` on `MainActivity`.
*   `ChatInputBar` inside `Scaffold.bottomBar` applies `Modifier.imePadding()` so it anchors above the IME.

**Two prior attempts that failed device verification (recorded so the mistakes aren't repeated):**
1.  **`LazyColumn(reverseLayout = true)` + `state.messages.asReversed()` + default `Arrangement.spacedBy(6.dp)` (Alignment.Top)** — Compose docs state explicitly that `reverseLayout` does not change `verticalArrangement` alignment. The default-Top alignment on `spacedBy` overrode the bottom-anchor that `reverseLayout`'s default `Arrangement.Bottom` would have provided. Robolectric's long-list test passed because viewport composition is independent of short-list anchoring — the failure only surfaced on device with a short conversation. User flagged via [`issues/messages_at_top.png`](issues/messages_at_top.png).
2.  **Missing `android:windowSoftInputMode`** — without an explicit `adjustResize`, API 30 + `enableEdgeToEdge()` fell back to `adjustPan` on the LG LM-G900, which panned the entire window up when the IME opened and pushed the `topBar` above the screen. User flagged via [`issues/hidden_top_bar.png`](issues/hidden_top_bar.png).
3.  **Third-pass learning — `adjustResize` alone wasn't enough.** With `enableEdgeToEdge()` on API 30+, `adjustResize` no longer physically resizes the window; the IME is reported as `WindowInsets.ime` instead. Scaffold's default `contentWindowInsets` does not include IME, so `bottomBar` doesn't auto-push above the keyboard. `Modifier.imePadding()` on the input bar is the canonical fix.

### 10.6.C — Auto-expanding input field (1 → 4 lines)

*   `ChatInputBar` replaced `BottomAppBar { Row(…) }` with `Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) { Row(verticalAlignment = Alignment.Bottom, …) }`. **`BottomAppBar`'s fixed `.height(BottomAppBarDefaults.ContainerHeight = 80.dp)` internal clamp defeated any `maxLines` cap on the TextField** — the ROADMAP-predicted "one-line fix via `maxLines = 4`" turned out to require a parent-container rewrite. The `Surface + Row` replacement preserves the M3 visual (same tonal colour + 3 dp elevation) while lifting the clamp.
*   `OutlinedTextField` got `minLines = 1, maxLines = 4` so it grows line-by-line with typed content and scrolls internally past four lines.
*   The placeholder `Text` got `maxLines = 1` + `TextOverflow.Ellipsis`. Without this, the Spanish placeholder copy `"Escribe un mensaje o toca el micro…"` wrapped to 2 lines on narrow phone widths, driving the empty field to render at 2-line height and defeating the compact empty-state intent. Surfaced during device verification after the first two changes shipped.

### Cross-cutting learning

All three phases exposed a single broader pattern: **Robolectric + Compose unit tests cannot reliably validate visual-layout anchoring, IME behaviour, or parent-container clamping interactions.** Tests that passed in the harness still produced broken UI on the device. Recorded in the auto-memory `ui_tasks_device_verification.md`: for any task touching `LazyColumn` anchoring, `Arrangement` alignments, `Modifier.imePadding` / `WindowInsets.ime`, `android:windowSoftInputMode`, Scaffold chrome placement, or parent height-clamping composables (`BottomAppBar`, etc.), device verification is mandatory before marking the task complete.

### 10.6.D — On-demand TTS on the last received assistant message (2026-04-19)

First output-side audio feature in the project, landing behind the Phase 10 Fork 6 Golden Rules narrowing. The architecture mirrors the existing `AudioRecorder` pattern deliberately so the codebase keeps one shape for all audio-adjacent lifecycle (capture on input, synthesize on output).

**Domain shape — `TextSpeaker` (`domain/audio/`):**

```kotlin
interface TextSpeaker {
    suspend fun speak(text: String, locale: Locale): Resource<Unit>
    fun stop()
    fun release()
}
```

*   `speak` is a suspending fire-and-await: it completes when the utterance plays through (via `UtteranceProgressListener.onDone`), when the coroutine is cancelled, or when the engine fails. `Locale` is `java.util.Locale` (pure JVM — no Android import, domain purity preserved).
*   `stop` and `release` mirror `AudioRecorder.release()` — synchronous, idempotent, safe from `onCleared`.

**Locale resolution — passed from caller, not derived in VM.** The ViewModel takes `Locale` as a parameter on `onTtsTap(messageId, text, locale)` rather than reading `Locale.getDefault()` internally. `ChatScreen` resolves it via `LocalConfiguration.current.locales[0]` so in-app locale changes (values-es/-pt resources) track correctly. This keeps the VM unit-testable without a Locale provider and isolates Android `Configuration` to the composable boundary.

**Playback lifecycle in `ChatViewModel`:**
*   `currentlySpeakingId: StateFlow<String?>` — consumed by `ChatScreenContent` to swap the `TtsPlayButton` icon between `VolumeUp` and `Stop`.
*   Internal `speakJob: Job?` holds the current playback coroutine. On retap of the same message: cancel + `textSpeaker.stop()` + clear ID. On tap of a different message while one is playing: cancel + stop + switch — never parallel playback.
*   `onCleared()` releases both `audioRecorder` and `textSpeaker` (extended the Phase 7.4.B lifecycle widening).

**`AndroidTextSpeaker` adapter (`data/local/audio/`):**
*   Async init via `TextToSpeech`'s `OnInitListener`, bridged to coroutines through `suspendCancellableCoroutine`. A `Mutex` guards the init-state machine (`NotInitialised` → `Ready` / `Failed` / `Released`) so concurrent `speak()` calls during first-use don't race the engine constructor.
*   Per-utterance flow: set language, register a fresh `UtteranceProgressListener` keyed by a `UUID` utterance id, call `tts.speak(..., QUEUE_FLUSH, ...)`. `onDone` → `Resource.Success`; `onError(id, errorCode)` → `AppError.TtsUnavailable.toResourceError()`. `invokeOnCancellation` calls `engine.stop()` so job cancellation aborts playback immediately.
*   `setLanguage` return codes `LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED` short-circuit to `TtsUnavailable` before enqueueing.
*   **No unit tests on the adapter — same convention as `MediaRecorderAudioRecorder`.** Framework types are hard to harness; correctness is verified on-device. Recorded so future work follows the same split (thin framework adapters = device-only verification; the `TextSpeaker` domain contract and VM wiring carry the unit-test load).

**UI placement rule (`ChatScreenContent`):** the `TtsPlayButton` renders beneath an assistant row **iff** `message.id == lastAssistantId && message.streamState == AssistantStreamState.RECEIVED`. Gated on *both* conditions deliberately:
*   The `== lastAssistantId` check prevents the button appearing on older RECEIVED replies (UX intent: read-aloud only on the latest turn).
*   The `== RECEIVED` check prevents the button flashing during LOADING / STREAMING / FAILED states.
If the last assistant row is LOADING, no row gets the button — not even older RECEIVED ones. Pinned by `olderReceivedAssistantWhenLatestIsLoading_noButtonOnAnyRow` in `ChatScreenContentTest`.

**Error routing:** `AppError.TtsUnavailable` is a `data object` (not `data class`) because the error carries no backend message — it's a local-origin failure. Routes through `Tier1Kind.TtsUnavailable` → `R.string.tier1_tts_unavailable` ("Read aloud isn't available on this device." / "La lectura en voz alta no está disponible en este dispositivo." / "A leitura em voz alta não está disponível neste dispositivo."). Tier-1 Snackbar, auto-dismissing — matches the UX grade of a missing-capability affordance.

**A11y pinning:** `cd_tts_play` / `cd_tts_stop` / `tier1_tts_unavailable` pinned byte-for-byte in `A11yStringMigrationTest` (en) + `EsTest` + `PtTest`. +9 assertions.
