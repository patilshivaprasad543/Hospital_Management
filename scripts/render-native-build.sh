#!/usr/bin/env bash
# Build SmartCare 360 on Render Native (Node) runtimes that do not ship Java.
# Also used as npm postinstall/build so a service created as "Node" still deploys.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64|amd64) ADOPTIUM_ARCH=x64 ;;
  aarch64|arm64) ADOPTIUM_ARCH=aarch64 ;;
  *) echo "Unsupported arch: $ARCH"; exit 1 ;;
esac

if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -q '"21'; then
  echo "Using existing Java 21"
else
  JDK_DIR="$ROOT/.jdk"
  if [[ ! -x "$JDK_DIR/bin/java" ]]; then
    echo "Downloading Temurin 21 ($ADOPTIUM_ARCH)..."
    mkdir -p "$JDK_DIR"
    curl -fsSL -o /tmp/temurin21.tar.gz \
      "https://api.adoptium.net/v3/binary/latest/21/ga/linux/${ADOPTIUM_ARCH}/jdk/hotspot/normal/eclipse?project=jdk"
    tar -xzf /tmp/temurin21.tar.gz -C "$JDK_DIR" --strip-components=1
    rm -f /tmp/temurin21.tar.gz
  fi
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export GRADLE_OPTS="${GRADLE_OPTS:--Xmx384m -Dorg.gradle.daemon=false}"
chmod +x "$ROOT/gradlew"
"$ROOT/gradlew" bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx384m"

JAR="$ROOT/build/libs/Hospital_Management-0.0.1-SNAPSHOT.jar"
test -f "$JAR"
echo "Built $JAR"
java -version
