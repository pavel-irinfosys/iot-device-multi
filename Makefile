SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE_FILE := compose.local-proxy.yml
CLUSTER_DIR := target/local-cluster
COOKIE_FILE ?= /tmp/multi-local-cookie.txt
MVN ?= mvn

.PHONY: help start stop restart up test package proxy-config status sticky-check logs logs-app1 logs-app2 logs-app3

help:
	@printf "Local multi-instance Spring Boot targets:\n"
	@printf "  make start          Build jar, start app1-app3, and start HAProxy\n"
	@printf "  make stop           Stop the managed app instances and HAProxy\n"
	@printf "  make restart        Stop, then start the local cluster\n"
	@printf "  make up             Start Docker Compose services\n"
	@printf "  make status         Check app1-app3 and the HAProxy front door\n"
	@printf "  make sticky-check   Verify HAProxy keeps one cookie jar on one backend\n"
	@printf "  make logs           Tail all local app logs\n"
	@printf "  make logs-app1      Tail app1 log\n"
	@printf "  make logs-app2      Tail app2 log\n"
	@printf "  make logs-app3      Tail app3 log\n"
	@printf "  make test           Run Maven tests\n"
	@printf "  make package        Build the runnable Spring Boot jar without tests\n"
	@printf "  make proxy-config   Validate/render the Docker Compose proxy config\n"

start:
	./scripts/start-local-cluster.sh

stop:
	./scripts/stop-local-cluster.sh

restart: stop start

up:
	docker compose -f $(COMPOSE_FILE) up -d

test:
	$(MVN) test

package:
	$(MVN) package -DskipTests

proxy-config:
	docker compose -f $(COMPOSE_FILE) config

status:
	@printf "app1:  "
	@curl -fsS http://localhost:8081/api/instance || true
	@printf "\napp2:  "
	@curl -fsS http://localhost:8082/api/instance || true
	@printf "\napp3:  "
	@curl -fsS http://localhost:8083/api/instance || true
	@printf "\nproxy: "
	@curl -fsS -c "$(COOKIE_FILE)" -b "$(COOKIE_FILE)" http://localhost:8080/api/instance || true
	@printf "\n"

sticky-check:
	@rm -f "$(COOKIE_FILE)"
	@first="$$(curl -fsS -c "$(COOKIE_FILE)" -b "$(COOKIE_FILE)" http://localhost:8080/api/instance)"; \
	second="$$(curl -fsS -c "$(COOKIE_FILE)" -b "$(COOKIE_FILE)" http://localhost:8080/api/instance)"; \
	third="$$(curl -fsS -c "$(COOKIE_FILE)" -b "$(COOKIE_FILE)" http://localhost:8080/api/instance)"; \
	printf "first:  %s\nsecond: %s\nthird:  %s\n" "$$first" "$$second" "$$third"; \
	if [[ "$$first" != "$$second" || "$$first" != "$$third" ]]; then \
	  echo "Sticky routing check failed" >&2; \
	  exit 1; \
	fi

logs:
	tail -f $(CLUSTER_DIR)/app1.log $(CLUSTER_DIR)/app2.log $(CLUSTER_DIR)/app3.log

logs-app1:
	tail -f $(CLUSTER_DIR)/app1.log

logs-app2:
	tail -f $(CLUSTER_DIR)/app2.log

logs-app3:
	tail -f $(CLUSTER_DIR)/app3.log
