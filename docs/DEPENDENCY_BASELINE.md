# Dependency Baseline

Declared source baseline on 2026-07-19:

- Android Gradle Plugin 9.3.0
- Gradle 9.5.0 wrapper target
- Kotlin 2.3.21
- Kotlin coroutines 1.11.0
- Compose BOM 2026.06.00
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- AndroidX Core 1.19.0
- Room 2.8.4
- DataStore 1.2.1
- WorkManager 2.11.2
- Dagger/Hilt 2.59.2
- DocumentFile 1.1.0
- AndroidX Test JUnit 1.3.0 and Espresso 3.7.0
- compile/target SDK 37, minimum SDK 26
- NDK 28.2.13676358
- CMake 3.22.1 project requirement

Versions are centralized in `gradle/libs.versions.toml` where applicable. The first resolved Gradle sync/build is the source of truth: this environment could not download or resolve Android artifacts, and version compatibility must be confirmed on the prepared workstation before release.
