#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER_DIR="$ROOT_DIR/target/local-cluster"
COMPOSE_FILE="$ROOT_DIR/compose.local-proxy.yml"
JAR_PATH="$ROOT_DIR/target/multi-0.0.1.jar"
SIMULATOR_INTERVAL_MS="${DEVICE_SIMULATOR_INTERVAL_MS:-30}"

cd "$ROOT_DIR"

if [[ -n "${MVN_CMD:-}" ]]; then
  read -r -a MAVEN_COMMAND <<< "$MVN_CMD"
elif command -v mvn >/dev/null 2>&1; then
  MAVEN_COMMAND=(mvn)
elif [[ -x "$ROOT_DIR/mvnw" ]]; then
  MAVEN_COMMAND=("$ROOT_DIR/mvnw")
else
  echo "Maven is required. Install mvn or set MVN_CMD." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run the local HAProxy front door." >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required for local cluster health checks." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose is required to run the local HAProxy front door." >&2
  exit 1
fi

mkdir -p "$CLUSTER_DIR"

"$ROOT_DIR/scripts/stop-local-cluster.sh" --quiet

echo "Building Spring Boot jar..."
"${MAVEN_COMMAND[@]}" -DskipTests package

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Expected jar not found: $JAR_PATH" >&2
  exit 1
fi

start_instance() {
  local instance_id="$1"
  local port="$2"
  local log_file="$CLUSTER_DIR/$instance_id.log"
  local pid_file="$CLUSTER_DIR/$instance_id.pid"
  local pid

  echo "Starting $instance_id on port $port..."
  if command -v setsid >/dev/null 2>&1; then
    INSTANCE_ID="$instance_id" \
    SERVER_PORT="$port" \
    DEVICE_SIMULATOR_INTERVAL_MS="$SIMULATOR_INTERVAL_MS" \
      nohup setsid java -jar "$JAR_PATH" > "$log_file" 2>&1 < /dev/null &
  else
    INSTANCE_ID="$instance_id" \
    SERVER_PORT="$port" \
    DEVICE_SIMULATOR_INTERVAL_MS="$SIMULATOR_INTERVAL_MS" \
      nohup java -jar "$JAR_PATH" > "$log_file" 2>&1 < /dev/null &
  fi

  pid="$!"
  echo "$pid" > "$pid_file"
  disown "$pid" 2>/dev/null || true
}

wait_for_instance() {
  local instance_id="$1"
  local port="$2"
  local pid_file="$CLUSTER_DIR/$instance_id.pid"
  local log_file="$CLUSTER_DIR/$instance_id.log"
  local pid

  pid="$(cat "$pid_file")"

  for _ in {1..60}; do
    if curl -fsS "http://localhost:$port/api/instance" >/dev/null 2>&1; then
      return 0
    fi

    if ! kill -0 "$pid" >/dev/null 2>&1; then
      echo "$instance_id exited before it became healthy. Log tail:" >&2
      tail -n 80 "$log_file" >&2 || true
      exit 1
    fi

    sleep 1
  done

  echo "$instance_id did not become healthy on port $port. Log tail:" >&2
  tail -n 80 "$log_file" >&2 || true
  exit 1
}

verify_proxy_sticky_session() {
  local cookie_file="$CLUSTER_DIR/proxy-cookie.txt"
  local first_response=""
  local second_response=""
  local third_response=""

  rm -f "$cookie_file"

  for _ in {1..60}; do
    if first_response="$(
      curl -fsS -c "$cookie_file" -b "$cookie_file" \
        "http://localhost:8080/api/instance"
    )"; then
      break
    fi
    sleep 1
  done

  if [[ -z "$first_response" ]]; then
    echo "HAProxy did not become healthy on port 8080." >&2
    docker compose -f "$COMPOSE_FILE" logs haproxy >&2 || true
    exit 1
  fi

  second_response="$(
    curl -fsS -c "$cookie_file" -b "$cookie_file" \
      "http://localhost:8080/api/instance"
  )"
  third_response="$(
    curl -fsS -c "$cookie_file" -b "$cookie_file" \
      "http://localhost:8080/api/instance"
  )"

  if [[ "$first_response" != "$second_response" || "$first_response" != "$third_response" ]]; then
    echo "HAProxy sticky routing check failed." >&2
    echo "First:  $first_response" >&2
    echo "Second: $second_response" >&2
    echo "Third:  $third_response" >&2
    exit 1
  fi

  echo "Verified sticky proxy routing through http://localhost:8080/api/instance: $first_response"
}

start_instance app1 8081
start_instance app2 8082
start_instance app3 8083

wait_for_instance app1 8081
wait_for_instance app2 8082
wait_for_instance app3 8083

echo "Starting HAProxy on http://localhost:8080..."
docker compose -f "$COMPOSE_FILE" up -d
verify_proxy_sticky_session

echo
echo "Local Spring Boot cluster is running:"
echo "  app1: http://localhost:8081"
echo "  app2: http://localhost:8082"
echo "  app3: http://localhost:8083"
echo "  proxy: http://localhost:8080"
echo
echo "Logs and PID files are in $CLUSTER_DIR"
echo "Run scripts/stop-local-cluster.sh to stop the cluster."
