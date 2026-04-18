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
- [x] **7.2 Edge Cases** — Manual edge-case audit (2026-04-18) + contract alignment with backend API_SPEC v1.2.0 (Fork 5). Phase started as "test empty audio / network loss / long responses", surfaced one latent client bug (`AppError` variants discarded backend-supplied messages), drove a symmetric fix across six variants, and closed with manual device verification. Sub-tasks:
    - **Manual findings:**
        - *Empty / too-short audio* → backend `400 BAD_REQUEST` with verbatim `"Audio file is empty or too short to transcribe."` (Fork 5). Short-but-above-threshold clips pass through to Whisper and return `200 OK { "text": "you" }` (Whisper near-silence hallucination) — indistinguishable from a valid transcription client-side; surface as-is.
        - *Network loss mid-stream* → Tier-1 Snackbar `"Network connection failed."` — working since Phase 7.0, no client change needed.
        - *Long AI responses* → backend caps replies at 1024 tokens/message; client imposes no additional limits. SSE pipeline handles the cap comfortably.
    - **7.2.A** Backend-message preservation across the six fixed-message `AppError` variants (Option B, symmetric) — shipped. `BadRequest` / `FileTooLarge` / `Timeout` / `Network` / `ServiceUnavailable` / `Internal` converted from `data object` singletons to `data class Variant(rawMessage: String = DEFAULT)`; `AppError.fromCode(code, message)` threads backend messages through verbatim on every known code. `ChatEvent.toChatEvent()` `when` branches moved to `is` patterns; `ErrorMapper` singleton refs replaced with no-arg constructors. Six test files swept (`assertSame` → `assertEquals`), plus 12 new preserved-message tests.
    - **7.2.B** Manual E2E on device confirmed: zero-byte / too-short audio surfaces an auto-dismissing Snackbar with the verbatim backend message `"Audio file is empty or too short to transcribe."` — not a Tier-3 AlertDialog, no auto-retry. All four success criteria passed.
    - Final state: **262 unit tests green**, domain + presentation purity green, `assembleStagingDebug` green.
