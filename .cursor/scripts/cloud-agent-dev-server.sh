#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export PORT="${PORT:-8085}"

exec ./gradlew bootRun --no-daemon
