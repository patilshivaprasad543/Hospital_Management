#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

if [[ ! -x ./gradlew ]]; then
  echo "Gradle wrapper not found in $(pwd)" >&2
  exit 1
fi

./gradlew build -x test --no-daemon
