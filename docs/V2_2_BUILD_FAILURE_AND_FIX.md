# Retra 2.2 Build Failure and Fix

Date: 2026-07-22

## Failure that was reported

The previous CI run reached Kotlin annotation processing and then failed in `:app:kaptReleaseKotlin`. The underlying exception was not an emulator-core or Compose issue. Room attempted to deserialize the committed version 6 schema and encountered end-of-file before the JSON object closed.

The broken schema was effectively a partial compiler output. Because the release and debug KAPT tasks could run concurrently, the failure could surface in one variant while the other was still active, which made the top-level Gradle error look less specific than it was.

## Repair

Retra 2.2 makes schema generation deterministic:

- Replaced the truncated `app/schemas/.../6.json` with a complete, parseable Room schema.
- Applied the Room Gradle plugin and configured a single schema directory.
- Added the committed schema directory to `androidTest` assets.
- Added a Room 5→6 migration instrumentation test.
- Disabled Gradle parallel execution for this project and uses `--no-parallel` in CI.
- Added `tools/schema-verification/run.sh`, which parses every committed schema before and after Android compilation.
- Split unit tests, debug assembly, release compilation, migration-test compilation, and lint into explicit CI steps so the failing stage is obvious.
- CI uploads an installable debug APK for private testing while still compiling the unsigned release variant. Production signing remains private.

## Remaining build boundary

This container has no cached Gradle 9.5.0 distribution and cannot currently resolve the Gradle distribution host. Therefore the Android dependency graph, Compose code, Hilt/KAPT processors, and APK packaging could not be executed here. The project-level, schema, platform-neutral, JNI, and mock-libretro verification suites were executed locally; the Android workflow is designed to perform the missing checks on a provisioned runner.

No APK is claimed in this source release.
