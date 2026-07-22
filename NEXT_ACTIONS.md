# Next Actions

1. Run `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` in provisioned CI.
2. Generate and review the Room schema 6 export; add instrumentation tests for 5→6 migration and user-title preservation.
3. Stage reviewed mGBA 0.10.5 libretro binaries for every declared ABI and make release packaging fail on omissions.
4. Add live-contract tests for Homebrew Hub and Libretro response changes, offline behavior, timeouts, and malformed data.
5. Add a Save Health center and ROM-excluding backup/export bundle before expanding social features.
6. Add controller calibration and per-device/per-game mappings.
7. Test compact phone, tablet/foldable, dark/light, high contrast, large text, reduced motion, and reduced transparency.
8. Validate lawful FireRed/Emerald backups through checksum identification, matching cheat install, save, restore, patch, and conflict flows.
9. Inject release signing only from private CI/store configuration; add SBOM and privacy review before distribution.
