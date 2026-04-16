# File: CONTEXT.md

## 🧠 Project Identity
**Project Name:** Luzia Voice Assistant (Android)
**Purpose:** A high-performance, AI-powered voice-to-text chat application. The app enables users to interact via voice (as a UX convenience) and receive streamed, real-ai-generated text responses.

## 🎯 Core Mission & Principle
To demonstrate senior-level Android engineering through a seamless, real-time integration between a mobile client and a cloud-based AI backend, strictly adhering to **Clean Architecture**, **TDD (Test-Driven Development)**, and **Unidirectional Data Flow (UDF)**.

## 🚫 The "Golden Rules" (Non-Negotiable)
These rules are absolute. Any violation is a failure of the implementation.

1.  **Zero Local ASR/TTS:** The Android client **MUST NOT** perform any local speech-to-text or text-to-speech. All transcription happens on the backend (Whisper). All communication is text-based.
2.  **Zero Local Audio Processing:** The client is a "capture and transmit" agent. No noise reduction, no re-encoding. Send the raw `.m4a`.
3.  **Domain Purity:** The `domain/` layer must have **ZERO** Android dependencies. It must be pure Kotlin, making it 100% unit-testable on the JVM.
4.  **No Data Leakage:** The `presentation/` layer must never import from `data/`. It only interacts with the `domain/` layer via interfaces.
5.  **Strict TDD:** No production code shall be written without a preceding failing test (**RED $\rightarrow$ GREEN $\rightarrow$ REFACTOR**).
6.  **Text-Only UI:** The chat interface is strictly text-based. No audio players, waveforms, or playback controls are permitted in the message list.

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