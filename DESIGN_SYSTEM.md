# File: DESIGN_SYSTEM.md

## 🎨 Design Philosophy: Material 3 Expressive
The UI follows **Material 3 Expressive** principles: adaptive shapes, motion-driven feedback, and dynamic personalization (Material You). The focus is on reducing "latency anxiety" during AI processing through fluid transitions.

## 📱 Screen Architecture: Single-Screen Layout
A single `ChatScreen` with **three anchored zones** (updated Phase 10.6.B — role selector moved out of the scrolling content area into the fixed top chrome):

1.  **Top chrome — stacked inside `Scaffold.topBar`:**
    *   `ChatTopAppBar` — title + `Icons.Outlined.DeleteSweep` action.
    *   `RoleSelectorChips` — horizontal `FilterChip` strip (single-select).
    Both stay pinned when the IME opens, so the user always sees the persona selector, app title, and delete affordance even while typing.
2.  **Message List:** `LazyColumn` between top chrome and input bar. Messages rendered in natural (chronological) order with `verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)` — short conversations hug the input bar with empty space above; long conversations fill the viewport and rely on a `LaunchedEffect` keeping the newest message visible.
3.  **Input Bar:** `ChatInputBar` — auto-expanding `OutlinedTextField` (1 → 4 lines) + `MorphingActionButton` (Mic ↔ Send). Pinned above the IME via `Modifier.imePadding()`.

---

## 🧩 Component Specifications

### 1. Top App Bar
*   **Title:** `titleLarge` typography.
*   **Action:** `IconButton` (`Icons.Outlined.DeleteSweep`).
*   **Behavior:** Triggers a `ConfirmDelete` AlertDialog. Button is disabled if the conversation is empty.

### 2. Role Selector (Persona Selection)
*   **Type:** `FilterChip` (Horizontal Scroll).
*   **Selection Mode:** `SingleSelect`.
*   **Visuals:** Selected state uses `primaryContainer` fill + leading checkmark.
*   **Personas:** Exactly three, declared in `res/values/strings.xml` as parallel string arrays `role_names` and `role_prompts` (indexes correspond 1:1):
    *   **Student** — *"You are a patient, educational tutor. Explain concepts step by step and encourage learning."*
    *   **Scientist** — *"You are a rigorous scientist. Provide evidence-based, analytical, and precise answers."*
    *   **Artist** — *"You are a creative artist. Think imaginatively, brainstorm ideas, and inspire creativity."*
*   **Default persona:** Student (index 0) on app launch.
*   **Logic — per-message persona capture:**
    *   **No `system` message is ever inserted** at index 0 of the history. The `system` role is removed from the domain model.
    *   When a **user** message is created, the persona prompt active at that moment is captured and becomes the message's serialized `role` field.
    *   Assistant messages continue to serialize with `"role": "assistant"`.
    *   Changing the persona mid-conversation affects only **subsequent** user messages; historical user messages retain the persona that was active when they were sent.

### 3. Message Bubbles (The Core Component)
All messages are **text-only**. No audio players or waveforms.

#### **User Message (Right-aligned)**
*   **Container:** `Surface` with `ShapeDefaults.ExtraLarge`.
*   **Color:** `primaryContainer`.
*   **States:**
    *   `SENDING`: `alpha = 0.7f` + `CircularProgressIndicator` (14dp) instead of checkmark.
    *   `SENT`: `alpha = 1.0f` + `Icons.Filled.Check` (14dp) in `onSurfaceVariant`.

#### **AI Message (Left-aligned)**
*   **Container:** `Surface` with `ShapeDefaults.ExtraLarge`.
*   **Color:** `surfaceVariant`.
*   **States:**
    *   `LOADING`: **Shimmer Effect**. Use a `ShimmerBox` (animated gradient) representing 2–3 lines of text. `LiveRegion.Polite` so TalkBack announces *"Loading response"* when the shimmer appears.
    *   `STREAMING`: Partial content as SSE tokens arrive; soft `alpha` fade-in per token. `LiveRegion.Polite` so TalkBack announces new tokens as they stream in.
    *   `RECEIVED`: Complete text content. **Not** a live region — historical messages should not be re-announced when the user scrolls them back into view.
    *   `FAILED`: Warning icon + friendly copy that varies by retryability (Phase 7.3.3.B):
        *   *Latest* failure (retryable) → *"I've gone blank. Mind retrying?"*
        *   *Older* failures (non-retryable) → *"Sorry, empty message"*
        The retry button itself is rendered **beneath** the bubble (not inside it) by `ChatScreenContent`, as a `RetryAssistantReplyButton` `TextButton` — moved out in Phase 10.6.A because the in-bubble IconButton got visually cramped in longer-text locales (Spanish, Portuguese).

