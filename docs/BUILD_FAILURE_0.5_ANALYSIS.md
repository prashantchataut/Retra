# Analysis of the reported 0.5.0 build failure

## Failure point

The release pipeline completed module compilation, resource processing, Kotlin annotation processing, and native CMake builds for arm64-v8a, armeabi-v7a, and x86_64. It failed only at `:app:compileReleaseKotlin`.

## Root cause

Six Compose screens explicitly imported:

```kotlin
import androidx.compose.foundation.layout.weight
```

With the Compose version used by the project, that import resolved to an internal `RowColumnParentData?.weight` property. Kotlin correctly blocked access to the internal symbol.

## Correct usage

`weight` is a scoped `Modifier` extension. Use it inside a `Row` or `Column` content lambda without importing the internal symbol:

```kotlin
Row {
    Box(Modifier.weight(1f))
}
```

## Repair and prevention

The invalid import was removed from all six files. The static project verifier now scans every production Kotlin source and fails if the import is reintroduced.

## Confidence boundary

The repair directly addresses every compiler diagnostic in the supplied log. A full Android rebuild is still required to discover any later-stage error that was previously hidden by this first compilation failure.
