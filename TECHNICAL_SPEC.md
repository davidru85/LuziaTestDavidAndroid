# File: TECHNICAL_SPEC.md

## 🏗 Architecture & Layer Rules
The project follows **Clean Architecture** with a strict dependency unidirectional flow.

### The Import Guard (Non-Negotiable)
To maintain testability and separation of concerns, the following rules must be enforced by the compiler/linter:

| Package | Forbidden Imports | Reason |
| :--- | :--- | :--- |
| `domain/` | `android.*`, `androidx.*`, `...data.*` | Must be pure Kotlin/JVM for unit testing. |
| `data/` | `...presentation.*` | Data layer must be agnostic of the UI. |
| `presentation/` | `...data.*` | Presentation must only depend on `domain`. |

### Data Flow
`UI Event` $\rightarrow$ `ViewModel` $\rightarrow$ `UseCase` $\rightarrow$ `Repository` $\rightarrow$ `DataSource (Remote/Local)`

---

## 📡 API Contracts
**Base URL:** `http://127.0.0.1:8000` (Configured via `local.properties` $\rightarrow$ `BuildConfig`).

### 1. POST `/transcribe` (Audio to Text)
*   **Content-Type:** `multipart/form-data`
*   **Payload:** Field `audio` (Binary, `.m4a` format, AAC encoder).
*   **Success Response:**
    ```json
    { "text": "transcribed text" }
    ```
