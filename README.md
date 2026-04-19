# Luzia Voice Assistant — Android

> High-performance voice-to-text chat client for a self-hosted AI backend. Built as a senior-level Android engineering showcase: Clean Architecture, strict TDD, MVVM + Unidirectional Data Flow, Material 3 Expressive.

---

## Table of Contents

1. [Overview](#overview)
2. [The Golden Rules](#the-golden-rules)
3. [Tech Stack](#tech-stack)
4. [Architecture](#architecture)
5. [Features Delivered](#features-delivered)
6. [Project Structure](#project-structure)
7. [Setup](#setup)
8. [Build Variants](#build-variants)
9. [Testing](#testing)
10. [CI / CD](#ci--cd)
11. [Design Decisions](#design-decisions)
12. [Future Improvements / Next Steps](#future-improvements--next-steps)
13. [Reference Documentation](#reference-documentation)

---

## Overview

Luzia is a voice-first chat client. The user taps the microphone, the raw audio is captured and uploaded to a local AI backend which transcribes via Whisper, forwards the conversation to an LLM, and streams the reply back token-by-token over Server-Sent Events. The Android client renders the streaming reply as text, with an optional on-demand "read aloud" affordance using the Android system TTS engine.

The project is deliberately scoped as an engineering showcase rather than a shipping consumer app. Every architectural choice optimises for testability, layer purity, and contract-first coordination with the backend team — trade-offs that show up in the **497-test unit suite**, the `DomainPurityTest`-enforced layer separation, and the versioned [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) that sits between the two teams as the source of truth.

Status: **Phases 1 through 10 complete** (chat + voice + streaming + personas + TTS + i18n + 3-tier error strategy + R8 + baseline profile + Hilt test graph). Phase 11 (this README + future-improvements backlog) is the last remaining item. See [ROADMAP.md](ROADMAP.md) for phase-by-phase history.

---

## The Golden Rules

Six non-negotiable invariants, enforced by the code and tests:

1. **Zero local STT / ASR for user input.** Audio is captured raw and uploaded; transcription is performed server-side (Whisper). On the output side, the Android system `TextToSpeech` engine **may** be invoked on user tap to read an assistant reply aloud — no third-party engines, no autoplay.
2. **Zero local audio processing.** The client is a capture-and-transmit agent. No noise reduction, no re-encoding. The recorder writes `.m4a` / AAC straight from `MediaRecorder` and sends the bytes as-is.
3. **Domain purity.** The `domain/` layer has **zero** Android or AndroidX dependencies. Pure Kotlin / JVM, 100 % unit-testable. Enforced by [`DomainPurityTest`](app/src/test/java/com/ruizurraca/luziatestdavid/architecture/DomainPurityTest.kt).
4. **No data leakage.** `presentation/` never imports from `data/`. Layers talk only through `domain/` interfaces. Enforced by `PresentationPurityTest`.
5. **Strict TDD.** Red → Green → Refactor. No production code ships without a preceding failing test.
6. **Text-first UI.** The message list is text-only. No waveforms, no playback controls for recorded user audio. The only output-side audio affordance is a single TTS icon button on the **latest** received assistant reply.

Full context and historical narrowing (Fork 6, which scoped the TTS carve-out) in [CONTEXT.md](CONTEXT.md) and [MEMORY.md](MEMORY.md).

---

## Tech Stack

| Area | Choice | Version |
| :--- | :--- | :--- |
| Language | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 Expressive | Compose BOM 2026.02.01 |
| Architecture | MVVM + Clean Architecture (3 layers, unidirectional) | — |
| DI | Hilt | 2.59.2 |
| Async | Coroutines + Flow | 1.10.2 |
| Networking | Ktor Client (OkHttp engine) | 3.3.0 |
| Serialization | kotlinx.serialization (JSON, `explicitNulls = false`) | 1.9.0 |
| Persistence | Room | 2.8.4 |
| Build | Android Gradle Plugin + KSP | AGP 9.1.1 / KSP 2.2.10-2.0.2 |
| JDK | 17 (source, target, Gradle daemon) | — |
| Min / Target SDK | 24 / 36 | — |
| Testing | JUnit 5 (unit) + JUnit 4 Vintage (Robolectric Compose) + MockK + Turbine + Ktor MockEngine + Hilt test graph | — |
| Coverage | Jacoco (AGP-native `enableUnitTestCoverage`) | 0.8.12 |
| Performance | Baseline profile (producer module) | androidx.benchmark 1.5.0-alpha05 |

Version catalog: [gradle/libs.versions.toml](gradle/libs.versions.toml). Compatibility pins and their rationale are documented in the `agp9_compatibility_pins` auto-memory.

---

## Architecture

Clean Architecture with strict unidirectional data flow:

```
UI Event → ViewModel → UseCase → Repository → DataSource (Remote / Local)
```

### Import guard (non-negotiable)

| Package | Forbidden imports | Reason |
| :--- | :--- | :--- |
| `domain/` | `android.*`, `androidx.*`, `...data.*` | Pure-JVM unit testing |
| `data/` | `...presentation.*` | UI-agnostic |
| `presentation/` | `...data.*` | Talks only to `domain/` interfaces |

Violations fail CI via the purity tests. Full detail in [TECHNICAL_SPEC.md §Architecture](TECHNICAL_SPEC.md).

### Threading model

All `Dispatchers` switching happens in the repository / data-source layer via `flowOn()`. Dispatchers are Hilt-injected (`@IoDispatcher`, `@MainDispatcher`) so tests can swap in `UnconfinedTestDispatcher` deterministically.

- `Dispatchers.IO` — network, Room, SSE parsing.
- `Dispatchers.Main` — `MediaRecorder` lifecycle, UI state mutations.
- `Dispatchers.Default` — heavy data processing (if any).

**No `Thread.sleep()` in any test.** All async tests use `runTest` + `StandardTestDispatcher`.

### Error strategy (3 tiers)

| Tier | Code symbol | Mechanism | Trigger |
| :--- | :--- | :--- | :--- |
| **Tier 1** | `ChatEvent.TransientSnackbar` | Snackbar, auto-dismissing | Validation / format (VALIDATION_ERROR, BAD_REQUEST, FILE_TOO_LARGE, local recorder failures, TTS unavailable) |
| **Tier 2** | rides on `MessageStatus.FAILED` | Inline bubble + retry button beneath | Connectivity / network mid-stream |
| **Tier 3** | `ChatEvent.BlockingErrorDialog` | AlertDialog, modal | Critical / server errors (INTERNAL_ERROR, SERVICE_UNAVAILABLE) |

`AppError` sealed hierarchy in `domain/`, classification by `ErrorMapper` in `data/`, routing via `AppError.toChatEvent()` in `presentation/`. Backend-supplied error messages are preserved verbatim through the stack. Full table in [TECHNICAL_SPEC.md §Error Handling](TECHNICAL_SPEC.md).

---

## Features Delivered

Phases 1–10 complete. Notable capabilities:

- **Voice-to-text chat.** Tap to record → Whisper transcription round-tripped through `/transcribe` → transcript displayed as a user message → permission handling + rationale dialog.
- **Streaming AI replies.** SSE over `/chat` parsed line-by-line; UI transitions LOADING (shimmer) → STREAMING (token fade-in + LiveRegion.Polite) → RECEIVED.
- **Per-message persona capture.** Three personas (Student / Scientist / Artist) selectable via a top-bar `FilterChip` strip with persona-specific icons and bounce animation on selection. The persona active at send time is captured per user message and forwarded as `role_prompt` on the wire (Fork 4 semantics).
- **On-demand TTS** (Phase 10.6.D). "Read aloud" icon on the latest RECEIVED assistant reply only. Tap speaks, retap stops, switching messages switches playback, playback auto-clears on natural completion. Locale-aware via `LocalConfiguration.current.locales[0]`.
- **3-Tier error strategy.** `AppError` sealed domain model, `ErrorMapper` envelope-parser, backend-supplied messages preserved verbatim. `BlockingErrorDialog` reuses a shared `LuziaAlertDialog` composable with icon slot + severity tint + collapsible details.
- **Retry on failed assistant replies.** Dedicated `RetryAssistantReplyButton` beneath the latest FAILED bubble (relocated out of the bubble in Phase 10.6.A). Per-position friendly copy — *"I've gone blank. Mind retrying?"* for the retryable latest failure, *"Sorry, empty message"* for older non-retryable empties.
- **Internationalisation.** Three fully-translated locales (en / es-ES / pt-PT), including `role_prompts`, with every user-facing string pinned byte-for-byte via locale-specific tests. Optional `lang` field on both POST endpoints (Phase 10.6.H, API_SPEC v1.4.0) forwards the device locale to the backend so users on locales the app itself doesn't translate can still get replies in their preferred language.
- **Accessibility.** `LiveRegion.Polite` on streaming / loading assistant states (historical messages deliberately not re-announced on scroll), `Role.RadioButton` on persona chips, content descriptions on every interactive element, TalkBack-verified on device.
- **Material 3 Expressive.** Dynamic colour on Android 12+, adaptive launcher icon with themed-icons monochrome layer for Android 13+, expressive AlertDialog redesign with icon slots + severity tints, motion-driven persona-chip selection.
- **R8 + resource shrinking on release.** 21.6 MB → 1.79 MB (~92 % reduction). kotlinx.serialization manual keep rules for reflection-only `@Serializable` DTOs.
- **Baseline profile** covering cold start → Hilt graph → ChatScreen render → persona rotation (Phase 10.3.B). Producer module under [`baseline-profile/`](baseline-profile/), consumed by `:app`.
- **Jacoco coverage tooling** (Phase 10.2.A). Business-logic packages at 96–100 % line coverage; report under `app/build/reports/jacoco/`.
- **Activity-level smoke test** (`MainActivitySmokeTest`) using `@HiltAndroidTest` + `ActivityScenario` + a `TestAppModule` that substitutes `NetworkModule` (MockEngine), `DatabaseModule` (in-memory Room), and `AudioModule` (fake recorder + speaker).

---

## Project Structure

```
com.ruizurraca.luziatestdavid/
├── domain/                  # Pure Kotlin / JVM — no Android imports
│   ├── audio/               # AudioRecorder + TextSpeaker contracts
│   ├── catalog/             # PersonaCatalog contract
│   ├── common/              # Resource<T>, AppError sealed hierarchy
│   ├── locale/              # LocaleProvider (Phase 10.6.H)
│   ├── model/               # ChatMessage, MessageRole, Persona, PersonaEntry, MessageStatus
│   ├── repository/          # ChatRepository contract
│   └── usecase/             # TranscribeAudioUseCase, StreamAssistantReplyUseCase, SendMessageUseCase
├── data/
│   ├── catalog/             # DefaultPersonaCatalog (Android-strings-backed impl)
│   ├── local/
│   │   ├── audio/           # MediaRecorderAudioRecorder + AndroidTextSpeaker adapters
│   │   ├── dao/ + entity/   # Room DAO + ChatMessageEntity
│   │   ├── locale/          # AndroidLocaleProvider (Phase 10.6.H)
│   │   └── mapper/          # ChatEntityMapper (entity ↔ domain)
│   ├── remote/
│   │   ├── api/             # L1ApiClient (Ktor transport)
│   │   ├── dto/             # ChatMessageDto, ChatRequestDto, TranscribeResponseDto, ApiErrorEnvelope
│   │   ├── mapper/          # ChatMapper (domain → wire DTO), ErrorMapper
│   │   └── sse/             # SseParser + SseEvent
│   └── repository/          # ChatRepositoryImpl (orchestrator wiring L1ApiClient + SseParser + mappers + DAO)
├── presentation/
│   ├── component/           # AssistantMessageBubble, UserMessageBubble, MessageBubble, ChatInputBar,
│   │                        # ChatTopAppBar, RoleSelectorChips, MorphingActionButton, ShimmerBox,
│   │                        # StreamingIndicator, RetryAssistantReplyButton, TtsPlayButton, LuziaAlertDialog
│   ├── model/               # ChatMessageUiModel + ChatMessageUiMapper (domain → UI)
│   ├── screen/              # ChatScreen (VM-wired) + ChatScreenContent (stateless)
│   ├── state/               # ChatUiState sealed interface, ChatEvent (TransientSnackbar / BlockingErrorDialog)
│   ├── theme/               # Color.kt, Type.kt, Theme.kt (dynamic-colour aware)
│   └── viewmodel/           # ChatViewModel
├── di/                      # Hilt modules — Network / Database / Repository / Audio / Catalog / Locale / Dispatcher
├── LuziaApp.kt              # @HiltAndroidApp
└── MainActivity.kt          # Single activity, @AndroidEntryPoint, hosts ChatScreen + enableEdgeToEdge
```

---

## Setup

### Prerequisites

- Android Studio Koala (2024.1.1) or newer, or command-line Gradle ≥ 8.11
- JDK 17 (the Gradle `foojay-resolver-convention` plugin will fetch a matching toolchain if needed)
- An AI backend reachable at the configured `BASE_URL` — see [TECHNICAL_SPEC.md §API Contracts](TECHNICAL_SPEC.md) for the wire shape

### `local.properties`

Create the file at the project root with both flavour URLs. The build fails loudly (`IllegalStateException`) if either key is missing — no silent defaults.

```properties
BASE_URL_STAGING=http://127.0.0.1:8000
BASE_URL_PRODUCTION=http://127.0.0.1:8000
```

On a physical device the backend must be reachable from the device's network. The Android emulator should use `http://10.0.2.2:8000` to reach the host machine's loopback.

### Running

```bash
# Build + install the staging-debug APK on the connected device / emulator
./gradlew :app:installStagingDebug

# Run the full unit-test suite (~497 tests, ~30 s)
./gradlew :app:testStagingDebugUnitTest

# Lint (filters the AGP-9 compatibility baseline)
./gradlew :app:lintStagingDebug

# Coverage report → app/build/reports/jacoco/jacocoStagingDebugCoverageReport/html/
./gradlew :app:jacocoStagingDebugCoverageReport

# Release build with R8 + resource shrinking
./gradlew :app:assembleStagingRelease
```

---

## Build Variants

Two product flavours × three build types = **six variants**.

**Flavours** (environment dimension):
- `staging` — `applicationIdSuffix = ".staging"`, installable side-by-side with production.
- `production` — no suffix.

Both flavours currently read from the same backend URL (per [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) — the Android-side flavour split is an engineering organisation concern, not a distinct deployment target).

**Build types:**
- `debug` — standard dev build. `enableUnitTestCoverage = true` turns on Jacoco instrumentation.
- `release` — R8 + resource shrinking. Requires a release signing config for external distribution; see [ROADMAP.md §Phase 11](ROADMAP.md) "Release-APK install validation" for the remaining gap.
- `benchmark` — mirrors `release` (R8 + resource-shrink) but uses the debug signing config so the APK is installable, and is marked `isDebuggable = false` so ART can AOT-optimise the baseline profile. Used exclusively by the `:baseline-profile` producer module.

---

## Testing

The test suite is the project's first-class artefact. 497 unit tests, all running on the JVM, all green.

### Unit tests — JUnit 5 + MockK + Turbine

```bash
./gradlew :app:testStagingDebugUnitTest
```

Covers:
- Pure-JVM domain (use cases, mappers, models, `AppError` variants)
- Repository / mapper integration using `Ktor MockEngine` + in-memory Room
- Compose leaf tests (`createComposeRule` + Robolectric via JUnit 4 Vintage engine)
- Stateless scaffold tests for `ChatScreenContent`
- Locale-pinning tests (`A11yStringMigrationTest` / `EsTest` / `PtTest`) — every translatable string pinned byte-for-byte
- Activity-level smoke test (`MainActivitySmokeTest`) via `@HiltAndroidTest` + `ActivityScenario`

### Architectural tests

`DomainPurityTest` + `PresentationPurityTest` scan source imports at test time and fail if the import guard is violated. Run as part of the unit-test task — no separate invocation needed.

### Coverage

Jacoco wired via AGP-native `enableUnitTestCoverage = true` (Phase 10.2.A). The post-ASM bytecode path on AGP 9 is `intermediates/classes/<variant>/transformClassesWithAsm/dirs`.

- Business-logic packages (`domain/`, `data/` mappers + use cases + repository) land at 96–100 % line coverage.
- Compose UI packages show 0 % because Jacoco cannot trace through Compose lambda indirection — the UI IS exercised by tests, but the measurement is blind to it.

No CI coverage gate yet (deliberately deferred until the baseline stabilises). See [ROADMAP.md §Phase 11](ROADMAP.md) "Jacoco CI coverage gate."

### Baseline profile

Producer module under [`baseline-profile/`](baseline-profile/). Generated profile at `app/src/stagingRelease/generated/baselineProfiles/baseline-prof.txt` (19,774 entries, 692 Luzia-specific lines). Covers cold start → Hilt graph → ChatScreen render → persona rotation. Extension to cover mic → transcribe → SSE → render is a Phase 11 follow-up (backend wasn't reachable during the initial profile collection).

### Conventions and protocols

- **No `Thread.sleep()`.** Use `runTest` + `StandardTestDispatcher` and Hilt-injected dispatchers.
- **Full-suite runs after every GREEN transition.** Targeted `--tests` subsets hide regressions in distant locale-pinning or sealed-hierarchy tests. Captured as auto-memory `run_full_suite_after_green`.
- **Device verification required for Compose UI / layout tasks.** Robolectric's layout pass does not faithfully simulate short-list anchoring, IME behaviour, parent-container clamping, or placeholder wrapping. Captured as auto-memory `ui_tasks_device_verification` with the anti-patterns that surfaced in Phases 10.6.B / 10.6.C.

Full protocol in [TECHNICAL_SPEC.md §Testing Protocol](TECHNICAL_SPEC.md).

---

## CI / CD

GitHub Actions workflow at [.github/workflows/android-ci.yml](.github/workflows/android-ci.yml).

- **Triggers:** `push` to `main`, `pull_request` against `main`.
- **Runner:** `ubuntu-latest` + JDK 17 (Temurin) + `gradle/actions/setup-gradle@v4`.
- **Pipeline:** `Lint → Unit Tests → Build` (`lintStagingDebug` → `testStagingDebugUnitTest` → `assembleStagingDebug`).
- **Artefacts:** lint report + unit-test report uploaded on every run (including failures) for inspection.
- **Concurrency:** per-branch cancel-in-progress so stale runs don't pile up on force-pushes.

Known transient failure mode: Maven Central occasionally 403s GitHub's shared runner IPs. Surfaces as a config-cache error because `jacocoAgent` resolution fails before Gradle can serialise the test task. Re-running the job clears it. Captured during the Phase 10.6.H backend-coordinated commit push; see session notes if it recurs.

---

## Design Decisions

Durable architectural decisions are logged in [MEMORY.md](MEMORY.md) as numbered **Forks**. Read the relevant Fork before re-opening a topic — each Fork records the options considered, what was chosen, and what survives or gets superseded.

| Fork | Topic | Phase |
| :--- | :--- | :--- |
| 1 | Per-message persona capture (no `system` turn injected by the client) | 5.5 |
| 2 | Tier-2 inline retry (`onRetryLastFailure` + dedicated button) | 5.5 |
| 3 | Permission handling merged into `ChatScreen` (absorbs Phase 6) | 5.5 |
| 4 | Revised `/chat` wire contract — three-field `role` / `role_prompt` / `content` (API_SPEC v1.1.0) | 7.1 |
| 5 | `/transcribe` 400 for empty / too-short audio with verbatim message (API_SPEC v1.2.0) | 7.2 |
| 6 | Golden-Rule narrowing — output-side TTS carve-out on Rules #1 and #6 | 10 |
| 7 | Optional `lang` field on `/chat` + `/transcribe` (API_SPEC v1.4.0) | 10.6.H |

Operational rules (auto-committed to an agent-memory system) include: contract-first when coordinating with the backend team, 3-commit TDD split (test / feat / docs) per sub-task, device verification mandatory for layout-sensitive UI work, run the full test suite after every GREEN transition. These are not repo documentation but they shape how the code evolved.

---

## Future Improvements / Next Steps

Curated backlog of items either deliberately deferred during Phases 5–10 or surfaced as senior-level gaps during the build-out. Full detail with code references, rationale, and suggested implementation hints in [ROADMAP.md §Phase 11](ROADMAP.md).

**Visual / UX**
- Markdown rendering in assistant message bubbles
- Streaming auto-scroll at token granularity (current scroll fires on message count, not per-token content growth)
- Copy-to-clipboard affordance on assistant replies

**Architecture / engineering**
- **Room 2.x → Room 3.x migration.** Switch `androidx.room` → `androidx.room3` once 3.x reaches stable. Coroutine-first APIs, new `SQLiteDriver`, KMP support, `@DaoReturnTypeConverters`. Deferred from Phase 4.3 because 3.x was alpha.
- Better multilanguage support (more locales, regional variants, in-app language override, RTL audit, `<plurals>`).
- Personas / roles managed from backend (`GET /personas`) instead of client-side string arrays.
- Instrumented test suite (`app/src/androidTest/`) on real device / emulator.
- Screenshot tests (Paparazzi or Roborazzi) for theme regression coverage.
- Release-APK install validation (R8 is enabled but the release APK has never been installed on a device).

**Observability & production-readiness**
- Crash reporting (Firebase Crashlytics / Sentry) with PII redaction.
- SSE stream watchdog — `withTimeout` around token reads so the client doesn't hang if the backend never emits `[DONE]`.
- Ktor logging redaction review for debug builds.

**Content & moderation**
- Prohibited-terms / profanity filtering — backend-authoritative, locale-aware, two-layer (user input + assistant output).

**Testing & tooling**
- Jacoco CI coverage gate.
- Baseline profile extension covering mic → `/transcribe` → SSE → render.
- Dependabot / Renovate for automated dependency updates.
- Pre-upload audio duration guard (client-side short-reject before multipart upload).

**Minor cleanup**
- `cd_retry_reply` string-key rename (key has `cd_` prefix but is used as a button label, not contentDescription).
- ROADMAP editorial collapse of §10.6 sub-tasks into the parent summary.

---

## Reference Documentation

Layered docs, each with a distinct purpose. Start here, follow the links down.

| File | Purpose |
| :--- | :--- |
| **[CONTEXT.md](CONTEXT.md)** | Project identity, Golden Rules, tech-stack summary, reference map. The "what and why at 30,000 feet." |
| **[TECHNICAL_SPEC.md](TECHNICAL_SPEC.md)** | API contracts (wire-level, byte-pinned error strings), threading model, 3-tier error strategy, import guard, testing protocol. The contract between the Android team and the backend team. |
| **[DESIGN_SYSTEM.md](DESIGN_SYSTEM.md)** | Screen architecture, component specs, interaction & motion, A11y checklist. The contract between the Android team and the UX. |
| **[ROADMAP.md](ROADMAP.md)** | Phase-by-phase execution plan with per-sub-task completion notes. The project's engineering history. |
| **[MEMORY.md](MEMORY.md)** | Decision log — the *why* behind non-obvious choices. Numbered Forks, appended as they settle. Read before re-opening a settled question. |
| **[AGENTS.md](AGENTS.md)** | Operational protocol for AI-agent-assisted development on this repo. Relevant if the repo is picked up by an AI-augmented engineering workflow. |

---

*Last updated as part of Phase 11 (2026-04). For any sub-task deeper than "which file renders the send button" the five markdowns above are more authoritative than this README — they describe the system in the terms the engineers were thinking in as they built it.*
