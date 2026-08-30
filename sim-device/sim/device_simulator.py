
import time
import random
import threading
from concurrent.futures import ThreadPoolExecutor

class DeviceSimulator:

    def __init__(self, devices, socket_client, interval_ms=1000):
        self.__devices = devices
        self.__socket_client = socket_client
        self.__interval_seconds = interval_ms / 1000
        self.__executor = ThreadPoolExecutor(max_workers=20)

    def simulate(self):
        self.__socket_client.connect()
        while True:
            current_time = int(time.time() * 1000)

            for device in self.__devices:
                if device.is_ready_for_upload(current_time):
                    self.__executor.submit(self.__update_device, device)

            time.sleep(self.__interval_seconds)

    def __update_device(self, device):
        data = {
            "imei": device.imei,
            "status": random.choice(["ONLINE", "IDLE", "CHARGING"]),
            "signal": random.randint(2, 33),
            "power": round(random.uniform(100, 5000), 2),
            "temperature": round(random.uniform(20, 70), 2),
            "thread": threading.current_thread().name,
            "timestamp": int(time.time() * 1000),
        }
        print(data)
        self.__socket_client.send_device_data(device.imei, data)

