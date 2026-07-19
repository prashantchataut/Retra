# Next Actions

1. **Produce the first Android debug build.** Generate the wrapper, resolve pinned dependencies, run `:app:assembleDebug`, unit tests, lint, and Compose tests; fix Android-only compile or code-generation issues.
2. **Integrate real mGBA on device.** Fetch the pinned source, verify its license/hash, build `libmgba_libretro.so`, package ABIs, and run ROM/frame/audio/input/state/battery/cheat tests on arm64 hardware.
3. **Complete durability testing.** Exercise process death, background/foreground, storage pressure, interrupted writes, corrupted states, migrations, and pre-cheat recovery on real Android filesystems.
4. **Implement core link callbacks.** Define serial/link timing events, deterministic synchronization, rollback/resync behavior, and compatibility fixtures before connecting the existing LAN transport to gameplay.
5. **Harden internet services.** Add DNS-resolution private-address enforcement, certificate/network tests, durable WorkManager queues, catalog signatures/revocation, and explicit trust UX.
6. **Run product QA.** Accessibility audit, screen-reader/controller-only navigation, foldable/tablet layouts, thermal/battery profiling, low-end-device tests, and save-integrity soak tests.
7. **Add credentialed services only after foundations pass.** Provider OAuth, relay, cloud saves, and external achievements require real credentials, privacy terms, account deletion/export, security review, and operational ownership.
