#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER_DIR="$ROOT_DIR/target/local-cluster"
COMPOSE_FILE="$ROOT_DIR/compose.local-proxy.yml"
QUIET=false

if [[ "${1:-}" == "--quiet" ]]; then
  QUIET=true
fi

log() {
  if [[ "$QUIET" != true ]]; then
    echo "$@"
  fi
}

cd "$ROOT_DIR"

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  log "Stopping HAProxy..."
  docker compose -f "$COMPOSE_FILE" down >/dev/null 2>&1 || true
fi

if [[ ! -d "$CLUSTER_DIR" ]]; then
  log "No local cluster PID directory found."
  exit 0
fi

for pid_file in "$CLUSTER_DIR"/*.pid; do
  [[ -e "$pid_file" ]] || continue

  pid="$(cat "$pid_file")"
  instance_id="$(basename "$pid_file" .pid)"

  if [[ -z "$pid" ]] || ! kill -0 "$pid" >/dev/null 2>&1; then
    rm -f "$pid_file"
    continue
  fi

  command_line="$(ps -p "$pid" -o args= 2>/dev/null || true)"
  if [[ "$command_line" != *"java"* || "$command_line" != *"multi-0.0.1.jar"* ]]; then
    log "Skipping $instance_id because PID $pid is not the managed Spring Boot jar."
    continue
  fi

  log "Stopping $instance_id with PID $pid..."
  kill "$pid" >/dev/null 2>&1 || true

  for _ in {1..20}; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      break
    fi
    sleep 0.5
  done

  if kill -0 "$pid" >/dev/null 2>&1; then
    log "$instance_id with PID $pid is still running after SIGTERM."
  else
    rm -f "$pid_file"
  fi
done

log "Local cluster stop complete."
