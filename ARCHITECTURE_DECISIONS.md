# Architecture Decisions

## AD-001 — Emulator core remains independent

Compose, accounts, catalogs, artwork, achievements, and cloud systems do not enter the native core. `EmulationCore` is the sole app-facing contract.

## AD-002 — mGBA is a build-time component

Retra uses pinned mGBA 0.10.5 through the libretro ABI. Native executable code is compiled and bundled; it is never downloaded at runtime. Missing symbols produce an explicit diagnostic fallback.

## AD-003 — Google identity uses Credential Manager

The client requests Google identity with a nonce, parses only the official credential type, stores no raw ID token, and grants no cloud trust without backend verification.

## AD-004 — Offline play is unconditional

Onboarding and account screens always offer offline use. Authentication cannot block local ROMs, saves, patches, cheats, achievements, or settings.

## AD-005 — Rewind is bounded and non-persistent

Native state snapshots are copied into a thread-safe 32 MiB ring. They are cleared on load/reset/stop and never replace battery saves or Vault states.

## AD-006 — Video uses SurfaceView

The frame presenter reuses one mutable bitmap and supports aspect fit, integer scaling, and optional filtering without allocating a Compose bitmap each frame.

## AD-007 — Custom artwork is app-private metadata

Imported artwork is bounded, decoded, downsampled, re-encoded, atomically committed, and stored outside the ROM. Title/notes/cover changes never alter ROM bytes or identity.

## AD-008 — Automatic Android backup is disabled

The manifest disables platform auto-backup so ROMs, saves, identity metadata, and private catalog content are not silently uploaded. Future cloud backup must be explicit and save-focused.

## AD-009 — Input capture is contextual

Game controls consume controller/keyboard events only during gameplay or the explicit tester. Elsewhere, D-pad and controller events remain available to Android/Compose focus navigation.

## AD-010 — Visual design is token-driven

The original Retra Prism mark and Material 3 tokens drive onboarding, navigation, library, player, and profile. Glass and gradients are reserved for narrative anchors; dense technical screens remain opaque and readable.

## AD-011 — Glass is a surface system, not a blur effect

Content remains crisp. Blur is limited to decorative background atmosphere, while glass panels use translucent color, subtle edge highlights, and opaque accessibility fallbacks.

## AD-012 — Feedback is semantic and user-controlled

A central engine maps event meaning to short audio and haptic cues. Game buttons avoid interface sounds, frequent actions use light haptics, and users can independently disable haptics, interface sounds, or notifications.

## AD-013 — Notification permission is contextual

Channels are created at app startup, but Android 13+ runtime permission is requested only from the notification settings surface. No emulator capability depends on permission approval.

## AD-014 — Settings disclose one category at a time

The profile remains visible, while preferences are partitioned into focused categories to reduce scanning cost and accidental changes.

