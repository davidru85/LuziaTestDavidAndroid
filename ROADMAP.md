# File: ROADMAP.md

## 🚀 Implementation Methodology: TDD
All development must follow the **Test-Driven Development** cycle to ensure maximum code reliability and prevent regressions.

1.  **🔴 RED:** Write a failing unit/integration test that defines the expected behavior. No production code should exist yet for this specific feature.
2.  **🟢 GREEN:** Write the **minimum** amount of production code required to make the test pass.
3.  **🔵 REFACTOR:** Clean up the code, improve naming, and optimize structure **while ensuring all tests remain green**.

---

## 📅 Execution Phases

### Phase 1: Foundation & Project Setup
*Goal: Establish a compilable, configured, and "empty" project skeleton.*

- [x] 1.1 **Dependencies:** Update `libs.versions.toml` (Hilt, K10, Ktor, Room, kotlinx.serialization, Testing libs).
- [x] 1.2 **Plugins:** Register all required Gradle plugins (`kotlin.serialization`, `hilt.android`, `ksp`).
- [x] 1.3 **App Entry:** Create `LuziaApp.kt` (`@HiltAndroidApp`) and `MainActivity.kt` (`@AndroidEntryPoint`).
- [x] 1.4 **Permissions:** Declare `RECORD_AUDIO` and `INTERNET` in `AndroidManifest.xml`.
- [x] 1.5 **Config:** Set up `BuildConfig.BASE_URL` via `local.properties`.
- [x] 1.6 **DI Skeleton:** Create placeholder Hilt modules (`NetworkModule`, `DatabaseModule`, `RepositoryModule`).
- [x] 1.7 **Build Variants:** Configure `staging` and `production` flavors.
- [x] 1.8 **Verification:** Execute `./gradlew assembleStagingDebug` $\rightarrow$ **Result: SUCCESS**.

### Phase 2: CI/CD Automation
*Goal: Automated validation of every commit.*

- [x] 2.1 **Workflow:** Create `.github/workflows/android-ci.yml`.
- [x] 2.2 **Pipeline:** Implement steps: `Lint` $\rightarrow$ `Unit Tests` $\rightarrow$ `Build`.

### Phase 3: Domain Layer (TDD)
*Goal: Implement the pure Kotlin business logic.*

- [x] **3.1 RED (Tests):** Write failing tests for `ChatMessage` model, `ChatRepository` interface, `TranscribeAudioUseCase`, and `SendMessageUseCase`.
- [x] **3.2 GREEN (Implementation):** Implement `ChatMessage`, `MessageRole`, `ChatRepository` (interface), and all `UseCases`.
- [x] **3.3 REFACTOR:** Ensure zero Android imports in `domain/` and verify `Resource<T>` utility.

### Phase 4: Data Layer (TDD)
*Goal: Implement the technical implementation of the domain interfaces.*

- [x] **4.1 RED (Tests):** Write failing tests for `SseParser`, `ChatMapper`, `L1ApiClient` (using `MockEngine`), and `ChatRepositoryImpl`.
- [x] **4.2 GREEN (Implementation):** Implement `SseParser`, `DTOs`, `Ktor Client`, `Room` entities/DAOs, and `ChatRepositoryImpl`.
- [x] **4.3 REFACTOR:** Finalize Hilt bindings (`@Binds`) and optimize `NetworkModule` (logging/interceptors).

### Phase 5: Presentation Layer (TDD)
*Goal: Implement the UI, ViewModels, and State Management.*

- [x] **5.1 Theme:** Configure Material 3 `Color`, `Type`, and `Theme` (Dynamic Color support).
- [x] **5.2 UI Models:** Implement `ChatUiState` (sealed interface) and `ChatMessageUiModel`.
- [x] **5.3 ViewModels:** Write tests for `ChatViewModel` (State transitions: `Idle` $\rightarrow$ `Recording` $\rightarrow$ `Streaming`). Implement `ViewModel` logic.
- [x] **5.4 Components:** Implement `MessageBubble`, `RecordButton`, and `StreamingIndicator` (test via `ComposeTestRule`).
- [x] **5.5 Screen:** Compose `ChatScreen` from the 5.4 leaves; integrate `ChatViewModel`, the persona system, Tier-2 retry, and permission flow. Forks + decision log recorded in [MEMORY.md](MEMORY.md). Delivered:
    - **Per-message persona architecture** (MEMORY Fork 1): dropped `MessageRole.SYSTEM`; added `ChatMessage.personaPrompt: String?`; `ChatMapper` emits the persona prompt as the wire `role` for user messages and `"assistant"` for assistant replies; destructive Room bump (v1 → v2) with `fallbackToDestructiveMigration(dropAllTables = true)`.
    - **Persona catalog**: `Persona` enum (`STUDENT`/`SCIENTIST`/`ARTIST`) + `PersonaEntry` data class + `PersonaCatalog` domain interface; pure-JVM `DefaultPersonaCatalog` impl wired via `CatalogModule` Hilt provider; `role_names` / `role_prompts` string arrays in `strings.xml`.
    - **Tier-2 retry flow** (MEMORY Fork 2): `ChatRepository.deleteMessage(id)` + DAO `@Query`; `ChatViewModel.onRetryLastFailure()` deletes the last FAILED assistant row and re-invokes `StreamAssistantReplyUseCase` with cleaned history; `AssistantMessageBubble` gained optional `onRetry` that renders only on FAILED.
    - **ViewModel extensions**: `selectedPersona: StateFlow<Persona>` (default `STUDENT`), `onPersonaSelected(...)`, and `onSendTap` captures the active persona prompt per user message.
    - **Compose leaves**: `RoleSelectorChips` (single-select `FilterChip` strip, `Role.RadioButton` semantics), `ChatInputBar` (OutlinedTextField + MorphingActionButton + StreamingIndicator inside `BottomAppBar`), `ChatTopAppBar` (DeleteSweep icon + confirm `AlertDialog`, disabled when empty).
    - **Screen scaffold**: `ChatScreenContent` stateless Scaffold composing all leaves with LazyColumn message list; `ChatScreen` VM-wired wrapper handling `RECORD_AUDIO` permission launcher + rationale dialog + Snackbar events (absorbs former Phase 6.2).
    - **Activity wiring**: `MainActivity` hosts `ChatScreen` as the sole entry point (absorbs former Phase 6.1).

