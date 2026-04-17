---
name: m3-component-build
description: Ensure UI components adhere to Material 3 standards and the project's DESIGN_SYSTEM.md. Use when creating Composables or UI screens.
---

# M3 Component Build

## When to use this skill
Use this skill when implementing any Jetpack Compose UI element, from small components (buttons, text fields) to full screen layouts.

## The UI Construction Protocol

### 🎨 PHASE 1: Design Alignment
1. **Token Check:** Refer to `DESIGN_SYSTEM.md`. Use designated theme tokens (e.g., `MaterialTheme.colorScheme.primary`) instead of hardcoded hex colors.
2. **Typography:** Use the project's defined typography scale (e.g., `MaterialTheme.typography.bodyLarge`).
3. **Spacing:** Adhere to the 8dp grid system defined in the design system.

### 🏗️ PHASE 2: Component Implementation
1. **Statelessness:** Prefer "Stateless" composables. Pass state in and events out (State Hoisting).
2. **Previewing:** Create a `@Preview` for multiple states:
    - Light Mode / Dark Mode.
    - Different screen sizes (Compact, Medium, Expanded).
    - Error states / Loading states.
3. **Accessibility:** Ensure `contentDescription` is provided for all icons and images.

### ✅ PHASE 3: Interaction & Polish
1. **Haptics:** Add `LocalHapticFeedback.current` for critical interactions.
2. **Animation:** Use `AnimatedVisibility` or `animateColorAsState` for smooth transitions.
3. **Review:** Compare the resulting UI against the "vibe" and the specified design constraints.

## Verification Checklist
- [ ] Does it use `MaterialTheme` tokens instead of hardcoded values?
- [ ] Is the component stateless (State Hoisting)?
- [ ] Are there previews for both Light and Dark modes?
- [ ] Does it follow the 8dp spacing grid?
- [ ] Is it accessible (Content Descriptions)?
"