- [ ] 7.3 **Accessibility, Polish & UX** — larger scope split into three sub-tasks per the `phase7_polish_deferred` memo + user-surfaced UX items. Absorbs:
    - 7.1.3.E (Tier-3 AlertDialog UX redesign)
    - 7.1.4 Option-B *(orphan-assistant UX rework, if we decide to do it)*
    - deferred `collectAsStateWithLifecycle` swap
    - deferred `MainActivity` smoke test
    - [x] **7.3.1 Accessibility** — shipped (2026-04-18). Hardcoded English a11y / dialog strings pulled out to `res/values/strings.xml`; `LiveRegion.Polite` wired on transient assistant states; TalkBack verified on-device. Sub-tasks:
        - **7.3.1.A** `stringResource` migration — 20 entries added under `strings.xml` (11 `cd_*` contentDescriptions + 8 `dialog_*` / clear-confirm copy + `input_placeholder`). Six components (`AssistantMessageBubble`, `UserMessageBubble`, `MorphingActionButton`, `ChatInputBar`, `ChatTopAppBar`) plus `ChatScreen`'s mic-rationale and Tier-3 AlertDialogs migrated to `stringResource(...)`. New `A11yStringMigrationTest` (Robolectric) pins each string's expected English value as a regression guard and opens the door to `values-es/strings.xml` for localisation.
        - **7.3.1.B** `LiveRegion.Polite` on `LOADING` + `STREAMING` branches of `AssistantMessageBubble`. `LoadingLines` combines `contentDescription = "Loading response"` with `liveRegion = LiveRegionMode.Polite` so TalkBack announces the shimmer; `AssistantText` gains an `announceChanges: Boolean` param — `true` for `STREAMING`, `false` for `RECEIVED` so history isn't re-announced on scroll. `FAILED` deliberately silent (the icon's `contentDescription` is enough). 4 new semantics tests guard each branch.
        - **7.3.1.C** Manual TalkBack pass confirmed on device — mic flow, persona chip selection (`Role.RadioButton` semantics already present from 5.5.F), Snackbar errors, streaming reply, clear-conversation dialog, and Tier-3 AlertDialog all announce sensibly. Touch targets and contrast eyeballed — no issues flagged.
        - **Explicitly deferred to 7.3.3:** Tier-3 AlertDialog `title` strings (`"Service error"` / `"Unexpected error"`) — require re-plumbing `ChatEvent.Tier3` (move title selection from a pre-resolved `String` to a semantic kind enum resolved by the composable). Fits naturally alongside the AlertDialog UX redesign in 7.3.3.
        - Final state: **286 unit tests**, domain + presentation purity, `assembleStagingDebug` — all green.
    - [x] **7.3.2 Code Polish** — shipped (2026-04-18) as two sub-tasks; 7.3.2.B promoted out to standalone Phase 8 after a spike revealed its scope.
        - **7.3.2.A** `collectAsStateWithLifecycle` migration in `ChatScreen.kt`. New `androidx-lifecycle-runtime-compose` entry in `libs.versions.toml` (reused `lifecycleRuntimeKtx = "2.10.0"`). `viewModel.state` and `viewModel.selectedPersona` now collect via the lifecycle-aware API so emissions pause when the app backgrounds. Pure API-swap — no RED test (library owns the lifecycle-pause behaviour; existing suites guard emission shape).
        - **7.3.2.C** `lintStagingDebug`-driven polish: (1) removed redundant `android:label` from the `<activity>` element in `AndroidManifest.xml` — `<application>` already sets it (`RedundantLabel` rule); (2) reordered `MorphingActionButton` params so `modifier` precedes `enabled` per Compose guidelines (`ModifierParameter` rule); (3) deleted 7 unused template colours (`purple_200/500/700`, `teal_200/700`, `black`, `white`) from `res/values/colors.xml` (`UnusedResources` rule). Remaining 15 warnings are all deliberately-out-of-scope dependency-upgrade suggestions (`NewerVersionAvailable` / `GradleDependency` / `AndroidGradlePluginVersion`) — AGP 9 compatibility pins memory documents the rationale.
        - Final state: **286 unit tests**, lint 0 actionable warnings, `assembleStagingDebug` — all green.
    - **7.3.3 UI/UX Improvements** — six sub-tasks per user-surfaced suggestions + design decisions locked in `MEMORY.md §Phase 7.3.3 — UX Decisions Locked`. Ordering: A → B → C → D → E → F (quick wins first, i18n last so it translates the final stabilised copy).
        - [x] **7.3.3.A** Record → Stop icon swap in `MorphingActionButton` — shipped. Visible icon now conditional: `Icons.Filled.Stop` when `isRecording`, `Icons.Filled.Mic` otherwise. `contentDescription` toggle already in place since 7.3.1.A. Single `feat:` commit per the `commit_style_preference` exception for one-shot visual refactors; existing `MorphingActionButtonTest` continues to guard the semantic surface.
        - [x] **7.3.3.B** FAILED assistant bubble copy — shipped. `FailedIndicator` now renders a text beside the warning icon: `bubble_failed_latest_message` (*"I've gone blank. Mind retrying?"*) when `onRetry != null`; `bubble_failed_older_message` (*"Sorry, empty message"*) otherwise. Disambiguation uses the existing retryability signal — no domain-model change. 2 new tests in `AssistantMessageBubbleTest` guard the branch; 2 new pins in `A11yStringMigrationTest`. Strings queued for 7.3.3.F translation.
        - [ ] **7.3.3.C** Tier-3 AlertDialog — M3 Expressive redesign + Option-B copy strategy. Re-plumb `ChatEvent.Tier3.title: String` → `Tier3.kind: Tier3Kind` enum; composable resolves icon + `stringResource` title + hand-written friendly body + collapsible "Details" section carrying the backend message. Unblocks the i18n item deferred from 7.3.1.
        - [ ] **7.3.3.D** Mic permission rationale + Clear conversation confirm dialogs — M3 Expressive icons, minor copy polish, probably share a reusable `LuziaAlertDialog` wrapper with 7.3.3.C.
        - [ ] **7.3.3.E** Role selector — icons per persona + motion on selection. Keeps M3 `FilterChip` + single-select `Role.RadioButton` semantics from 5.5.F; visual layer only.
        - [ ] **7.3.3.F** i18n — neutral `values-es` + neutral `values-pt` translations of every string in `res/values/strings.xml` (including `role_names`, `role_prompts`, and new 7.3.3.B / 7.3.3.C copy). Per-locale pinning tests (`A11yStringMigrationEsTest` / `A11yStringMigrationPtTest`) using Robolectric `@Config(qualifiers = ...)`. ~40 new test assertions. **Risk flagged:** translating `role_prompts` changes the system prompt delivered to the backend LLM — if reply quality regresses for non-English users, revert that specific subset only.
