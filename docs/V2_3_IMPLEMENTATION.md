# Retra 2.3 Implementation Status

## Completed from the 2.3 recommendations

### Controller Studio

- Reads Android hardware keys, D-pad, joystick, hat, and analog trigger axes.
- Persists mappings by stable device descriptor.
- Supports optional per-game override profiles.
- Provides remapping capture, live input testing, reset, analog dead-zone, and trigger-threshold controls.
- Uses atomic bounded profile files (`RETRA-CONTROLLER-1`).
- Separates touch, touch-axis, hardware-key, and hardware-axis states before merging input.

### Save Timeline

- Creates named and automatic immutable save-state checkpoints.
- Stores exact game SHA-256, save kind/slot, core/version, timestamp, size, cheat state, and automatic/manual origin.
- Retains bounded automatic and named histories.
- Validates the save envelope before creation and restore.
- Restores through `AtomicSaveStore`, rotating the current save first so rollback is reversible.

### Measured performance advisor

- Samples actual runtime metrics once per second only during running sessions.
- Records frame-time percentiles, presented FPS, speed, dropped frames, audio underruns, thermal status, and battery level.
- Requires at least 120 samples before advice is marked ready.
- Persists bounded local evidence and exposes the evidence in Settings.
- Applies recommendations through a per-game launch profile rather than a global “boost” gimmick.

### Compatibility notebook

- Persists local compatibility status and up to 4,000 characters of user observations.
- Keeps notes separate from checksum/canonical identity.
- Does not upload paths, ROM bytes, saves, or account identifiers.

### Per-game launch profiles

- Performance profile
- scaling mode
- display smoothing
- touch-control layout
- touch-control visibility
- fast-forward speed

### Player/core corrections

- Fast-forward divides the frame budget by the selected multiplier.
- Runtime speed is calculated against the GBA target rate.
- Authentic, Balanced, and Battery Saver are the visible supported profiles.
- Legacy Boosted/Extreme stored values migrate to Balanced.

## Deliberately not claimed complete

### Network game achievements

rcheevos is not integrated yet. Doing it correctly requires exact game identity, audited core memory access, token lifecycle, offline queue semantics, hardcore-mode enforcement, and clear unsupported-game handling. Retra's local achievements remain separate and functional.

### Controller profile export

Profiles are robust and local, but SAF export/import is deferred until the profile format receives versioned compatibility tests and conflict-preview UX.

### Timeline thumbnails

The timeline schema supports a screenshot path, but automatic internal thumbnail capture and preview are not yet wired. Save correctness was prioritized over image generation on the gameplay thread.

### Device certification

Physical-device soak testing, controller matrix testing, thermal testing, and accessibility review remain release gates.
