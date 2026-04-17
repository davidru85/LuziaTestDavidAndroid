# File: DESIGN_SYSTEM.md

## 🎨 Design Philosophy: Material 3 Expressive
The UI follows **Material 3 Expressive** principles: adaptive shapes, motion-driven feedback, and dynamic personalization (Material You). The focus is on reducing "latency anxiety" during AI processing through fluid transitions.

## 📱 Screen Architecture: Single-Screen Layout
A single `ConversationScreen` with four vertical zones:
1.  **Top App Bar:** Title + `DeleteSweep` action.
2.  **Role Selector:** Horizontal `FilterChip` strip (Single-select).
3.  **Message List:** `LazyColumn` (Chronological, bottom-up).
4.  **Input Bar:** `TextField` + Adaptive `IconButton` (Mic $\leftrightarrow$ Send).

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
    *   `LOADING`: **Shimmer Effect**. Use a `ShimmerBox` (animated gradient) representing 2-3 lines of text.
    *   `RECEIVED`: Text content. During SSE streaming, use a soft `alpha` fade-in for each new token.

### 4. Input Bar (Adaptive Interaction)
*   **Container:** `BottomAppBar` with `WindowInsets` handling.
*   **TextField:** `OutlinedTextField` (M3) with placeholder `"Type a message or tap the mic…"`.
*   **The Morphing Button:** Use `AnimatedContent` to switch between:
    *   **Mode A (Empty Field):** `FilledIconButton` with `Icons.Filled.Mic`.
    *   **Mode B (Text Present):** `IconButton` with `Icons.AutoMirrored.Filled.Send`.

---

## 🔄 Interaction & Motion Logic

### The Voice Lifecycle (State Machine)
The UI must react to the `UiState` transitions:
1.  **`Idle`**: Standard view.
2.  **`Listening`**: Mic button pulses (`animateFloatAsState` scale/ripple). `LinearProgressIndicator` (Indeterminate) appears above the input bar.
3.  **`Processing`**: Input bar disabled. `LinearProgressIndicator` stays active with label `"Thinking..."`.
4.  **`Streaming`**: `LazyColumn` uses `animateScrollToItem` to follow the latest token.

### Auto-Scroll Logic
*   **Constraint:** Only scroll to bottom if the user is already near the bottom.
*   **Implementation:** Use `derivedStateOf` on `LazyListState`. If `firstVisibleItemIndex` is far from the end, suspend auto-scrolling to allow the user to read history.

---

## ♿ Accessibility (A11y) Checklist
*   **Touch Targets:** All interactive elements $\ge$ 48x48dp.
*   **Semantics:**
    *   Mic button: `contentDescription` must toggle between `"Record voice message"` and `"Stop recording"`.
    *   Role Chips: Use `role = Role.RadioButton` for selection semantics.
    *   Message Bubbles: Use `LiveRegion.Polite` for streaming text so TalkBack announces updates.
*   **Contrast:** Ensure all `onContainer` colors meet WCWC AA standards.