### 4. Input Bar (Adaptive Interaction)
*   **Container:** `Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp)` wrapping a `Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp))`.
    *   **Replaced `BottomAppBar` in Phase 10.6.C.** `BottomAppBar` applies a fixed `.height(BottomAppBarDefaults.ContainerHeight = 80.dp)` internally, which clamped the TextField and defeated any `maxLines` cap. The `Surface + Row` replacement lifts the clamp while preserving the M3 visual (same tonal colour + 3 dp elevation).
*   **IME handling:** `Modifier.imePadding()` on the input bar composable so it anchors above the keyboard. Scaffold's default `contentWindowInsets` does NOT include `WindowInsets.ime` on API 30+ edge-to-edge, so the padding must be applied explicitly.
*   **TextField:** `OutlinedTextField` (M3) with placeholder `"Type a message or tap the mic…"`.
    *   **Auto-expanding 1 → 4 lines** (Phase 10.6.C): `minLines = 1, maxLines = 4`. The field starts single-line, grows as the user types, caps at four lines, then scrolls internally.
    *   **Placeholder** constrained to `maxLines = 1` + `TextOverflow.Ellipsis` so long locale copy (e.g., Spanish *"Escribe un mensaje o toca el micro…"*) cannot wrap and force the empty field to render at 2-line height.
*   **The Morphing Button:** `AnimatedContent` switching between:
    *   **Mode A (Empty Field, Not Recording):** `FilledIconButton` with `Icons.Filled.Mic`.
    *   **Mode B (Recording):** `FilledIconButton` with `Icons.Filled.Stop` (7.3.3.A).
    *   **Mode C (Text Present):** `IconButton` with `Icons.AutoMirrored.Filled.Send`.

---

## 🔄 Interaction & Motion Logic

### The Voice Lifecycle (State Machine)
The UI must react to the `UiState` transitions:
1.  **`Idle`**: Standard view.
2.  **`Listening`**: Mic button pulses (`animateFloatAsState` scale/ripple). `LinearProgressIndicator` (Indeterminate) appears above the input bar.
3.  **`Processing`**: Input bar disabled. `LinearProgressIndicator` stays active with label `"Thinking..."`.
4.  **`Streaming`**: `LazyColumn` uses `animateScrollToItem` to follow the latest token.

### Auto-Scroll Logic (updated Phase 10.6.B)
*   **Short lists (content fits the viewport):** anchored to the bottom via `verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)` — no scrolling needed; messages naturally hug the input bar with empty space above the oldest.
*   **Long lists (content exceeds the viewport):** a `LaunchedEffect(state.messages.size) { listState.scrollToItem(state.messages.lastIndex) }` advances the viewport every time the message count changes. This covers both new messages arriving and the streaming case where the final assistant row extends in-place.
*   **Why not `reverseLayout = true`?** Compose docs state explicitly that `reverseLayout` does **not** change `verticalArrangement` alignment, and the default `Arrangement.spacedBy(space)` uses `Alignment.Top` regardless — which overrides the bottom-anchor the `reverseLayout` default would otherwise provide. Using `Arrangement.spacedBy(_, Alignment.Bottom)` + natural item order + an explicit `LaunchedEffect` is less subtle and matches real device expectations.

---

## ♿ Accessibility (A11y) Checklist
*   **Touch Targets:** All interactive elements $\ge$ 48x48dp.
*   **Semantics:**
    *   Mic button: `contentDescription` must toggle between `"Record voice message"` and `"Stop recording"`.
    *   Role Chips: Use `role = Role.RadioButton` for selection semantics.
    *   Message Bubbles: Use `LiveRegion.Polite` for streaming text so TalkBack announces updates.
*   **Contrast:** Ensure all `onContainer` colors meet WCWC AA standards.