- [ ] 7.4 **Cleanup:** Delete temporary `.m4a` files and `MediaRecorder` resource release.

### Phase 8: Hilt Test Infrastructure & `MainActivity` Smoke Test
*Goal: establish the project's first activity-level, DI-wired test graph, and use it to smoke-test `MainActivity` end-to-end under Robolectric.*
*Promoted from Phase 7.3.2.B after the 2026-04-18 spike — the scope justifies standalone-phase treatment rather than sitting inside a polish sub-task. Full spike rationale in the `phase7_polish_deferred` memo.*

- [ ] **8.1 Catalog & dependencies:** add `hilt-android-testing` to `libs.versions.toml`; add `testImplementation(libs.hilt.android.testing)` and `kspTest(libs.hilt.compiler)` to the app module.
- [ ] **8.2 Test app module:** create a `TestAppModule` under `app/src/test/` providing test doubles for the three production modules that can't boot under Robolectric: MockEngine-backed `HttpClient` (substitutes `NetworkModule`), in-memory `LuziaDatabase` (substitutes `DatabaseModule`), fake `AudioRecorder` (substitutes `AudioModule`). Keep `CatalogModule`, `RepositoryModule`, `DispatcherModule` intact — they have no native/system-service dependencies.
- [ ] **8.3 `MainActivity` smoke test:** `@HiltAndroidTest` + `@Config(application = HiltTestApplication::class)` + `@UninstallModules(NetworkModule::class, DatabaseModule::class, AudioModule::class)` + `HiltAndroidRule`; launch via `ActivityScenario.launch(MainActivity::class.java)`; assert `ChatScreen` renders by locating the mic button via `onNodeWithContentDescription(context.getString(R.string.cd_record_voice_message)).assertIsDisplayed()`.
- [ ] **8.4 Verification:** full unit suite + purity + `assembleStagingDebug` stay green after the new test graph lands.

### Phase 9: Final Audit
- [ ] 9.1 **Unit Tests:** Verify 100% coverage of `UseCases` and `Mappers`.
- [ ] 9.2 **Lint & Build:** `./gradlew lintStagingDebug` $\rightarrow$ **Result: ZERO ERRORS**.

### Phase 10: Documentation
- [ ] 10.1 **README:** Complete the professional `README.md` (Design Decisions, Tech Stack, Setup).
  - Include a **"Future Improvements / Next Steps"** section covering:
    - **Room 2.x → Room 3.x migration:** Switch `androidx.room` → `androidx.room3` once 3.x reaches stable. Room 3 offers coroutine-first APIs (`withWriteTransaction`), the new `SQLiteDriver` surface, Kotlin Multiplatform support, and `@DaoReturnTypeConverters`. Deferred from Phase 4.3 because 3.x is alpha as of 2026-04-17; stability outweighs the upside for this build.
    - (Additional items to capture as they arise during Phases 5–9.)