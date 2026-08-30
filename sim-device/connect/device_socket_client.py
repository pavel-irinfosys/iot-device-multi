import json
import os
import websocket


class DeviceSocketClient:
    def __init__(self, url=None):
        self.url = url or os.getenv("DEVICE_SOCKET_URL", "ws://localhost:8080/ws")
        self.ws = None

    def connect(self):
        self.ws = websocket.create_connection(self.url)
        self.ws.send("CONNECT\naccept-version:1.2\nhost:localhost\n\n\x00")
        self.ws.recv()

    def send_device_data(self, imei, data, command="H0"):
        payload = {
            "imei": imei,
            "command": command,
            "data": data,
        }

        body = json.dumps(payload)

        frame = (
            "SEND\n"
            "destination:/app/devices.data\n"
            "content-type:application/json\n"
            f"content-length:{len(body)}\n"
            "\n"
            f"{body}\x00"
        )

        self.ws.send(frame)

    def close(self):
        if self.ws:
            self.ws.close()


