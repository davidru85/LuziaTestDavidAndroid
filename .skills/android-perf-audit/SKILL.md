---
name: android-perf-audit
description: Optimize Jetpack Compose performance and memory management. Use before finalizing a feature or when noticing UI jank.
---

# Android Performance Audit

## When to use this skill
Use this skill during the REFACTOR phase of TDD or before submitting a feature for final review to ensure production-grade performance.

## The Optimization Protocol

### ⚡ PHASE 1: Compose Recomposition Audit
1. **Stability Check:** Ensure that data classes passed to Composables are `@Immutable` or `@Stable` to avoid unnecessary recompositions.
2. **Lambda Stability:** Use `remember` for lambdas or method references to prevent the Composable from thinking the callback has changed.
3. **Heavy Computation:** Move any non-UI logic (sorting, filtering) out of the Composable and into the `ViewModel` using `StateFlow`.
4. **Derived State:** Use `derivedStateOf` when a state depends on another state that changes more frequently than the UI needs to update.

### 🧠 PHASE 2: Memory & Lifecycle Audit
1. **Coroutine Scopes:** Verify that all async work in the `ViewModel` uses `viewModelScope` to prevent memory leaks when the ViewModel is cleared.
2. **Flow Collection:** Ensure `StateFlow` is collected using `collectAsStateWithLifecycle()` in the UI to stop collection when the app is in the background.
3. **Resource Leaks:** Check that any listeners, observers, or timers are properly disposed of in `onCleared()` or `DisposableEffect`.

### 📦 PHASE 3: Resource Efficiency
1. **Image Optimization:** Ensure images are sized correctly and not loading 4K assets into small thumbnails.
2. **Avoid Over-draw:** Review the layout hierarchy to eliminate unnecessary wrapping layouts (e.g., removing a `Box` that doesn't provide any alignment or styling).

## Verification Checklist
- [ ] Are all state-driven UI updates using `collectAsStateWithLifecycle`?
- [ ] Have I eliminated unnecessary recompositions using `remember` or `derivedStateOf`?
- [ ] Are all coroutines tied to the correct lifecycle scope?
- [ ] Are there any heavy computations happening directly inside a Composable function?