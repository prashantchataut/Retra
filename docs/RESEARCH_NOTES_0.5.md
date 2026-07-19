# Retra 0.5 Research Notes

## Emulator core

mGBA 0.10.5 remains the pinned gameplay core. The source is MPL-2.0, while bundled third-party components can carry additional notices. Retra uses the libretro ABI behind its own Kotlin/JNI frontend so UI, accounts, catalogs, and cloud systems never become dependencies of the emulator core.

Primary references:

- https://github.com/mgba-emu/mgba
- https://mgba.io/2025/03/28/mgba-0.10.5/
- https://github.com/mgba-emu/mgba/blob/master/LICENSE

## Identity

Android now recommends Credential Manager for Sign in with Google. Explicit sign-in uses `GetSignInWithGoogleOption`; returned ID tokens must be validated by a server and nonce use is recommended against replay.

- https://developer.android.com/identity/sign-in/credential-manager-siwg

## Controller behavior

Android game controller handling should cover key events, joystick motion, hat axes, analog fallback, device addition/removal, and controller-friendly focus navigation. Retra captures game input only while gameplay or the explicit tester is active so D-pad navigation remains available elsewhere.

- https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input
- https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior

## Audio

Retra keeps a streaming `AudioTrack` path with focus handling and a pause-on-output-disconnect option. The native core remains isolated so an Oboe/AAudio backend can replace the current sink without changing emulation or save APIs.

- https://developer.android.com/games/sdk/oboe/low-latency-audio
- https://developer.android.com/media/optimize/audio-focus

## Rendering and large screens

The player uses `SurfaceView`, aspect-fit presentation, optional integer scaling, and optional bitmap filtering. The surrounding Compose shell uses bottom navigation on phones and a rail on larger widths.

- https://developer.android.com/develop/ui/compose/layouts/adaptive
- https://developer.android.com/develop/ui/views/graphics/surface-view

## Save integrity and rewind

Rewind is intentionally memory-only. Native states are copied into a bounded 32 MiB ring and are discarded when the game changes, resets, or closes. Persistent saves continue to use versioned, ROM-bound, checksummed envelopes and atomic replacement.
