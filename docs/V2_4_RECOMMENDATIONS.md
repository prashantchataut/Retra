# Retra 2.4 Recommendations

## P0: prove reliability on hardware

1. Run scripted 30-, 60-, and 120-minute sessions on low-, mid-, and high-tier devices.
2. Exercise background/foreground, calls, audio-route changes, screen rotation, controller disconnect/reconnect, low storage, and thermal throttling.
3. Add instrumentation tests for controller profile persistence, timeline creation/restore, and per-game launch profiles.
4. Add an opt-in diagnostic export containing app/core version, device class, settings, and bounded logs—never ROM bytes or saves.

## P0: finish Save Timeline UX

- Capture a small internal PNG thumbnail asynchronously after a state is safely written.
- Add before/after restore preview and explicit current-save backup visibility.
- Add “pin checkpoint” so retention never removes selected milestones.
- Add timeline search by title, date, slot, and cheat state.

## P1: controller portability

- Versioned `.rcp` export/import through Android's Storage Access Framework.
- Import preview showing device mismatch and reassignment conflicts.
- More axis bindings, stick inversion, turbo as an explicit accessibility feature, and per-button haptic strength.
- Tested presets for Xbox, DualSense, Switch Pro, 8BitDo, and generic HID controllers without assuming identical key codes.

## P1: exact achievements

Integrate rcheevos only behind an experimental flag after exact checksum identity and memory access are validated. Keep Retra lifecycle achievements visually and logically separate. Support offline queueing, token revocation, hardcore restrictions, and unsupported-game states.

## P1: patch reproducibility

- Visual base → patch → output lineage graph.
- Export a ROM-free recipe containing base checksum, patch checksum, patch format, expected output checksum, and source attribution.
- Verify recipe compatibility before touching the base file.

## P2: platform expansion

- GB/GBC as separate pinned cores and save namespaces.
- Foldable/tablet layouts after phone player usability testing.
- Android TV only after focus navigation and controller-only onboarding pass.
- Nintendo DS and netplay remain separate architecture milestones, not checklist additions.
