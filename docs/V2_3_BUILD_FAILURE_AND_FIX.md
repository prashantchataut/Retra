# Retra 2.3 Build Failure and Signing Fix

## Symptom

The workflow ran a strict shell block and exited with:

```text
A reusable app signing key or password must not be committed
```

The failure occurred before the Android build. It was emitted by `tools/project-verification/run.sh`, not by Gradle, Android signing, Hilt, Room, or mGBA.

## Root cause

The old check treated the presence of a local keystore path or the text `storePassword` in `app/build.gradle.kts` as proof that credentials were committed. That assumption was wrong: a secure build script must name signing properties while obtaining their values from Gradle Providers or environment variables.

## New policy

`tools/signing-verification/run.sh` now distinguishes source code from secrets.

It rejects:

- tracked `.jks`, `.keystore`, `.p12`, `.pfx`, `.pem`, `.der`, or `.key` files;
- tracked signing property files containing nonempty `storePassword` or `keyPassword` values;
- literal password strings assigned in Gradle source.

It permits:

- `providers.environmentVariable(...)`;
- `storePassword = releaseStorePassword.get()` and equivalent Provider-backed values;
- a keystore path outside the checkout;
- a CI-created temporary keystore under `$RUNNER_TEMP`.

## Local release build

Keep the keystore outside the Retra directory:

```bash
export RETRA_SIGNING_STORE_FILE="$HOME/.signing/retra-release.jks"
export RETRA_SIGNING_STORE_PASSWORD="..."
export RETRA_SIGNING_KEY_ALIAS="retra"
export RETRA_SIGNING_KEY_PASSWORD="..."
./gradlew --no-daemon --no-parallel :app:assembleRelease
```

With none of these variables, `assembleRelease` creates an unsigned release artifact for compilation verification. With only some variables, signing remains disabled locally; the GitHub workflow is stricter and fails partially configured secret sets.

## GitHub Actions secrets

Optional signed-release compilation uses four secrets:

- `RETRA_SIGNING_KEYSTORE_B64`
- `RETRA_SIGNING_STORE_PASSWORD`
- `RETRA_SIGNING_KEY_ALIAS`
- `RETRA_SIGNING_KEY_PASSWORD`

The workflow decodes the keystore into `$RUNNER_TEMP/retra-release.jks`, sets permissions to `0600`, and provides credentials only to the release Gradle step. All four values or none must be configured.

## Regression command

```bash
set -euo pipefail
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

This command now passes with the secure Provider-backed signing configuration in the source tree.
