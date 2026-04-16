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

- [ ] 1.1 **Dependencies:** Update `libs.versions.toml` (Hilt, K10, Ktor, Room, kotlinx.serialization, Testing libs).
- [ ] 1.2 **Plugins:** Register all required Gradle plugins (`kotlin.serialization`, `hilt.android`, `ksp`).
- [ ] 1.3 **App Entry:** Create `LuziaApp.kt` (`@HiltAndroidApp`) and `MainActivity.kt` (`@AndroidEntryPoint`).
- [ ] 1.4 **Permissions:** Declare `RECORD_AUDIO` and `INTERNET` in `AndroidManifest.xml`.
- [ ] 1.5 **Config:** Set up `BuildConfig.BASE_URL` via `local.properties`.
- [ ] 1.6 **DI Skeleton:** Create placeholder Hilt modules (`NetworkModule`, `DatabaseModule`, `RepositoryModule`).
- [ ] 1.7 **Build Variants:** Configure `staging` and `production` flavors.
- [ ] 1.8 **Verification:** Execute `./gradlew assembleStagingDebug` $\rightarrow$ **Result: SUCCESS**.

### Phase 2: CI/CD Automation
*Goal: Automated validation of every commit.*

- [ ] 2.1 **Workflow:** Create `.github/workflows/android-ci.yml`.
- [ ] 2.2 **Pipeline:** Implement steps: `Lint` $\rightarrow$ `Unit Tests` $\rightarrow$ `Build`.

### Phase 3: Domain Layer (TDD)
*Goal: Implement the pure Kotlin business logic.*

- [ ] **3.1 RED (Tests):** Write failing tests for `ChatMessage` model, `ChatRepository` interface, `TranscribeAudioUseCase`, and `SendMessageUseCase`.
- [ ] **3.2 GREEN (Implementation):** Implement `ChatMessage`, `MessageRole`, `ChatRepository` (interface), and all `UseCases`.
- [ ] **3.3 REFACTOR:** Ensure zero Android imports in `domain/` and verify `Resource<T>` utility.

### Phase 4: Data Layer (TDD)
*Goal: Implement the technical implementation of the domain interfaces.*

- [ ] **4.1 RED (Tests):** Write failing tests for `SseParser`, `ChatMapper`, `L1ApiClient` (using `MockEngine`), and `ChatRepositoryImpl`.
- [ ] **4.2 GREEN (Implementation):** Implement `SseParser`, `DTOs`, `Ktor Client`, `Room` entities/DAOs, and `ChatRepositoryImpl`.
- [ ] **4.3 REFACTOR:** Finalize Hilt bindings (`@Binds`) and optimize `NetworkModule` (logging/interceptors).

### Phase 5: Presentation Layer (TDD)
*Goal: Implement the UI, ViewModels, and State Management.*

- [ ] **5.1 Theme:** Configure Material 3 `Color`, `Type`, and `Theme` (Dynamic Color support).
- [ ] **5.2 UI Models:** Implement `ChatUiState` (sealed interface) and `ChatMessageUiModel`.
- [ ] **5.3 ViewModels:** Write tests for `ChatViewModel` (State transitions: `Idle` $\rightarrow$ `Recording` $\rightarrow$ `Streaming`). Implement `ViewModel` logic.
- [ ] **5.4 Components:** Implement `MessageBubble`, `RecordButton`, and `StreamingIndicator` (test via `ComposeTestRule`).
- [ ] **5.5 Screen:** Implement `ChatScreen` (scaffold, `LazyColumn` auto-scroll, permission handling).

### Phase 6: Integration & Wiring
*Goal: Connect all layers into a functional application.*

- [ ] 6.1 **Activity Wiring:** Host `ChatScreen` in `MainActivity`.
- [ ] 6.2 **Runtime Permissions:** Implement `ActivityResultLauncher` for `RECORD_AUDIO`.
 
### Phase 7: Polish & QA
*Goal: Refine the UX and ensure robustness.*

- [ ] 7.1 **End-to-End:** Manual test: Record $\rightarrow$ Transcribe $\rightarrow$ Stream $\rightarrow$ Display.
- [ ] 7.2 **Edge Cases:** Test empty audio, network loss, and extremely long AI responses.
- [ ] 7.3 **Accessibility:** Verify `TalkBack` support and `contentDescription` updates.
- [ ] 7.4 **Cleanup:** Delete temporary `.m4a` files and `MediaRecorder` resource release.

### Phase 8: Final Audit
- [ ] 8.1 **Unit Tests:** Verify 100% coverage of `UseCases` and `Mappers`.
- [ ] 8.2 **Lint & Build:** `./gradlew lintStagingDebug` $\rightarrow$ **Result: ZERO ERRORS**.

### Phase 9: Documentation
- [ ] 9.1 **README:** Complete the professional `README.md` (Design Decisions, Tech Stack, Setup).