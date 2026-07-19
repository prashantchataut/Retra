#!/usr/bin/env sh
set -eu
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not installed. Open the project in Android Studio or install Gradle 9.5.0, then run this script again." >&2
  exit 1
fi
gradle wrapper --gradle-version 9.5.0
