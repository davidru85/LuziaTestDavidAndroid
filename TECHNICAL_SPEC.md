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

### 2. POST `/chat` (LLM Streaming)
*   **Content-Type:** `application/json`
*   **Request Body:** The `messages` array carries the full conversation history. The `role` field is populated **per-message**:
    *   **User messages:** `role` = the persona prompt string (from `role_prompts`) that was active when the message was sent.
    *   **Assistant messages:** `role` = `"assistant"`.
    *   There is **no standalone `system` entry**; the persona prompt rides on every user message.
*   **Example** (first user turn in "Student" mode, assistant reply, second user turn in "Artist" mode):
    ```json
    {
      "messages": [
        {
          "role": "You are a patient, educational tutor. Explain concepts step by step and encourage learning.",
          "content": "¿Cómo funciona la fotosíntesis?"
        },
        { "role": "assistant", "content": "La fotosíntesis es el proceso…" },
        {
          "role": "You are a creative artist. Think imaginatively, brainstorm ideas, and inspire creativity.",
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
| **Tier 1** | `Snackbar` | Validation/Format errors (`BAD_REQUEST`, `FILE_TOO_LARGE`) | Auto-dismissing, non-blocking. |
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
3.  **Async Testing:** Use `runTest` and `StandardTestDispatcher`. **Never** use `Thread.sleep()`. Inject dispatchers via Hilt to allow for deterministic testing.