### Phase 6: Integration & Wiring
*Absorbed into Phase 5.5 — see [MEMORY.md](MEMORY.md) Fork 3.*

- [x] ~~6.1 **Activity Wiring:** Host `ChatScreen` in `MainActivity`.~~ → **5.5**
- [x] ~~6.2 **Runtime Permissions:** Implement `ActivityResultLauncher` for `RECORD_AUDIO`.~~ → **5.5**
 
### Phase 7: Polish & QA
*Goal: Refine the UX and ensure robustness.*

- [x] **7.0 Error Classification (deferred from Phase 5.5 — Fork 3):** 3-Tier error strategy implemented end-to-end per `TECHNICAL_SPEC.md §Error Handling`. `AppError` domain model + `ErrorMapper` classification + `ChatEvent.Tier1`/`Tier3` + `ChatScreen` AlertDialog host. Tier-2 (inline bubble + retry) continues to ride on `MessageStatus.FAILED` as already delivered in 5.5. 230 unit tests green.
    - [x] **7.0.A** Domain: `AppError` sealed class (`BadRequest` / `FileTooLarge` / `Timeout` / `Network` / `ServiceUnavailable` / `Internal` / `Unknown`) + `AppError.fromCode(...)` factory; extend `Resource.Error` with optional `error: AppError?`. Domain purity preserved.
    - [x] **7.0.B** Data: `ErrorMapper` (`class @Inject` under `data/remote/mapper/`) classifies Ktor/IO throwables (`HttpRequestTimeoutException`/`SocketTimeoutException` → `Timeout`; `UnknownHostException`/`ConnectException`/`IOException` → `Network`; `ClientRequestException` 400/413 → `BadRequest`/`FileTooLarge`; `ServerResponseException` 503/5xx → `ServiceUnavailable`/`Internal`; fallback → `Unknown`). SSE `event: error` flows via `AppError.fromCode(code, message)`. `ChatRepositoryImpl` now injects `ErrorMapper` and populates `Resource.Error.error`. MockEngine produces real Ktor exception instances in tests.
    - [x] **7.0.C** Presentation: `ChatEvent` replaced with `Tier1(message)` / `Tier3(title, message)` subtypes; `AppError.toChatEvent()` extension routes BadRequest/FileTooLarge/Timeout/Network → Tier1, ServiceUnavailable/Internal/Unknown → Tier3. `ChatViewModel.Resource.Error.toTieredEvent()` picks from the AppError or falls back to Tier1 for legacy errors with no `AppError`. `ChatScreen` renders Tier-1 via Snackbar (existing) and Tier-3 via a new `AlertDialog` host. Hardcoded English titles — i18n deferred to a later polish pass.
- [ ] 7.1 **End-to-End manual testing:** Record $\rightarrow$ Transcribe $\rightarrow$ Stream $\rightarrow$ Display.
    - [x] **7.1.1** `/transcribe` endpoint manual happy path — confirmed working end-to-end (audio captured → multipart upload → transcript returned → draft populated).
    - [ ] **7.1.2** `/chat` endpoint manual happy path — **blocked:** request/response contract mismatch between the app and the backend discovered during manual testing. See 7.1.3.
    - [ ] **7.1.3** Fix `/chat` request/response contract discrepancy (found in 7.1.2). Scope to be determined after inspecting the exact mismatch (likely involves `TECHNICAL_SPEC.md §API Contracts #2` + `ChatMapper` / `ChatRequestDto` / SSE parsing).
- [ ] 7.2 **Edge Cases:** Test empty audio, network loss, and extremely long AI responses.
- [ ] 7.3 **Accessibility:** Verify `TalkBack` support and `contentDescription` updates.
- [ ] 7.4 **Cleanup:** Delete temporary `.m4a` files and `MediaRecorder` resource release.

### Phase 8: Final Audit
- [ ] 8.1 **Unit Tests:** Verify 100% coverage of `UseCases` and `Mappers`.
- [ ] 8.2 **Lint & Build:** `./gradlew lintStagingDebug` $\rightarrow$ **Result: ZERO ERRORS**.

### Phase 9: Documentation
- [ ] 9.1 **README:** Complete the professional `README.md` (Design Decisions, Tech Stack, Setup).
  - Include a **"Future Improvements / Next Steps"** section covering:
    - **Room 2.x → Room 3.x migration:** Switch `androidx.room` → `androidx.room3` once 3.x reaches stable. Room 3 offers coroutine-first APIs (`withWriteTransaction`), the new `SQLiteDriver` surface, Kotlin Multiplatform support, and `@DaoReturnTypeConverters`. Deferred from Phase 4.3 because 3.x is alpha as of 2026-04-17; stability outweighs the upside for this build.
    - (Additional items to capture as they arise during Phases 5–8.)