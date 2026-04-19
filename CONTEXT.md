# File: CONTEXT.md

## 🧠 Project Identity
**Project Name:** Luzia Voice Assistant (Android)
**Purpose:** A high-performance, AI-powered voice-to-text chat application. The app enables users to interact via voice (as a UX convenience) and receive streamed, real-ai-generated text responses.

## 🎯 Core Mission & Principle
To demonstrate senior-level Android engineering through a seamless, real-time integration between a mobile client and a cloud-based AI backend, strictly adhering to **Clean Architecture**, **TDD (Test-Driven Development)**, and **Unidirectional Data Flow (UDF)**.

## 🚫 The "Golden Rules" (Non-Negotiable)
These rules are absolute. Any violation is a failure of the implementation.

1.  **Zero Local STT/ASR for user input:** The Android client **MUST NOT** perform any local speech-to-text or automatic speech recognition. User audio is captured raw and transmitted to the backend; transcription is performed server-side (Whisper). On the **output** side, the Android system TTS (`android.speech.tts.TextToSpeech`) MAY be invoked on-demand by the user to read an assistant reply out loud. No third-party TTS engines; no automatic / background playback.
2.  **Zero Local Audio Processing:** The client is a "capture and transmit" agent. No noise reduction, no re-encoding. Send the raw `.m4a`.
3.  **Domain Purity:** The `domain/` layer must have **ZERO** Android dependencies. It must be pure Kotlin, making it 100% unit-testable on the JVM.
4.  **No Data Leakage:** The `presentation/` layer must never import from `data/`. It only interacts with the `domain/` layer via interfaces.
5.  **Strict TDD:** No production code shall be written without a preceding failing test (**RED $\rightarrow$ GREEN $\rightarrow$ REFACTOR**).
6.  **Text-first UI:** The chat message list renders messages as text only — no audio players, waveforms, or playback controls for recorded user audio, either inbound or outbound. The sole permitted output-side audio affordance is a TTS control (single icon button) that invokes the Android system `TextToSpeech` on a rendered assistant message's text.

## 🛠 Tech Stack Summary
*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material 3 Expressive)
*   **Architecture:** MVVM + Clean Architecture
*   **Asynchrony:** Coroutines + Flow (for SSE streaming)
*   **Dependency Injection:** Hilt
*   **Networking:** Ktor Client (with OkHttp engine)
*   **Database:** Room (for conversation persistence)
*   **Backend Interface:** REST (Multipart/form-data) & SSE (Server-Sent Events)

## 🗺 Reference Map
This file is the entry point. For detailed instructions, refer to:
*   **[TECHNICAL_SPEC.md](./TECHNICAL_SPEC.md):** API contracts, threading models, error tiers, and technical implementation details.
*   **[DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md):** UI/UX specifications, Material 3 guidelines, and visual states.
*   **[ROADMAP.md](./ROADMAP.md):** The step-by-step implementation plan and current progress.
*   **[AGENTS.md](./AGENTS.md):** The protocol for how the AI Agent should interact, communicate, and execute tasks.