*   **Error responses — byte-level verbatim `message` strings** *(API_SPEC v1.2.0, 2026-04-18 — see MEMORY.md Fork 5)*:

    | Trigger                                                        | HTTP | `error.code`         | `error.message`                                       |
    | :------------------------------------------------------------- | :--- | :------------------- | :---------------------------------------------------- |
    | Missing `audio` field                                          | 400  | `BAD_REQUEST`        | `Missing required field: audio.`                      |
    | **Empty audio (0 bytes)**                                      | **400** | **`BAD_REQUEST`**    | **`Audio file is empty or too short to transcribe.`** |
    | **Too-short / corrupted audio (Whisper 4xx path)**             | **400** | **`BAD_REQUEST`**    | **`Audio file is empty or too short to transcribe.`** |
    | Audio > 25 MB                                                  | 413  | `FILE_TOO_LARGE`     | `Audio file exceeds the 25 MB size limit.`            |
    | MIME ≠ `audio/mp4`                                             | 415  | `UNSUPPORTED_FORMAT` | `Unsupported audio format. Accepted format: m4a.`     |
    | Whisper-side 5xx / network / timeout                           | 502  | `UPSTREAM_ERROR`     | `Transcription service failed. Please try again.`     |

    All error responses follow the standard envelope (`§3 Global Error Schema`).

    **Retry semantics:** `502 UPSTREAM_ERROR` is retry-appropriate (Whisper outage). `400 BAD_REQUEST` is **not** retry-appropriate — it indicates user-actionable input (prompt the user to re-record, don't auto-retry). The Android client must not auto-retry 4xx responses on `/transcribe`.

    **Whisper edge case (non-error):** audio that is short but passes the backend's duration floor is forwarded to Whisper, which returns a legitimate `{"text":"..."}` response. In practice Whisper tends to hallucinate `"you"` for near-silent or ambiguous short clips. This is a `200 OK` (not an error) and the client has no way to distinguish it from a valid transcription. Surface as-is; treat as user-visible Whisper output.

### 2. POST `/chat` (LLM Streaming)
*   **Content-Type:** `application/json`
*   **Request Body:** The `messages` array carries the full conversation history. Each message has **three fields**:
    *   **`role`** *(string, required)* — enum identifying the author of the message, following standard LLM conventions. Allowed values: `"user"`, `"assistant"`, `"system"`.
    *   **`role_prompt`** *(string, required on `"user"` turns; optional-but-ignored on `"assistant"` / `"system"` turns)* — the persona prompt (from `role_prompts`) that was active when the user sent the message. On user turns the backend reads it to steer the reply (see server-side semantics below). On non-user turns the backend silently drops it, so sending or omitting is equivalent; the Android client **omits it** for payload minimality.
    *   **`content`** *(string, required)* — the textual content of the message.
*   **Per-message persona capture semantics** (preserved from prior contract, moved from `role` to `role_prompt`):
    *   When a user message is created, the persona prompt active at that moment is captured and persisted as `role_prompt`.
    *   Changing the persona mid-conversation affects only **subsequent** user messages; historical user messages retain the `role_prompt` active when they were sent.
    *   **Android never emits `"system"`** turns. The enum value exists because the backend is the sole producer of `system` turns (injected server-side — see below). If the client ever needed to model `system`, it would follow the same rules as `assistant` (no `role_prompt`, non-empty `content`).
*   **Server-side persona injection (informational — the backend does this, the client cannot observe it):**
    1.  The backend reads the `role_prompt` of the **latest** `"user"` message in `messages`.
    2.  Prepends a synthetic `{"role":"system","content":<that role_prompt>}` turn at the head of the payload forwarded to OpenAI.
    3.  Strips `role_prompt` from every message.
    4.  Drops any `"assistant"` message whose `content` is an empty string.
    5.  Forwards the resulting standard `{role, content}` list to OpenAI.

    Consequence: **only the last user turn's `role_prompt` steers the next reply**. Historical `role_prompt`s on earlier user turns are required to be valid (see validation rules) but are audit metadata only — they do not re-enter OpenAI. No server-side memory, no per-turn interleaved system turns.
*   **Empty-`content` handling (per role):**
    | Role          | Empty `content`                                                             |
    | :------------ | :-------------------------------------------------------------------------- |
    | `"user"`      | **422 rejected**                                                            |
    | `"system"`    | **422 rejected**                                                            |
    | `"assistant"` | Accepted on the wire, but silently **dropped** before forwarding to OpenAI. |

    Practical meaning for the Android client: the transient empty-assistant-placeholder that may appear while an SSE stream is in flight is wire-safe (no 422) but has zero effect on the LLM. Cleanup of lingering empty-content assistant rows in the local history is a separate client concern (ROADMAP 7.1.4).
*   **Validation errors — verbatim 422 `message` strings** *(match byte-for-byte; the client's error mapper keys off these)*:
    | Trigger                                              | `message`                                                          |
    | :--------------------------------------------------- | :----------------------------------------------------------------- |
    | Empty `messages` array                               | `Messages array must contain at least one message.`                |
    | `role` not in the enum                               | `role must be one of user\|assistant\|system.`                     |
    | Missing / empty `role_prompt` on a `"user"` turn     | `user messages must include role_prompt.`                          |
    | Empty `content` on a `"user"` or `"system"` turn     | `content must be a non-empty string for user and system messages.` |
    | Missing field / wrong type (fallback)                | `Each message must have 'role' and 'content' fields.`              |

    All 422 responses share `code: "VALIDATION_ERROR"` and the standard `{"error":{"code":"...","message":"..."}}` envelope (`§3 Global Error Schema`).
*   **Backwards compatibility:** **none.** The client and backend cut over to the three-field schema in the same release. Mixed / two-field payloads will 422.
*   **Example** (first user turn in "Student" mode, assistant reply, second user turn in "Artist" mode):
    ```json
    {
      "messages": [
        {
          "role": "user",
          "role_prompt": "You are a patient, educational tutor. Explain concepts step by step and encourage learning.",
          "content": "¿Cómo funciona la fotosíntesis?"
        },
        {
          "role": "assistant",
          "content": "La fotosíntesis es el proceso…"
        },
        {
          "role": "user",
          "role_prompt": "You are a creative artist. Think imaginatively, brainstorm ideas, and inspire creativity.",
          "content": "Ahora escríbelo como un poema"
        }
      ]
    }
    ```
*   **Response Type:** `text/event-stream` (SSE)
*   **Stream Format:**
    *   Token chunk: `data: <token>\n\n`
    *   End Sentinel: `data: [DONE]\n\n`
    *   Error Event: `event: error\ndata: {"code": "...", "message": "..."}\n\n`

### 3. Global Error Schema
All error responses follow this structure:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable description."
  }
}
```

---

## 🛠 Networking & Streaming Implementation
### Ktor Client Configuration
*   **Engine:** `OkHttp` (for Android stability).
*   **Plugins:** `ContentNegotiation` (with `kotlinx.serialization`), `Logging` (LogLevel.BODY in debug), `DefaultRequest`.
*   **SSE Parsing:** Use `preparePost()` and `ByteReadChannel.readUTF8Line()`. Parse the `data:` prefix and detect the `[DONE]` sentinel.

### Threading Model (Dispatchers)
All threading must be handled in the **Repository/DataSource** layer using `flowOn()`.
*   **`Dispatchers.IO`:** Network calls, Room database operations, SSE parsing.
*   **`Dispatchers.Main`:** `MediaRecorder` lifecycle (start/stop) and UI State mutations.
*   **`Dispatchers.Default`:** Heavy data processing (e.g., large string manipulation).

---

## ⚠️ Error Handling Strategy (3-Tier System)
Errors must be mapped to a `sealed class AppError` in the domain layer.

| Tier | Mechanism | Triggering Condition | UI Implementation |
| :--- | :--- | :--- | :--- |
| **Tier 1** | `Snackbar` | Validation/Format errors (`VALIDATION_ERROR`, `BAD_REQUEST`, `FILE_TOO_LARGE`) | Auto-dismissing, non-blocking. |
| **Tier 2** | `Inline Bubble` | Connectivity/Network errors (`TIMEOUT`, `NETWORK_ERROR`) | Error icon in the message bubble + **Retry Button**. |
| **Tier 3** | `AlertDialog` | Critical/Server errors (`INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`) | Modal, requires user dismissal. |

### The `Resource<T>` Pattern
All repository methods must return a `Resource` wrapper:
```kotlin
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>
}
```

---

## 🧪 Testing Protocol (TDD)
The project follows the **RED $\rightarrow$ GREEN $\rightarrow$ REFACTOR** cycle.

1.  **Unit Testing:**
    *   **Tools:** `JUnit 5`, `MockK`, `Turbine` (for Flow/SSE testing).
    *   **Target:** `UseCases`, `Mappers`, `SseParser`, `ViewModels`.
2.  **Integration Testing:**
    *   **Tools:** `Ktor MockEngine`, `Room in-memory database`.
    *   **Target:** `RepositoryImpl`, `LuziaApiClient`, `DAO`.
3.  **Compose / UI Testing:**
    *   **Tools:** `createComposeRule` + `Robolectric` (JUnit 4 runner via `junit-vintage-engine`).
    *   **Target:** individual Compose leaves (`AssistantMessageBubble`, `ChatInputBar`, etc.) and the stateless `ChatScreenContent` scaffold.
    *   **Limitation** (recorded in `MEMORY.md §Phase 10.6 — Chat UX Layout Decisions`): Robolectric's layout pass does **not** exercise real-device visual anchoring, IME show/hide, or parent-container clamping behaviour. Any task touching `LazyColumn` anchoring, `Arrangement.*`, `Modifier.imePadding` / `WindowInsets.ime`, `android:windowSoftInputMode`, Scaffold chrome placement, or `BottomAppBar`-style height-clamping parents **requires device verification** before being marked complete.
4.  **Activity-Level Smoke Testing (Phase 8):**
    *   **Tools:** `@HiltAndroidTest` + `HiltAndroidRule` + `@UninstallModules(NetworkModule, DatabaseModule, AudioModule)` + `@Config(application = HiltTestApplication::class)` + `RobolectricTestRunner` + `ActivityScenario`.
    *   **Target:** `MainActivitySmokeTest` — a single test that launches the real activity, materialises the Hilt test graph, and asserts `ChatScreen` renders core chrome (mic button, brand title, persona chips, disabled clear button).
    *   **Test module:** `TestAppModule` (under `src/test/`) substitutes three production modules that can't boot under Robolectric: `MockEngine`-backed `HttpClient` (replaces `NetworkModule`), in-memory `LuziaDatabase` (replaces `DatabaseModule`), and a fake `AudioRecorder` (replaces `AudioModule`). `CatalogModule` / `RepositoryModule` / `DispatcherModule` stay intact — they have no native / system-service dependencies.
5.  **Coverage (Phase 10.2.A):** Jacoco line/branch coverage report via `./gradlew :app:jacocoStagingDebugCoverageReport`. HTML + XML output under `app/build/reports/jacoco/`. Uses AGP's built-in `enableUnitTestCoverage = true` on the debug build type — class directories point at `intermediates/classes/<variant>/transformStagingDebugClassesWithAsm/dirs` (AGP 9 post-ASM path). Excludes cover Hilt factories, Room generated impls, Compose singletons, `R` / `BuildConfig` / `Manifest` / test classes. Compose UI packages report 0% because Jacoco cannot trace through Compose lambda indirection — `ChatScreenContentTest` + Compose leaf tests still cover those paths, just not numerically.
6.  **Async Testing:** Use `runTest` and `StandardTestDispatcher`. **Never** use `Thread.sleep()`. Inject dispatchers via Hilt to allow for deterministic testing.