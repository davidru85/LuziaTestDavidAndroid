---
name: layer-integrity-check
description: Prevent architectural drift and ensure strict unidirectional dependency flow (Presentation -> Domain -> Data). Use during refactoring or new feature implementation.
---

# Layer Integrity Check

## When to use this skill
Use this skill whenever you are adding imports to a class or moving logic between layers to ensure the architecture remains pure.

## The Integrity Audit

### 🛡️ RULE 1: Domain Purity (The Holy Grail)
- **Check:** Open any file in the `domain/` package.
- **Forbidden:** No imports from `android.*`, `androidx.*`, `kotlinx.android.*`, or the `data/` or `presentation/` layers.
- **Required:** Domain must only depend on Kotlin standard libraries and shared domain models.

### 🛡️ RULE 2: Data Layer Isolation
- **Check:** Open any file in the `data/` package.
- **Forbidden:** No imports from the `presentation/` layer.
- **Requirement:** The data layer may depend on `domain/` (to implement interfaces) and external SDKs (Retrofit, Room).

### 🛡️ RULE 3: Presentation Flow
- **Check:** Open any file in the `presentation/` package.
- **Forbidden:** No direct imports from `data/`. 
- **Requirement:** The presentation layer must only communicate with the `domain/` layer (UseCases/Repositories interfaces).

## Violation Remediation
If a violation is found:
1. **Identify the Leak:** Why is a data model being used in the UI?
2. **Create a Mapper:** Implement a `Mapper` class in the `data/` layer to convert Data entities to Domain models.
3. **Refactor:** Update the Repository to return Domain models instead of Data models.

## Verification Checklist
- [ ] Domain layer is 100% free of Android/Framework dependencies?
- [ ] Presentation layer does not import any class from the `data/` package?
- [ ] All cross-layer data transfer is handled via Mappers?