import json
import os
import http.cookiejar
from urllib.parse import urlsplit, urlunsplit
from urllib.request import HTTPCookieProcessor, Request, build_opener
import websocket


class DeviceSocketClient:
    def __init__(self, url=None):
        self.url = url or os.getenv("DEVICE_SOCKET_URL", "ws://localhost:8080/ws")
        self.ws = None
        self.instance_info = None
        self.cookie_jar = http.cookiejar.CookieJar()
        self.http_opener = build_opener(HTTPCookieProcessor(self.cookie_jar))

    def connect(self):
        self.instance_info = self.fetch_instance_info()
        self.ws = websocket.create_connection(
            self.url,
            cookie=self.create_cookie_header()
        )
        self.ws.send("CONNECT\naccept-version:1.2\nhost:localhost\n\n\x00")
        self.ws.recv()
        self.print_instance_info()

    def fetch_instance_info(self):
        try:
            request = Request(
                self.create_instance_info_url(),
                headers={"Cache-Control": "no-store"}
            )
            with self.http_opener.open(request, timeout=3) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as error:
            print(f"Connected Java instance: info unavailable ({error})")
            return None

    def create_instance_info_url(self):
        parsed_url = urlsplit(self.url)
        scheme = "https" if parsed_url.scheme == "wss" else "http"
        return urlunsplit((scheme, parsed_url.netloc, "/api/instance", "", ""))

    def create_cookie_header(self):
        return "; ".join(
            f"{cookie.name}={cookie.value}"
            for cookie in self.cookie_jar
        )

    def print_instance_info(self):
        if not self.instance_info:
            print("Connected Java instance: info unavailable")
            return

        print(
            "Connected Java instance: "
            f"{self.instance_info.get('instanceId')} | "
            f"Port: {self.instance_info.get('port')}"
        )

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

