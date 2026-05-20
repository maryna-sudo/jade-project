#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BIN_DIR="$ROOT_DIR/bin"
SRC_DIR="$ROOT_DIR/src"
LIB_JAR="$ROOT_DIR/lib/jade.jar"

rm -rf "$BIN_DIR"
mkdir -p "$BIN_DIR"

javac -cp "$LIB_JAR" -d "$BIN_DIR" $(find "$SRC_DIR" -name '*.java' | sort)

echo "Compiled classes into $BIN_DIR"
