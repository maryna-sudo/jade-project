#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CLASSPATH="$ROOT_DIR/lib/jade.jar:$ROOT_DIR/bin"

java -cp "$CLASSPATH" jade.Boot -gui \
  -agents "monitor:com.example.jade.projectmanagement.MonitoringAgent;worker1:com.example.jade.projectmanagement.TaskWorkerAgent(2000);worker2:com.example.jade.projectmanagement.TaskWorkerAgent(3500);worker3:com.example.jade.projectmanagement.TaskWorkerAgent(5000)"
