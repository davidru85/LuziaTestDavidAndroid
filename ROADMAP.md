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
- [x] **7.1 End-to-End manual testing** — Record $\rightarrow$ Transcribe $\rightarrow$ Stream $\rightarrow$ Display verified end-to-end against the backend Fork 4 contract (2026-04-18). The Phase started with a 422 incident on `/chat`, drove a coordinated contract revision with the backend team (MEMORY.md Fork 4), and closed with manual E2E confirmation of both happy path and Tier-1 error routing. Sub-tasks:
    - **7.1.1** `/transcribe` manual happy path confirmed — audio → multipart upload → transcript → draft populated.
    - **7.1.2** `/chat` manual happy path confirmed (after 7.1.3 landed) — 200 OK + SSE stream end-to-end.
    - **7.1.3** Revised `/chat` wire contract (Fork 4: three-field message shape `role` enum + `role_prompt` + `content`) shipped across four TDD sub-tasks:
        - **7.1.3.A** `ChatMessageDto` + `ChatMapper` three-field shape; strict `IllegalStateException` on null/blank `personaPrompt` for user turns (fail-fast guard); `NetworkModule` Json gains `explicitNulls = false` so absent `role_prompt` on assistants is omitted from the wire (not serialised as `null`).
        - **7.1.3.B** `AppError.ValidationError(rawMessage)` variant routing `VALIDATION_ERROR` → Tier-1 Snackbar with verbatim backend message preserved.
        - **7.1.3.D** `ErrorMapper` parses the backend `{error:{code,message}}` envelope on 4xx/5xx responses (new `ApiErrorEnvelope` DTO, `fromThrowable` is now `suspend`, `readEnvelope()` → `AppError.fromCode(...)` with `classifyByStatus()` as fallback when body is empty/malformed).
        - **7.1.3.C** Manual device verification: happy path (Check 1) and `mockable.io`-backed 422 (Check 2) both confirmed an auto-dismissing Tier-1 Snackbar — proving the Ktor Logging plugin does not consume the response body before `ErrorMapper` reads it in debug builds.
    - **7.1.3.E** *(queued for Phase 7.3)* Tier-3 AlertDialog UX redesign — functional but visually under-polished; tracked in `phase7_polish_deferred` memo.
    - **Invariant captured:** Room migrations in future 7.1.x sub-tasks (or beyond, while pre-launch) default to destructive — no user data to preserve.
    - Final state: 242 unit tests green, domain + presentation purity green, `assembleStagingDebug` green.
- [x] **7.1.4 Clean up orphaned assistant rows from failed streams** *(follow-up from Phase 7.1.2 Finding 3)* — shipped as **Option A** (wire-level filter). `ChatMapper.toRequestDto()` now drops any assistant message whose `content` is blank before building the DTO (new `ChatMessage.isBlankAssistant()` private extension). This prevents the transient PENDING → FAILED empty placeholders inserted by `StreamAssistantReplyUseCase` from re-poisoning subsequent `/chat` payloads. Tier-2 retry flow and `onRetryLastFailure()` are untouched — FAILED bubbles remain visible in history for the user to interact with. The deeper UX redesign (delete-instead-of-mark-FAILED, rework retry semantics to target the last user message) was explicitly scoped out of 7.1.4 and deferred to Phase 7.3 alongside the Tier-3 AlertDialog polish (7.1.3.E). 247 unit tests green.
- [ ] 7.2 **Edge Cases:** Test empty audio, network loss, and extremely long AI responses.
    - **Manual test findings (2026-04-18):**
        - *Empty audio / too-short audio* → backend returns `400 BAD_REQUEST` with the new verbatim `"Audio file is empty or too short to transcribe."` message (per Fork 5 / API_SPEC v1.2.0). Short-but-above-threshold clips pass through to Whisper and come back as `200 OK { "text": "you" }` (Whisper hallucination for near-silence) — indistinguishable from a valid transcription on the client; no special handling.
        - *Network loss mid-stream* → Tier-1 Snackbar `"Network connection failed."` — working as designed via `AppError.Network` → `ChatEvent.Tier1` → Snackbar (unchanged since Phase 7.0).
        - *Long responses* → backend caps replies at 1024 tokens per message; client imposes no additional limits. SSE pipeline comfortably handles the cap. No client-side work needed.
    - **Client gap discovered:** `AppError.BadRequest` (and by symmetry `FileTooLarge`, `Timeout`, `Network`, `ServiceUnavailable`, `Internal`) are `data object` singletons with **fixed generic messages**. `AppError.fromCode("BAD_REQUEST", backendMessage)` ignores the `backendMessage` argument, so the verbatim backend string (e.g., `"Audio file is empty or too short to transcribe."`) is **discarded** on the way to the user. The Snackbar shows `"The request was invalid."` instead of the actionable re-record hint. Latent since Phase 7.0 but made user-visible by Fork 5. Fix scoped as 7.2.A below.
    - [ ] **7.2.A** Preserve backend-supplied `message` strings verbatim in `AppError` singletons — **🔴 RED:** update `AppErrorTest` to assert `fromCode("BAD_REQUEST", "...")` / `fromCode("FILE_TOO_LARGE", "...")` / etc. preserve the supplied message when non-blank, fall back to the generic default when null/blank. Update `AppErrorToChatEventTest` to confirm Tier routing is unchanged. Tighten existing `ChatRepositoryImplTest` assertions that previously compared against singleton instances via `assertSame` (which would regress once singletons become data classes). **🟢 GREEN:** convert the six fixed-message variants from `data object` to `data class Variant(rawMessage: String = DEFAULT)`. Keep the codes unchanged. Update `AppError.fromCode(...)` to thread the supplied message through. **🔵 REFACTOR:** verify purity + full suite green.
        - **Scope note:** this is the minimal fix covering the observed /transcribe UX regression. An alternative "add new `InvalidAudioInput` variant" was considered and rejected for being less general — the backend pins specific messages on every error code, not just on BAD_REQUEST.
    - [ ] **7.2.B** Manual E2E re-run — intentionally record a zero-length / empty audio clip and confirm the Snackbar surfaces `"Audio file is empty or too short to transcribe."` verbatim. Marks 7.2 complete.
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