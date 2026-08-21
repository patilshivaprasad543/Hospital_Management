#!/usr/bin/env bash
# Start SmartCare 360 after a Render Native (Node) build.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -x "$ROOT/.jdk/bin/java" ]]; then
  export JAVA_HOME="$ROOT/.jdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

JAR="$ROOT/build/libs/Hospital_Management-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Jar missing — building..."
  bash "$ROOT/scripts/render-native-build.sh"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export PORT="${PORT:-8080}"
mkdir -p /app/data 2>/dev/null || mkdir -p "$ROOT/data"
exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1 \
  -jar "$JAR" --server.port="$PORT"
