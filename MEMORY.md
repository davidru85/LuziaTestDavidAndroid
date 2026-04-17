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
*   **Tier-3 AlertDialog:** **Deferred to Phase 7.** All errors surface as Tier-1 Snackbars for now. Tier classification inside `ChatEvent` is Phase 7 scope.

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
