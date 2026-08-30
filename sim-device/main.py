import sys
from sim.device import Device
from connect.device_socket_client import DeviceSocketClient
from sim.device_simulator import DeviceSimulator

UPLOAD_INTERVAL_MS = 1000


def create_imei(pattern: str, number: int) -> str:
    placeholder_count = pattern.count("X")
    if placeholder_count == 0:
        raise ValueError("IMEI pattern must contain X placeholders, example: 1000XXXXX")

    padded_number = str(number).zfill(placeholder_count)
    if len(padded_number) > placeholder_count:
        raise ValueError(f"Number {number} is too large for pattern {pattern}")

    return pattern.replace("X" * placeholder_count, padded_number)


def create_devices(device_count: int, imei_pattern: str):
    devices = []

    for number in range(1, device_count + 1):
        imei = create_imei(imei_pattern, number)
        devices.append(Device(imei, UPLOAD_INTERVAL_MS))

    return devices


def main():
    if len(sys.argv) != 3:
        print("Usage:")
        print("  python main.py <device_count> <imei_pattern>")
        print()
        print("Example:")
        print("  python main.py 10 1000XXXXX")
        sys.exit(1)

    device_count = int(sys.argv[1])
    imei_pattern = sys.argv[2]

    devices = create_devices(device_count, imei_pattern)

    socket_client = DeviceSocketClient()
    simulation = DeviceSimulator(devices, socket_client)
    simulation.simulate()


if __name__ == "__main__":
    main()
