#!/usr/bin/env bash
set -euo pipefail

# Detect OS and architecture
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"

# Map architecture
case "$ARCH" in
  x86_64) ARCH="x64" ;;
  aarch64|arm64) ARCH="aarch64" ;;
  *) echo "[setup-java] Unsupported architecture: $ARCH" >&2; exit 1 ;;
esac

# Map OS to Adoptium API format
case "$OS" in
  linux) OS_API="linux" ;;
  darwin) OS_API="mac" ;;
  *) echo "[setup-java] Unsupported OS: $OS" >&2; exit 1 ;;
esac

JDK_WORKDIR="${CI_PROJECT_DIR:-$(pwd)}/.ci-jdk"
JDK_HOME_DIR="$JDK_WORKDIR/home"
JDK_TARBALL="$JDK_WORKDIR/jdk.tar.gz"
JDK_VERSION="21"
JDK_URL="https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/ga/${OS_API}/${ARCH}/jdk/hotspot/normal/eclipse"

if [ ! -d "$JDK_HOME_DIR" ]; then
  echo "[setup-java] Installing Temurin JDK ${JDK_VERSION} for ${OS_API}/${ARCH}..."
  rm -rf "$JDK_WORKDIR"
  mkdir -p "$JDK_WORKDIR"
  curl -sSL --fail -o "$JDK_TARBALL" "$JDK_URL"
  tar -xzf "$JDK_TARBALL" -C "$JDK_WORKDIR"
  rm -f "$JDK_TARBALL"
  EXTRACTED_HOME=$(find "$JDK_WORKDIR" -maxdepth 5 -type d -name "Home" | head -n1)
  if [ -z "$EXTRACTED_HOME" ]; then
    echo "[setup-java] Failed to locate extracted JDK Home" >&2
    exit 1
  fi
  mkdir -p "$JDK_HOME_DIR"
  cp -R "$EXTRACTED_HOME/" "$JDK_HOME_DIR/"
else
  echo "[setup-java] Reusing cached JDK at $JDK_HOME_DIR"
fi

export JAVA_HOME="$JDK_HOME_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

java -version
