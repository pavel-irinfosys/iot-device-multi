# Multi Spring Boot WebSocket Demo

This project is a Spring Boot WebSocket/STOMP device data demo. It can run as a local three-instance cluster behind HAProxy, and a Python simulator can send generated device data into the Spring Boot websocket controller.

For the full architecture, operations, API, and troubleshooting guide, see [`docs/PROJECT_DOCUMENTATION.md`](docs/PROJECT_DOCUMENTATION.md).

## Architecture

Local cluster layout:

| Component | URL |
| --- | --- |
| HAProxy front door | `http://localhost:8080` |
| Spring Boot `app1` | `http://localhost:8081` |
| Spring Boot `app2` | `http://localhost:8082` |
| Spring Boot `app3` | `http://localhost:8083` |
| Redis | `localhost:26379` |

HAProxy uses sticky cookies so a browser session stays on the same backend instance. Websocket subscriptions stay in memory inside each Spring Boot process, while latest device data is shared through Redis.

## Requirements

- Java 25
- Maven or Maven Wrapper
- Docker with Docker Compose
- Python 3.12 or compatible Python 3

## Run The Local Cluster

From the project root:

```bash
cd /home/pavel/projects/test/multi
make start
```

This builds the Spring Boot jar, starts the project Redis service, starts `app1`, `app2`, and `app3`, then starts HAProxy on `http://localhost:8080`.

Open the browser at:

```text
http://localhost:8080
```

Stop the cluster:

```bash
make stop
```

Restart the cluster:

```bash
make restart
```

Check instance status:

```bash
make status
```

Verify sticky routing:

```bash
make sticky-check
```

Tail logs:

```bash
make logs
```

Tail one app log:

```bash
make logs-app1
make logs-app2
make logs-app3
```

## Run The Python Device Sender

Install Python dependencies:

```bash
cd /home/pavel/projects/test/multi/sim-device
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
```

Run the simulator:

```bash
.venv/bin/python main.py 10 1000XXXXX
```

By default, the Python sender connects to:

```text
ws://localhost:8080/ws
```

Override the websocket URL when needed:

```bash
DEVICE_SOCKET_URL=ws://localhost:8081/ws .venv/bin/python main.py 10 1000XXXXX
```

Using a direct backend URL such as `ws://localhost:8081/ws` is useful when you want to isolate one Spring Boot instance during local testing.

## WebSocket And STOMP Endpoints

WebSocket endpoint:

```text
/ws
```

Application destinations:

| Destination | Purpose |
| --- | --- |
| `/app/devices.imeis` | Ask the server for known device IMEIs |
| `/app/devices.subscribe` | Subscribe the current websocket session to selected IMEIs |
| `/app/devices.data` | Upload device data from the Python simulator |
| `/app/chat.send` | Send a chat message to the topic broker |

User queues:

| Destination | Purpose |
| --- | --- |
| `/user/queue/device-imeis` | Receives device IMEI list |
| `/user/queue/device-subscriptions` | Receives saved subscription response |
| `/user/queue/device-data` | Receives live device data for subscribed IMEIs |

Topics:

| Destination | Purpose |
| --- | --- |
| `/topic/messages` | Receives chat messages |

## Device Upload Payload

Python sends generated data to `/app/devices.data` with this shape:

```json
{
  "imei": "868484076000001",
  "command": "H0",
  "data": {
    "imei": "868484076000001",
    "status": "ONLINE",
    "signal": 20,
    "power": 1200.55,
    "temperature": 32.8,
    "thread": "ThreadPoolExecutor-0_0",
    "timestamp": 1798590000000
  }
}
```

Spring Boot receives that payload, stores it as latest device data, and publishes it to subscribed websocket sessions.

## Useful Build Commands

Run tests:

```bash
make test
```

Build the runnable jar without tests:

```bash
make package
```

Validate Docker Compose proxy config:

```bash
make proxy-config
```

## Troubleshooting

### `ModuleNotFoundError: No module named 'websocket'`

Install the Python dependency inside the simulator venv:

```bash
cd /home/pavel/projects/test/multi/sim-device
.venv/bin/python -m pip install -r requirements.txt
```

The Python package name is `websocket-client`, but the import name is `websocket`.

### HAProxy Returns `503`

Check that all three Spring Boot instances are running:

```bash
make status
```

If the apps are not healthy, restart the cluster:

```bash
make restart
```

### Browser Does Not Receive Python Device Data

Latest device data and update events are shared through Redis, so the browser and Python sender can use different backend instances. If live data does not appear, confirm Redis is running and that the browser subscribed to IMEIs the simulator is actively uploading.

To isolate HAProxy while debugging, use the same direct backend for both clients, for example:

```bash
DEVICE_SOCKET_URL=ws://localhost:8081/ws .venv/bin/python main.py 10 1000XXXXX
```

Then open:

```text
http://localhost:8081
```

### Docker Or Compose Is Missing

`make start` requires Docker and Docker Compose because Redis and HAProxy run in Docker. Confirm Compose is available:

```bash
docker compose version
```

## Generated Spring Help

`HELP.md` is the generated Spring Boot help file and is kept separate from this project README.
