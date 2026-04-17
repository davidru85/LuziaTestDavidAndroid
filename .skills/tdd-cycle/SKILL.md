---
name: tdd-cycle
description: Enforce the RED -> GREEN -> REFACTOR loop for all logic and feature development. Use when implementing any new functionality or fixing bugs.
---

# TDD Cycle (Test-Driven Development)

## When to use this skill
Use this skill whenever you are tasked with implementing a new feature, a UseCase, a Mapper, or fixing a bug in the production code.

## The Execution Protocol

### 🔴 PHASE 1: RED (Define the Requirement)
1. **Identify the Target:** Determine exactly which class/method needs the new behavior.
2. **Write the Test:** Create a test case that asserts the expected outcome.
3. **Verify Failure:** Run the test (`./gradlew test...`). The test **must fail** (or not compile) because the implementation does not yet exist.
4. **Document Failure:** Explicitly state *why* it failed.

### 🟢 PHASE 2: GREEN (Minimum Viable Implementation)
1. **Implement the Minimum:** Write the absolute minimum amount of code required to make the test pass. 
2. **Avoid Over-Engineering:** Do not add "just in case" logic. 
3. **Verify Success:** Run the test again. The test **must pass**.

### 🔵 PHASE 3: REFACTOR (Clean & Align)
1. **Clean Code:** Improve naming, remove redundancy, and optimize the implementation.
2. **Architectural Alignment:** Ensure the code obeys the `TECHNICAL_SPEC.md` (e.g., no Android imports in Domain).
3. **Regression Check:** Run all tests in the module to ensure no existing functionality was broken.
4. **Final Pass:** The test remains green.

## Verification Checklist
- [ ] Did I start with a failing test?
- [ ] Is the production code the minimum required to pass?
- [ ] Is the code cleaned and refactored?
- [ ] Are all existing tests still passing?