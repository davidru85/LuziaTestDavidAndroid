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
4. **Accessibility content descriptions:** English hardcoded strings for now (e.g., "Record voice message", "Sending message"). Migration to `stringResource()` is a Phase 7 polish item.
