#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CLASSPATH="$ROOT_DIR/lib/jade.jar:$ROOT_DIR/bin"

java -cp "$CLASSPATH" jade.Boot \
  -container \
  -host localhost \
  -agents "coordinator:com.example.jade.projectmanagement.ProjectCoordinatorAgent"
